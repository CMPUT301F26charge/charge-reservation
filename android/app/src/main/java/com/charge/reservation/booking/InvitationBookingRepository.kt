package com.charge.reservation.booking

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.model.ReservationStatus

/**
 * Collection names. ADJUST THESE to match your existing database.
 */
object FirestorePaths {
    const val LOTTERIES = "lotteries"
    const val CAMPSITES = "campsites"
    /**
     * Subcollection inside each campsite document that holds its AvailabilityDay records:
     * campsites/{campsiteId}/availability/{dayId}
     */
    const val AVAILABILITY = "availability"
    const val RESERVATIONS = "reservations"
    const val PARTICIPATIONS = "lotteryParticipations"
}

private const val TAG = "InvitationBooking"

/** An error whose message is safe to show directly to the user. */
class BookingException(message: String) : Exception(message)

/** Everything needed to show the invitation page. */
data class InvitationDetails(
    val lottery: Lottery,
    /** Firestore document ID of the entrant's LotteryParticipation. */
    val participationDocId: String,
    val participation: LotteryParticipation,
    /** Campsites the invitation can be used for (removed ones excluded). */
    val campsites: List<Campsite>
)

/** One bookable night, carrying its Firestore document ID so the booking can update it. */
data class NightOption(
    val docId: String,
    val date: LocalDate,
    val capacity: Int,
    val booked: Int
) {
    /** Places still bookable, including when existing bookings exceed a newer limit. */
    val remaining: Int get() = (capacity - booked).coerceAtLeast(0)
}

class InvitationBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    /** Loads the lottery, the signed-in user's invitation, and the campsites. */
    suspend fun loadInvitation(lotteryId: String): InvitationDetails {
        val uid = auth.currentUser?.uid
            ?: throw BookingException("Sign in to open your invitation.")

        val lotterySnap = db.collection(FirestorePaths.LOTTERIES).document(lotteryId).get().await()
        if (!lotterySnap.exists()) throw BookingException("This lottery no longer exists.")
        val lottery = lotterySnap.toLottery()

        val participationSnap = db.collection(FirestorePaths.PARTICIPATIONS)
            .whereEqualTo("lotteryId", lotteryId)
            .whereEqualTo("entrantId", uid)
            .limit(1)
            .get().await()
            .documents.firstOrNull()
            ?: throw BookingException("You are not entered in this lottery.")
        val participation = participationSnap.toParticipation()

        when (participation.status) {
            ParticipationStatus.INVITED -> Unit
            ParticipationStatus.USED -> throw BookingException("You have already used this invitation.")
            ParticipationStatus.EXPIRED -> throw BookingException("This invitation has expired.")
            ParticipationStatus.DECLINED -> throw BookingException("You declined this invitation.")
            else -> throw BookingException("You don't have an active invitation for this lottery.")
        }

        lottery.bookingDeadline?.let {
            if (Instant.now().isAfter(it)) {
                throw BookingException("The booking deadline for this invitation has passed.")
            }
        }

        val campsites = lottery.campsiteIds
            .map { db.collection(FirestorePaths.CAMPSITES).document(it).get().await() }
            .filter { it.exists() }
            .map { it.toCampsite() }
            .filterNot { it.removed }
        if (campsites.isEmpty()) throw BookingException("No campsites are available for this invitation.")

        return InvitationDetails(lottery, participationSnap.id, participation, campsites)
    }

    /**
     * Loads the nights of [campsiteId] that fall inside the lottery's booking window
     * (check-in on [Lottery.bookingStartDate], check-out by [Lottery.bookingEndDate]),
     * skipping nights that have already passed. Sorted by date.
     *
     * Nights that are full are still returned (remaining == 0) so the UI can show them
     * greyed out and make gaps in availability obvious.
     */
    suspend fun loadNights(campsiteId: String, lotteryId: String): List<NightOption> {
        val lotterySnapshot = db.collection(FirestorePaths.LOTTERIES)
            .document(lotteryId)
            .get(Source.SERVER)
            .await()

        if (!lotterySnapshot.exists()) {
            throw BookingException("This lottery no longer exists.")
        }

        val lottery = lotterySnapshot.toLottery()
        if (campsiteId !in lottery.campsiteIds) {
            throw BookingException("This campsite is no longer part of your invitation.")
        }

        val windowStart = lottery.bookingStartDate
        val lastCheckOut = lottery.bookingEndDate
        if (windowStart == null || lastCheckOut == null || !lastCheckOut.isAfter(windowStart)) {
            throw BookingException("This lottery doesn't have a valid booking period.")
        }

        val today = LocalDate.now(lottery.timeZone ?: ZoneId.systemDefault())
        val earliest = maxOf(windowStart, today)

        val maxOccupancy = db.collection(FirestorePaths.CAMPSITES)
            .document(campsiteId)
            .get(Source.SERVER)
            .await()
            .getLong("maxOccupancy")

        // Each campsite keeps its own nights in campsites/{campsiteId}/availability,
        // so the whole subcollection is read and date filtering is done here.
        return availabilityOf(campsiteId)
            .get().await()
            .documents
            .mapNotNull { doc ->
                val day = doc.toAvailabilityDay()
                val date = day.date
                val capacity = day.guestCapacity
                when {
                    date == null || capacity == null -> {
                        Log.w(TAG, "Skipping availability doc ${doc.id}: unreadable date or guestCapacity")
                        null
                    }
                    date.isBefore(earliest) || !date.isBefore(lastCheckOut) -> null
                    else -> {
                        val limit = minOf(capacity.toLong(), maxOccupancy ?: Long.MAX_VALUE)
                        NightOption(doc.id, date, limit.toInt(), day.bookedGuests)
                    }
                }
            }
            .sortedBy { it.date }
    }

    /** The availability subcollection of one campsite: campsites/{campsiteId}/availability. */
    private fun availabilityOf(campsiteId: String) =
        db.collection(FirestorePaths.CAMPSITES)
            .document(campsiteId)
            .collection(FirestorePaths.AVAILABILITY)

    // ---------------------------------------------------------------------
    // Booking
    // ---------------------------------------------------------------------

    /**
     * Books [nights] at [campsite] for [guestCount] guests, atomically.
     *
     * In one Firestore transaction this:
     *  1. re-checks the invitation is still INVITED and before the deadline,
     *  2. re-reads every night and confirms there is still room,
     *  3. increments `bookedGuests` on every night,
     *  4. creates the Reservation (CONFIRMED),
     *  5. marks the participation USED and links the reservation.
     *
     * If anything fails (e.g. someone else took the last spot), nothing is written.
     *
     * @return the new reservation's ID
     */
    suspend fun bookStay(
        details: InvitationDetails,
        campsite: Campsite,
        nights: List<NightOption>,
        guestCount: Int
    ): String {
        val uid = auth.currentUser?.uid
            ?: throw BookingException("Sign in to book your stay.")

        // Cheap client-side validation first.
        val sorted = nights.sortedBy { it.date }
        if (sorted.isEmpty()) throw BookingException("Select at least one night.")
        if (sorted.zipWithNext().any { (a, b) -> a.date.plusDays(1) != b.date }) {
            throw BookingException("Selected nights must be consecutive.")
        }
        if (guestCount < 1) throw BookingException("Add at least one guest.")

        val checkIn = sorted.first().date
        val checkOut = sorted.last().date.plusDays(1)

        val lotteryRef = db.collection(FirestorePaths.LOTTERIES).document(details.lottery.id)
        val participationRef = db.collection(FirestorePaths.PARTICIPATIONS).document(details.participationDocId)
        val reservationRef = db.collection(FirestorePaths.RESERVATIONS).document() // auto ID
        val nightRefs = sorted.map { availabilityOf(campsite.id).document(it.docId) }

        return db.runTransaction<String> { tx ->
            // ---- All reads must come before any writes ----
            val participation = tx.get(participationRef).toParticipation()
            if (participation.entrantId != uid ||
                participation.lotteryId != lotteryRef.id ||
                participation.status != ParticipationStatus.INVITED ||
                participation.reservationId != null
            ) {
                throw BookingException("This invitation has already been used or is no longer valid.")
            }

            val lotterySnap = tx.get(lotteryRef)
            if (!lotterySnap.exists()) {
                throw BookingException("This lottery no longer exists.")
            }

            val currentLottery = lotterySnap.toLottery()
            val now = Instant.now()
            val deadline = currentLottery.bookingDeadline
            if (deadline != null && now.isAfter(deadline)) {
                throw BookingException("The booking deadline for this invitation has passed.")
            }

            val windowStart = currentLottery.bookingStartDate
            val windowEnd = currentLottery.bookingEndDate
            if (windowStart == null || windowEnd == null || !windowEnd.isAfter(windowStart)) {
                throw BookingException("This lottery doesn't have a valid booking period.")
            }

            val zone = currentLottery.timeZone ?: ZoneId.systemDefault()
            val today = now.atZone(zone).toLocalDate()
            if (checkIn.isBefore(today)) {
                throw BookingException("The selected check-in date has already passed.")
            }

            if (checkIn.isBefore(windowStart) || checkOut.isAfter(windowEnd)) {
                throw BookingException(
                    "Choose a stay starting on or after $windowStart " +
                            "with checkout by $windowEnd."
                )
            }

            if (campsite.id !in currentLottery.campsiteIds) {
                throw BookingException("This campsite is no longer part of your invitation.")
            }

            val campsiteSnap = tx.get(
                db.collection(FirestorePaths.CAMPSITES).document(campsite.id)
            )
            if (!campsiteSnap.exists() || campsiteSnap.getBoolean("removed") == true) {
                throw BookingException("This campsite is no longer available.")
            }

            val maxOccupancy = campsiteSnap.getLong("maxOccupancy")
            if (maxOccupancy != null && guestCount.toLong() > maxOccupancy) {
                throw BookingException("This campsite allows up to $maxOccupancy guests.")
            }

            val nightSnaps = nightRefs.map { tx.get(it) }
            nightSnaps.forEachIndexed { index, snap ->
                val requestedDate = sorted[index].date
                if (!snap.exists()) {
                    throw BookingException("$requestedDate is no longer available.")
                }

                val day = snap.toAvailabilityDay()
                val capacity = day.guestCapacity
                if (day.date != requestedDate ||
                    day.campsiteId != campsite.id ||
                    capacity == null ||
                    day.bookedGuests !in 0..capacity ||
                    minOf(capacity.toLong(), maxOccupancy ?: Long.MAX_VALUE) -
                        day.bookedGuests < guestCount
                ) {
                    throw BookingException(
                        "$requestedDate no longer has room for $guestCount guests."
                    )
                }
            }

            // ---- Writes ----
            nightRefs.forEach { ref ->
                tx.update(ref, "bookedGuests", FieldValue.increment(guestCount.toLong()))
            }

            val bookingTimestamp = Timestamp.now()
            tx.set(
                reservationRef,
                mapOf(
                    "campsiteId" to campsite.id,
                    "lotteryId" to details.lottery.id,
                    "entrantId" to uid,
                    "createdAt" to bookingTimestamp,
                    "checkInDate" to checkIn.toString(),   // ISO yyyy-MM-dd
                    "checkOutDate" to checkOut.toString(),
                    "guestCount" to guestCount.toLong(),
                    "status" to ReservationStatus.CONFIRMED.name,
                    "statusChangedAt" to bookingTimestamp,
                    "statusChangeReason" to null
                )
            )

            tx.update(
                participationRef,
                mapOf(
                    "status" to ParticipationStatus.USED.name,
                    "reservationId" to reservationRef.id
                )
            )
            reservationRef.id
        }.await()
    }
}

