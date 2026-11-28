"use strict";

/** Exercises creator permissions and atomic, single-use campsite contributions. */
const test = require("node:test");
const assert = require("node:assert/strict");
const { createInvitation, acceptInvitation } = require("./sharedLotteries");

function fixture() {
    const records = new Map([
        ["users/creator", { isOwner: true, removed: false }],
        ["users/guest", { isOwner: true, removed: false }],
        ["users/other", { isOwner: true, removed: false }],
        ["lotteries/l1", { ownerId: "creator", campsiteIds: ["pine"], winnerCount: 2, waitlistCount: 3 }],
        ["campsites/pine", { ownerId: "creator", removed: false }],
        ["campsites/river", { ownerId: "guest", removed: false }],
        ["campsites/lake", { ownerId: "guest", removed: false }],
        ["campsites/forest", { ownerId: "other", removed: false }]
    ]);
    let nextId = 0;
    const db = {
        collection: (name) => ({
            doc: (id = "code-" + (++nextId)) => ({ id, path: name + "/" + id })
        }),
        async runTransaction(run) {
            const pending = new Map();
            const get = (ref) => ({
                exists: records.has(ref.path),
                get: (field) => records.get(ref.path)?.[field]
            });
            const result = await run({
                get,
                getAll: (...refs) => refs.map(get),
                create: (ref, data) => pending.set(ref.path, data),
                update: (ref, data) => pending.set(ref.path, { ...records.get(ref.path), ...data })
            });
            for (const [path, data] of pending) records.set(path, data);
            return result;
        }
    };
    return { db, records };
}

test("a contributing owner joins the original lottery without changing shared settings", async () => {
    const { db, records } = fixture();
    const { code } = await createInvitation(db, "creator", { lotteryId: "l1" });
    assert.deepEqual(await acceptInvitation(db, "guest", { code, campsiteId: "river" }), {
        lotteryId: "l1"
    });
    assert.deepEqual(records.get("lotteries/l1"), {
        ownerId: "creator",
        campsiteIds: ["pine", "river"],
        winnerCount: 2,
        waitlistCount: 3,
        contributedCampsiteIds: ["river"],
        contributingOwnerIds: ["guest"]
    });
});

test("only the lottery creator can issue codes, and guests can add only their active sites", async () => {
    const { db, records } = fixture();
    await assert.rejects(createInvitation(db, "guest", { lotteryId: "l1" }), {
        code: "permission-denied"
    });
    const { code } = await createInvitation(db, "creator", { lotteryId: "l1" });
    await assert.rejects(acceptInvitation(db, "guest", { code, campsiteId: "forest" }), {
        code: "permission-denied"
    });
    records.get("campsites/river").removed = true;
    await assert.rejects(acceptInvitation(db, "guest", { code, campsiteId: "river" }), {
        code: "permission-denied"
    });
    assert.deepEqual(records.get("lotteries/l1").campsiteIds, ["pine"]);
});

test("a retry on the same site is safe and the redeemed code cannot add another site", async () => {
    const { db, records } = fixture();
    const { code } = await createInvitation(db, "creator", { lotteryId: "l1" });
    const request = { code, campsiteId: "river" };
    const result = await acceptInvitation(db, "guest", request);
    assert.deepEqual(await acceptInvitation(db, "guest", request), result);
    await assert.rejects(acceptInvitation(db, "other", { code, campsiteId: "forest" }), {
        code: "failed-precondition"
    });
    assert.deepEqual(records.get("lotteries/l1").campsiteIds, ["pine", "river"]);
});

test("an undrawn lottery cannot accept a duplicate campsite", async () => {
    const { db, records } = fixture();
    const first = await createInvitation(db, "creator", { lotteryId: "l1" });
    const second = await createInvitation(db, "creator", { lotteryId: "l1" });
    await acceptInvitation(db, "guest", { code: first.code, campsiteId: "river" });
    await assert.rejects(acceptInvitation(db, "guest", { code: second.code, campsiteId: "river" }), {
        code: "failed-precondition"
    });
    assert.deepEqual(records.get("lotteries/l1").campsiteIds, ["pine", "river"]);
});

test("accepting a code after the draw fails without consuming the code", async () => {
    const { db, records } = fixture();
    const { code } = await createInvitation(db, "creator", { lotteryId: "l1" });
    records.get("lotteries/l1").drawCompletedAt = new Date();
    await assert.rejects(acceptInvitation(db, "guest", { code, campsiteId: "river" }), {
        code: "failed-precondition"
    });
    assert.equal(records.get("lotteryOwnerInvitations/" + code).acceptedBy, undefined);
});
