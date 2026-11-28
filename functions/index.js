
const { setGlobalOptions } = require("firebase-functions");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp, getApps } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const logger = require("firebase-functions/logger");

const { drawClosedLottery } = require("./lotteryDraw");
const sharedLotteries = require("./sharedLotteries");

if (getApps().length === 0) {
    initializeApp();
}

setGlobalOptions({
    maxInstances: 1
});

/** Returns a single-use invitation code to the lottery creator. */
exports.createSharedLotteryInvitation = onCall((request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Sign in to invite an owner.");
    return sharedLotteries.createInvitation(getFirestore(), request.auth.uid, request.data);
});

/** Adds one active campsite owned by the authenticated invited owner. */
exports.acceptSharedLotteryInvitation = onCall((request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Sign in to join a shared lottery.");
    return sharedLotteries.acceptInvitation(getFirestore(), request.auth.uid, request.data);
});

/**
 * US 02.05.01
 *
 * Automatically draws lotteries after registration closes.
 * Runs every minute, even when the Android application is closed.
 */
exports.runAutomaticLotteryDraws = onSchedule(
    {
        schedule: "every 1 minutes",
        timeZone: "UTC",
        maxInstances: 1
    },
    async () => {
        const db = getFirestore();

        // Retrieve lotteries whose draw has not completed.
        const snapshot = await db
            .collection("lotteries")
            .where("drawCompletedAt", "==", null)
            .get();

        const now = Date.now();
        let attempted = 0;

        for (const lotteryDoc of snapshot.docs) {

            if (attempted >= 50) {
                break;
            }

            const lottery = lotteryDoc.data();
            const closingTime = lottery.registrationEndAt;

            if (
                !closingTime ||
                typeof closingTime.toDate !== "function"
            ) {
                logger.warn(
                    "Invalid registration closing time:",
                    lotteryDoc.id
                );
                continue;
            }

            // Skip lotteries whose registration is still open.
            if (closingTime.toDate().getTime() > now) {
                continue;
            }

            attempted++;

            try {
                const result = await drawClosedLottery(
                    db,
                    lotteryDoc.id
                );

                logger.info(
                    "Automatic lottery draw result:",
                    result
                );

            } catch (error) {
                logger.error(
                    "Automatic lottery draw failed:",
                    lotteryDoc.id,
                    error
                );
            }
        }
    }
);
