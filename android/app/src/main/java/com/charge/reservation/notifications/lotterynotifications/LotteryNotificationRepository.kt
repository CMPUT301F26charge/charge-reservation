package com.charge.reservation.notifications.lotterynotifications

import com.charge.reservation.booking.BookingException
import com.charge.reservation.booking.FirestorePaths
import com.charge.reservation.booking.toLottery
import com.charge.reservation.booking.toParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything the lottery notification screens need, already resolved. */
data class LotteryNotificationData(
    val lotteryId: String,
    val lotteryName: String,
    val lotteryCode: String,
    val status: ParticipationStatus,
    val bookingDeadline: Instant?,
    val deadlineText: String,
    /** The reservation made with this invitation, if it has been used. */
    val reservation: Reservation?,
    /** Name of the reserved campsite, if known. */
    val reservationCampsiteName: String?
) {
    fun deadlinePassed(now: Instant = Instant.now()): Boolean =
        bookingDeadline?.let { now.isAfter(it) } ?: false
}

/**
 * Loads one entrant's participation in a lottery for the notification screens,
 * and declines invitations.
 *
 * Booking is handled by [com.charge.reservation.booking.InvitationBookingRepository];
 * this class reuses its collection names ([FirestorePaths]) and Firestore mappers
 * so both read the database the same way.
 */
class LotteryNotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Returns null if the participation or lottery does not exist. */
    suspend fun load(lotteryId: String, entrantId: String): LotteryNotificationData? {
        val participation = findParticipation(lotteryId, entrantId)?.toParticipation()
            ?: return null

        val lotterySnap = db.collection(FirestorePaths.LOTTERIES).document(lotteryId).get().await()
        if (!lotterySnap.exists()) return null
        val lottery = lotterySnap.toLottery()
        val zone = lottery.timeZone ?: ZoneId.systemDefault()

        val campsiteNames: Map<String, String> = lottery.campsiteIds.mapNotNull { id ->
            db.collection(FirestorePaths.CAMPSITES).document(id).get().await()
                .takeIf { it.exists() }
                ?.let { id to (it.get("name") as? String).orEmpty() }
        }.toMap()

        val reservation = participation.reservationId
            ?.takeIf { it.isNotBlank() }
            ?.let { id ->
                db.collection(FirestorePaths.RESERVATIONS).document(id).get().await()
                    .takeIf { it.exists() }
                    ?.toReservation()
            }

        val firstName = lottery.campsiteIds.firstNotNullOfOrNull { id ->
            campsiteNames[id]?.takeIf { it.isNotBlank() }
        }
        val lotteryName = when {
            firstName == null -> "Lottery"
            lottery.campsiteIds.size > 1 -> "$firstName Lottery (+${lottery.campsiteIds.size - 1} more)"
            else -> "$firstName Lottery"
        }

        val deadline = lottery.bookingDeadline
        val deadlineText = deadline?.let {
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z", Locale.getDefault())
                .withZone(zone)
                .format(it)
        } ?: "No deadline set"

        return LotteryNotificationData(
            lotteryId = lotteryId,
            lotteryName = lotteryName,
            lotteryCode = "LOT-" + lotteryId.take(6).uppercase(),
            status = participation.status,
            bookingDeadline = deadline,
            deadlineText = deadlineText,
            reservation = reservation,
            reservationCampsiteName = reservation?.campsiteId
                ?.let { campsiteNames[it] }
                ?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Declines the entrant's invitation by setting their participation status to DECLINED.
     *
     * That locks them out of booking: [com.charge.reservation.booking.InvitationBookingRepository]
     * refuses to load or book any invitation that isn't INVITED.
     *
     * Runs in a transaction so it only succeeds if the invitation is still INVITED and unused.
     *
     * @throws BookingException if the participation is missing or no longer INVITED
     */
    suspend fun declineInvitation(lotteryId: String, entrantId: String) {
        val ref = findParticipation(lotteryId, entrantId)?.reference
            ?: throw BookingException("Your lottery entry could not be found.")

        db.runTransaction { tx ->
            val current = tx.get(ref).toParticipation()
            if (current.status != ParticipationStatus.INVITED || current.reservationId != null) {
                throw BookingException("This invitation is no longer available.")
            }
            tx.update(ref, "status", ParticipationStatus.DECLINED.name)
            null
        }.await()
    }

    private suspend fun findParticipation(lotteryId: String, entrantId: String): DocumentSnapshot? =
        db.collection(FirestorePaths.PARTICIPATIONS)
            .whereEqualTo("lotteryId", lotteryId)
            .whereEqualTo("entrantId", entrantId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
}

// Same storage conventions as InvitationBookingRepository:
// LocalDate -> "yyyy-MM-dd" string (a Timestamp is read as UTC), Instant -> Timestamp.
private fun DocumentSnapshot.toReservation() = Reservation(
    id = id,
    campsiteId = (get("campsiteId") as? String).orEmpty(),
    lotteryId = (get("lotteryId") as? String).orEmpty(),
    entrantId = (get("entrantId") as? String).orEmpty(),
    createdAt = (get("createdAt") as? Timestamp)?.toDate()?.toInstant(),
    checkInDate = get("checkInDate").toLocalDateOrNull(),
    checkOutDate = get("checkOutDate").toLocalDateOrNull(),
    guestCount = (get("guestCount") as? Number)?.toInt(),
    status = (get("status") as? String)
        ?.let { s -> ReservationStatus.entries.firstOrNull { it.name == s } }
        ?: ReservationStatus.CONFIRMED,
    statusChangedAt = (get("statusChangedAt") as? Timestamp)?.toDate()?.toInstant(),
    statusChangeReason = get("statusChangeReason") as? String
)

private fun Any?.toLocalDateOrNull(): LocalDate? = when (this) {
    is String -> runCatching { LocalDate.parse(this) }.getOrNull()
    is Timestamp -> toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()
    else -> null
}
