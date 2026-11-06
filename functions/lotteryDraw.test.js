
const test = require("node:test");
const assert = require("node:assert/strict");

const { selectWinners } = require("./lotteryDraw");

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
