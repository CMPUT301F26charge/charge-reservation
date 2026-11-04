package com.charge.reservation
import com.charge.reservation.model.ParticipationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything the lottery notification screens need, already resolved. */
data class LotteryNotificationData(
    val lotteryName: String,
    val lotteryCode: String,
    val status: ParticipationStatus,
    val bookingDeadline: Instant?,
    val deadlineText: String
)

/**
 * Loads one entrant's participation in a lottery plus the lottery and campsite
 * details shown on the notification screens.
 *
 * Assumed Firestore layout (change the strings below if yours differs):
 *  - "lotteryParticipations": fields lotteryId, entrantId, status (enum name string)
 *  - "lotteries": fields campsiteIds (list of strings), bookingDeadline (Timestamp),
 *    timeZone (string such as "America/Edmonton")
 *  - "campsites": field name
 */
class LotteryNotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Returns null if the participation or lottery does not exist. */
    suspend fun load(lotteryId: String, entrantId: String): LotteryNotificationData? {
        val participation = db.collection("lotteryParticipations")
            .whereEqualTo("lotteryId", lotteryId)
            .whereEqualTo("entrantId", entrantId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull() ?: return null

        val status = participation.getString("status")
            ?.let { runCatching { ParticipationStatus.valueOf(it) }.getOrNull() }
            ?: ParticipationStatus.WAITING

        val lottery = db.collection("lotteries").document(lotteryId).get().await()
        if (!lottery.exists()) return null

        val deadline = lottery.getTimestamp("bookingDeadline")?.toDate()?.toInstant()
        val zone = lottery.getString("timeZone")
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()

        val firstCampsiteId = (lottery.get("campsiteIds") as? List<*>)
            ?.filterIsInstance<String>()
            ?.firstOrNull()
        val campsiteName = firstCampsiteId?.let {
            db.collection("campsites").document(it).get().await().getString("name")
        }

        val deadlineText = deadline?.let {
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                .withZone(zone)
                .format(it)
        } ?: "Not set"

        return LotteryNotificationData(
            lotteryName = if (campsiteName.isNullOrBlank()) "Lottery" else "$campsiteName Lottery",
            lotteryCode = "LOT-" + lotteryId.take(6).uppercase(),
            status = status,
            bookingDeadline = deadline,
            deadlineText = deadlineText
        )
    }

    /**
     * Declines the entrant's invitation by setting their participation status to DECLINED.
     *
     * Runs in a transaction so it only succeeds if the status is still INVITED
     * (for example, it won't overwrite USED or EXPIRED).
     *
     * @throws IllegalStateException if the participation is missing or no longer INVITED
     */
    suspend fun declineInvitation(lotteryId: String, entrantId: String) {
        val ref = db.collection("lotteryParticipations")
            .whereEqualTo("lotteryId", lotteryId)
            .whereEqualTo("entrantId", entrantId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.reference
            ?: throw IllegalStateException("Participation not found")

        db.runTransaction { transaction ->
            val current = transaction.get(ref).getString("status")
            if (current != ParticipationStatus.INVITED.name) {
                throw IllegalStateException("Invitation is no longer available")
            }
            transaction.update(ref, "status", ParticipationStatus.DECLINED.name)
            null
        }.await()
    }
}