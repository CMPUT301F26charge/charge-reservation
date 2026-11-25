
const test = require("node:test");
const assert = require("node:assert/strict");

const { selectWinners, drawClosedLottery } = require("./lotteryDraw");

test("selects the requested number of winners", () => {
    const entrants = ["A", "B", "C", "D", "E"];

    const winners = selectWinners(entrants, 3);

    assert.equal(winners.length, 3);
    assert.equal(new Set(winners).size, 3);

    for (const winner of winners) {
        assert.ok(entrants.includes(winner));
    }
});

test("selects everyone when entrants are fewer than spots", () => {
    const winners = selectWinners(["A", "B"], 5);

    assert.equal(winners.length, 2);
    assert.deepEqual(new Set(winners), new Set(["A", "B"]));
});

test("handles an empty waitlist", () => {
    assert.deepEqual(selectWinners([], 5), []);
});

test("does not modify the original entrants array", () => {
    const entrants = ["A", "B", "C"];
    const original = [...entrants];

    selectWinners(entrants, 2);

    assert.deepEqual(entrants, original);
});

test("rejects invalid winner counts", () => {
    assert.throws(() => selectWinners(["A"], -1));
    assert.throws(() => selectWinners(["A"], 1.5));
});

test("winners are not always selected in input order", () => {
    const entrants = ["A", "B", "C", "D", "E"];

    const results = new Set();

    for (let i = 0; i < 100; i++) {
        results.add(selectWinners(entrants, 1)[0]);
    }

    assert.ok(results.size > 1);
});


function createFakeFirestore() {
    const lotteryId = "test-lottery";
    const lotteryData = {
        registrationEndAt: {
            toDate: () => new Date("2020-01-01T00:00:00Z")
        },
        winnerCount: 1,
        drawCompletedAt: null,
        waitlistCount: 2
    };

    const participants = [
        { id: "participant-A", entrantId: "A", status: "WAITING" },
        { id: "participant-B", entrantId: "B", status: "WAITING" }
    ];

    const notifications = new Map();

    const lotteryRef = {
        id: lotteryId,
        kind: "lottery"
    };

    const db = {
        collection(name) {
            return {
                doc(id) {
                    return { id, collection: name, kind: name };
                },
                where(field, operator, value) {
                    const filters = [[field, value]];

                    const query = {
                        where(nextField, nextOperator, nextValue) {
                            filters.push([nextField, nextValue]);
                            return query;
                        },
                        limit() {
                            return query;
                        },
                        collectionName: name,
                        filters
                    };

                    return query;
                }
            };
        },

        async runTransaction(callback) {
            const writes = [];

            const transaction = {
                async get(reference) {
                    if (reference.collection === "lotteries") {
                        return {
                            exists: true,
                            data: () => ({ ...lotteryData })
                        };
                    }

                    const matching = participants.filter(participant =>
                        reference.filters.every(([field, value]) =>
                            (field === "lotteryId"
                                ? lotteryId
                                : participant[field]) === value
                        )
                    );

                    return {
                        size: matching.length,
                        docs: matching.map(participant => ({
                            id: participant.id,
                            ref: {
                                kind: "participant",
                                participant
                            },
                            get(field) {
                                return participant[field];
                            }
                        }))
                    };
                },

                update(reference, data) {
                    writes.push({ type: "update", reference, data });
                },

                set(reference, data) {
                    writes.push({ type: "set", reference, data });
                }
            };

            const result = await callback(transaction);

            // Apply changes only after the transaction completes.
            for (const write of writes) {
                if (write.type === "set") {
                    notifications.set(write.reference.id, write.data);
                } else if (write.reference.kind === "participant") {
                    Object.assign(write.reference.participant, write.data);
                } else {
                    Object.assign(lotteryData, write.data);
                }
            }

            return result;
        }
    };

    return { db, lotteryId, lotteryData, participants, notifications };
}

test("creates one notification for each lottery result", async () => {
    const fake = createFakeFirestore();

    const result = await drawClosedLottery(fake.db, fake.lotteryId);

    assert.equal(result.status, "completed");
    assert.equal(result.winnersSelected, 1);
    assert.equal(fake.notifications.size, 2);

    const invited = fake.participants.filter(p => p.status === "INVITED");
    const rejected = fake.participants.filter(p => p.status === "NOT_SELECTED");

    assert.equal(invited.length, 1);
    assert.equal(rejected.length, 1);

    for (const participant of fake.participants) {
        const notification = fake.notifications.get(
            `lottery-result-${participant.id}`
        );

        assert.ok(notification);
        assert.equal(notification.recipientId, participant.entrantId);
        assert.equal(notification.lotteryId, fake.lotteryId);
        assert.equal(notification.type, "SYSTEM");
        assert.equal(notification.sendStatus, "SENT");
    }
});

test("winner and loser receive the correct messages", async () => {
    const fake = createFakeFirestore();

    await drawClosedLottery(fake.db, fake.lotteryId);

    for (const participant of fake.participants) {
        const notification = fake.notifications.get(
            `lottery-result-${participant.id}`
        );

        if (participant.status === "INVITED") {
            assert.match(notification.content, /Congratulations/);
        } else {
            assert.match(notification.content, /not selected/);
        }
    }
});

test("drawing an already completed lottery creates no duplicates", async () => {
    const fake = createFakeFirestore();

    await drawClosedLottery(fake.db, fake.lotteryId);
    const firstNotifications = new Map(fake.notifications);

    const secondResult = await drawClosedLottery(fake.db, fake.lotteryId);

    assert.equal(secondResult.status, "already_completed");
    assert.equal(fake.notifications.size, 2);
    assert.deepEqual(fake.notifications, firstNotifications);
});