// -------------------------------------------------------------------------
// Firestore -> model mapping
//
// Firestore can't store java.time types directly, so this file assumes:
//   Instant   -> Firestore Timestamp
//   LocalDate -> ISO string "yyyy-MM-dd" (a Timestamp is also accepted, read as UTC)
//   ZoneId    -> string such as "America/Edmonton"
//   enums     -> their name() string
// Change these helpers if your database stores them differently.
// -------------------------------------------------------------------------

private fun Any?.toInstantOrNull(): Instant? = when (this) {
    is Timestamp -> Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    is Long -> Instant.ofEpochMilli(this)
    else -> null
}

private fun Any?.toLocalDateOrNull(): LocalDate? = when (this) {
    is String -> runCatching { LocalDate.parse(this) }.getOrNull()
    // A Timestamp is read as UTC midnight. If you store dates this way in another zone, adjust here.
    is Timestamp -> Instant.ofEpochSecond(seconds, nanoseconds.toLong()).atZone(ZoneOffset.UTC).toLocalDate()
    else -> null
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { runCatching { enumValueOf<E>(it) }.getOrNull() }

private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

fun DocumentSnapshot.toLottery() = Lottery(
    id = id,
    ownerId = getString("ownerId").orEmpty(),
    campsiteIds = stringList("campsiteIds"),
    registrationStartAt = get("registrationStartAt").toInstantOrNull(),
    registrationEndAt = get("registrationEndAt").toInstantOrNull(),
    bookingDeadline = get("bookingDeadline").toInstantOrNull(),
    bookingStartDate = get("bookingStartDate").toLocalDateOrNull(),
    bookingEndDate = get("bookingEndDate").toLocalDateOrNull(),
    timeZone = getString("timeZone")?.let { runCatching { ZoneId.of(it) }.getOrNull() },
    waitlistCapacity = getLong("waitlistCapacity")?.toInt(),
    winnerCount = getLong("winnerCount")?.toInt(),
    drawCompletedAt = get("drawCompletedAt").toInstantOrNull(),
    contributedCampsiteIds = stringList("contributedCampsiteIds")
)

private fun DocumentSnapshot.toCampsite() = Campsite(
    id = id,
    ownerId = getString("ownerId").orEmpty(),
    name = getString("name").orEmpty(),
    description = getString("description").orEmpty(),
    promoImageUrls = stringList("promoImageUrls"),
    tags = stringList("tags"),
    maxOccupancy = getLong("maxOccupancy")?.toInt(),
    pricePerNight = getDouble("pricePerNight"),
    averageRating = getDouble("averageRating"),
    removed = getBoolean("removed") ?: false,
    rules = getString("rules").orEmpty(),
    cancellationPolicy = getString("cancellationPolicy").orEmpty(),
    location = getString("location").orEmpty(),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude")
)

fun DocumentSnapshot.toParticipation() = LotteryParticipation(
    lotteryId = getString("lotteryId").orEmpty(),
    entrantId = getString("entrantId").orEmpty(),
    joinedAt = get("joinedAt").toInstantOrNull(),
    status = getString("status").toEnumOrNull<ParticipationStatus>() ?: ParticipationStatus.WAITING,
    reservationId = getString("reservationId"),
    entrantDisplayName = get("entrantDisplayName") as? String
)

private fun DocumentSnapshot.toAvailabilityDay() = AvailabilityDay(
    // Days live under their campsite, so fall back to the parent document's ID
    // if the day itself doesn't store campsiteId.
    campsiteId = (get("campsiteId") as? String) ?: reference.parent.parent?.id.orEmpty(),
    date = get("date").toLocalDateOrNull(),
    guestCapacity = getLong("guestCapacity")?.toInt(),
    bookedGuests = getLong("bookedGuests")?.toInt() ?: 0
)
