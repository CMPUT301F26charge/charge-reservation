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
        val lotteryRef = db.collection(FirestorePaths.LOTTERIES).document(lotteryId)
        val ref = participationRef(lotteryId, entrantId)

        val lotterySnap = lotteryRef.get().await()
        if (!lotterySnap.exists()) throw LotteryActionException("This lottery no longer exists.")
        lotterySnap.toLottery().waitlistCapacity?.let { capacity ->
            val waiting = db.collection(FirestorePaths.PARTICIPATIONS)
                .whereEqualTo("lotteryId", lotteryId)
                .whereEqualTo("status", ParticipationStatus.WAITING.name)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            if (waiting >= capacity) throw LotteryActionException("This lottery's waitlist is full.")
        }

        db.runTransaction<Any?> { tx ->
            val lottery = tx.get(lotteryRef).also {
                if (!it.exists()) throw LotteryActionException("This lottery no longer exists.")
            }.toLottery()

            val now = Instant.now()
            val start = lottery.registrationStartAt
            val end = lottery.registrationEndAt
            if (lottery.drawCompletedAt != null ||
                start == null || end == null ||
                now.isBefore(start) || !now.isBefore(end)
            ) {
                throw LotteryActionException("Registration for this lottery isn't open.")
            }

            val existing = tx.get(ref)
            val status = if (existing.exists()) existing.toParticipation().status else null
            when (status) {
                null -> tx.set(
                    ref,
                    mapOf(
                        "lotteryId" to lotteryId,
                        "entrantId" to entrantId,
                        "joinedAt" to Timestamp.now(),
                        "status" to ParticipationStatus.WAITING.name,
                        "reservationId" to null
                    )
                )
                ParticipationStatus.WITHDRAWN -> tx.update(
                    ref,
                    mapOf(
                        "status" to ParticipationStatus.WAITING.name,
                        "joinedAt" to Timestamp.now()
                    )
                )
                ParticipationStatus.WAITING -> Unit // already in; nothing to do
                else -> throw LotteryActionException("You can't rejoin this lottery.")
            }
            null
        }.await()
    }

    /** Withdraws from the lottery. Only possible while WAITING and before the draw. */
    suspend fun leave(lotteryId: String, entrantId: String) {
        val lotteryRef = db.collection(FirestorePaths.LOTTERIES).document(lotteryId)
        val ref = participationRef(lotteryId, entrantId)

        db.runTransaction<Any?> { tx ->
            val lotterySnap = tx.get(lotteryRef)
            if (lotterySnap.exists() && lotterySnap.toLottery().drawCompletedAt != null) {
                throw LotteryActionException("The draw has already happened, so you can't leave now.")
            }

            val snap = tx.get(ref)
            if (!snap.exists() || snap.toParticipation().status != ParticipationStatus.WAITING) {
                throw LotteryActionException("You aren't currently in this lottery.")
            }

            tx.update(ref, "status", ParticipationStatus.WITHDRAWN.name)
            null
        }.await()
    }
}