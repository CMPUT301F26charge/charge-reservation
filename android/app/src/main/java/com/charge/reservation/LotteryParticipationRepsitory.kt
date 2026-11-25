package com.charge.reservation

import com.charge.reservation.booking.FirestorePaths
import com.charge.reservation.booking.toLottery
import com.charge.reservation.booking.toParticipation
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant

/** An error whose message is safe to show directly to the user. */
class LotteryActionException(message: String) : Exception(message)

/**
 * Lets an entrant join or leave a lottery before its draw.
 *
 * Each entrant has at most one participation document per lottery, with the ID
 * "<lotteryId>_<entrantId>". Joining creates it (WAITING). Leaving sets it to WITHDRAWN
 * rather than deleting it, which is what [ParticipationStatus.WITHDRAWN] is for.
 */
class LotteryParticipationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun participationRef(lotteryId: String, entrantId: String) =
        db.collection(FirestorePaths.PARTICIPATIONS).document("${lotteryId}_${entrantId}")

    /** The entrant's participation in the lottery, or null if they never joined. */
    suspend fun getMine(lotteryId: String, entrantId: String): LotteryParticipation? {
        val snap = participationRef(lotteryId, entrantId).get().await()
        return if (snap.exists()) snap.toParticipation() else null
    }

    /**
     * Joins the lottery (or rejoins after withdrawing).
     *
     * The registration window and draw state are checked inside a transaction.
     * The waitlist-capacity check is best-effort: Firestore client transactions can't run
     * count queries, so two people joining the last spot at the same moment could both get in.
     * Enforce it strictly with a Cloud Function or a counter field if that matters.
     */

    suspend fun join(lotteryId: String, entrantId: String) {
        val lotteryRef = db.collection(FirestorePaths.LOTTERIES)
            .document(lotteryId)
        val ref = participationRef(lotteryId, entrantId)

        db.runTransaction<Any?> { tx ->
            // Read both documents before making any writes.
            val lotterySnap = tx.get(lotteryRef)
            val existing = tx.get(ref)

            if (!lotterySnap.exists()) {
                throw LotteryActionException("This lottery no longer exists.")
            }

            val lottery = lotterySnap.toLottery()
            val now = Instant.now()

            if (
                lottery.drawCompletedAt != null ||
                lottery.registrationStartAt == null ||
                lottery.registrationEndAt == null ||
                now.isBefore(lottery.registrationStartAt) ||
                !now.isBefore(lottery.registrationEndAt)
            ) {
                throw LotteryActionException(
                    "Registration for this lottery isn't open."
                )
            }

            val status = if (existing.exists()) {
                existing.toParticipation().status
            } else {
                null
            }

            if (status == ParticipationStatus.WAITING) {
                return@runTransaction null
            }

            if (status != null && status != ParticipationStatus.WITHDRAWN) {
                throw LotteryActionException(
                    "You can't rejoin this lottery."
                )
            }

            val count = lotterySnap.getLong("waitlistCount")
                ?: throw LotteryActionException(
                    "Lottery waitlist count is not initialized."
                )

            val capacity = lottery.waitlistCapacity

            if (capacity != null && count >= capacity.toLong()) {
                throw LotteryActionException(
                    "This lottery's waitlist is full."
                )
            }

            if (status == ParticipationStatus.WITHDRAWN) {
                tx.update(
                    ref,
                    mapOf(
                        "status" to ParticipationStatus.WAITING.name,
                        "joinedAt" to Timestamp.now()
                    )
                )
            } else {
                tx.set(
                    ref,
                    mapOf(
                        "lotteryId" to lotteryId,
                        "entrantId" to entrantId,
                        "joinedAt" to Timestamp.now(),
                        "status" to ParticipationStatus.WAITING.name,
                        "reservationId" to null
                    )
                )
            }

            // Atomic counter update within the same transaction.
            tx.update(lotteryRef, "waitlistCount", count + 1L)

            null
        }.await()
    }

    /** Withdraws from the lottery. Only possible while WAITING and before the draw. */

    suspend fun leave(lotteryId: String, entrantId: String) {
        val lotteryRef = db.collection(FirestorePaths.LOTTERIES)
            .document(lotteryId)
        val ref = participationRef(lotteryId, entrantId)

        db.runTransaction<Any?> { tx ->
            val lotterySnap = tx.get(lotteryRef)
            val participantSnap = tx.get(ref)

            if (!lotterySnap.exists()) {
                throw LotteryActionException("Lottery not found.")
            }

            if (lotterySnap.toLottery().drawCompletedAt != null) {
                throw LotteryActionException(
                    "The draw has already happened."
                )
            }

            if (!participantSnap.exists() ||
                participantSnap.toParticipation().status !=
                ParticipationStatus.WAITING
            ) {
                throw LotteryActionException(
                    "You aren't currently in this lottery."
                )
            }

            val count = lotterySnap.getLong("waitlistCount")
                ?: throw LotteryActionException(
                    "Waitlist counter is not initialized."
                )

            if (count <= 0L) {
                throw LotteryActionException(
                    "Waitlist counter is inconsistent."
                )
            }

            tx.update(
                participantSnap.reference,
                "status",
                ParticipationStatus.WITHDRAWN.name
            )

            tx.update(
                lotteryRef,
                "waitlistCount",
                count - 1L
            )

            null
        }.await()
    }
}