
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

        // Draw from entrants who are still waiting and have an active entrant profile.
        const waitingQuery = db
            .collection("lotteryParticipations")
            .where("lotteryId", "==", lotteryId)
            .where("status", "==", "WAITING");

        const waitingSnap = await transaction.get(waitingQuery);
        const entrantIds = waitingSnap.docs.map((doc) => doc.get("entrantId"));
        const profileRefs = entrantIds.map((id) => db.collection("users").doc(id));
        const profiles = profileRefs.length === 0
            ? []
            : await transaction.getAll(...profileRefs, {
                fieldMask: ["isEntrant", "removed", "notificationEnabled"]
            });

        const eligibleProfiles = new Map(
            profiles
                .filter((profile) =>
                    profile.exists &&
                    profile.get("removed") !== true &&
                    profile.get("isEntrant") !== false
                )
                .map((profile) => [profile.id, profile])
        );
        const winners = selectWinners([...eligibleProfiles.keys()], winnerCount);
        const winnerSet = new Set(winners);

        for (const participantDoc of waitingSnap.docs) {
            const entrantId = participantDoc.get("entrantId");
            const profile = eligibleProfiles.get(entrantId);

            if (!profile) {
                transaction.update(participantDoc.ref, { status: "CANCELLED" });
                continue;
            }

            const selected = winnerSet.has(entrantId);
            const status = selected ? "INVITED" : "NOT_SELECTED";
            transaction.update(participantDoc.ref, { status });

            // Notification opt-out affects only whether an alert is created.
            if (profile.get("notificationEnabled") === false) {
                continue;
            }

            const notificationRef = db
                .collection("notifications")
                .doc(`lottery-result-${participantDoc.id}`);

            transaction.set(notificationRef, {
                recipientId: entrantId,
                senderId: null,
                type: "SYSTEM",
                content: selected
                    ? "Congratulations! You have been selected in the lottery."
                    : "Unfortunately, you were not selected in the lottery.",
                lotteryId: lotteryId,
                reservationId: null,
                campsiteId: null,
                sendStatus: "SENT",
                createdAt: now
            });
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

