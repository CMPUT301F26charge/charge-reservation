
const { randomInt } = require("node:crypto");

/**
 * Randomly selects distinct winners.
 * Every eligible entrant has an equal chance of selection.
 *
 * This function does not change the original array.
 */
function selectWinners(entrantIds, winnerCount) {
    if (!Number.isInteger(winnerCount) || winnerCount < 0) {
        throw new Error("Invalid winner count.");
    }

    const shuffled = [...entrantIds];

    // Fisher-Yates shuffle with secure, unbiased random integers.
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = randomInt(i + 1);

        [shuffled[i], shuffled[j]] = [
            shuffled[j],
            shuffled[i]
        ];
    }

    return shuffled.slice(
        0,
        Math.min(winnerCount, shuffled.length)
    );
}

/**
 * Draws one lottery after registration has closed.
 *
 * All participant changes and draw completion are committed
 * atomically in a Firestore transaction.
 *
 * Requires the Firebase Admin Firestore database instance.
 */
async function drawClosedLottery(db, lotteryId) {
    if (!lotteryId) {
        throw new Error("Lottery ID is required.");
    }

    const lotteryRef = db
        .collection("lotteries")
        .doc(lotteryId);

    return db.runTransaction(async (transaction) => {
        // Read the lottery before any writes.
        const lotterySnap = await transaction.get(lotteryRef);

        if (!lotterySnap.exists) {
            throw new Error("Lottery not found.");
        }

        const lottery = lotterySnap.data();

        // Never draw a lottery twice.
        if (lottery.drawCompletedAt != null) {
            return {
                status: "already_completed",
                lotteryId
            };
        }

        const closingTimestamp = lottery.registrationEndAt;

        if (!closingTimestamp ||
            typeof closingTimestamp.toDate !== "function") {
            throw new Error("Invalid registration closing time.");
        }

        const now = new Date();
        const closesAt = closingTimestamp.toDate();

        if (now < closesAt) {
            return {
                status: "not_closed",
                lotteryId
            };
        }

        const winnerCount = lottery.winnerCount;

        if (!Number.isInteger(winnerCount) || winnerCount <= 0) {
            throw new Error("Invalid configured winner count.");
        }

        // Only WAITING entrants can be selected.
        // The limit protects this single-transaction implementation
        // from exceeding Firestore's transaction write limits.
        const waitingQuery = lotteryRef
            .collection("participants")
            .where("status", "==", "WAITING")
            .limit(451);

        const waitingSnap = await transaction.get(waitingQuery);

        if (waitingSnap.size > 450) {
            throw new Error(
                "More than 450 waiting entrants. " +
                "A larger-scale draw strategy is required."
            );
        }

        const entrantIds = waitingSnap.docs.map(
            (doc) => doc.id
        );

        const winners = selectWinners(
            entrantIds,
            winnerCount
        );

        const winnerSet = new Set(winners);

        // No writes occur until all reads finish.
        for (const participantDoc of waitingSnap.docs) {
            const selected = winnerSet.has(participantDoc.id);

            transaction.update(
                participantDoc.ref,
                {
                    status: selected
                        ? "INVITED"
                        : "NOT_SELECTED"
                }
            );
        }

        // Marks completion even if nobody entered.
        // This prevents repeated draws of empty lotteries.
        transaction.update(lotteryRef, {
            drawCompletedAt: now,
            waitlistCount: 0
        });

        return {
            status: "completed",
            lotteryId,
            totalEntrants: entrantIds.length,
            winnersSelected: winners.length,
            winnerIds: winners
        };
    });
}

module.exports = {
    selectWinners,
    drawClosedLottery
};

