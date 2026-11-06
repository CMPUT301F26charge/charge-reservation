package com.charge.reservation.booking

import android.util.Log
import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.model.ReservationStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Collection names. ADJUST THESE to match your existing database.
 */
object FirestorePaths {
    const val LOTTERIES = "lotteries"
    const val CAMPSITES = "campsites"
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
    val remaining: Int get() = capacity - booked
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
    suspend fun loadNights(campsiteId: String, lottery: Lottery): List<NightOption> {
        val windowStart = lottery.bookingStartDate ?: return emptyList()
        val lastCheckOut = lottery.bookingEndDate ?: return emptyList()
        val today = LocalDate.now(lottery.timeZone ?: ZoneId.systemDefault())
        val earliest = if (windowStart.isAfter(today)) windowStart else today

        // Single-field filter => no composite index required. Date filtering is done here.
        return db.collection(FirestorePaths.AVAILABILITY)
            .whereEqualTo("campsiteId", campsiteId)
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
                    else -> NightOption(doc.id, date, capacity, day.bookedGuests)
                }
            }
            .sortedBy { it.date }
    }

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
        campsite.maxOccupancy?.let {
            if (guestCount > it) throw BookingException("This campsite allows up to $it guests.")
        }
        if (campsite.id !in details.lottery.campsiteIds) {
            throw BookingException("This campsite isn't part of your invitation.")
        }

        val checkIn = sorted.first().date
        val checkOut = sorted.last().date.plusDays(1)

        val lotteryRef = db.collection(FirestorePaths.LOTTERIES).document(details.lottery.id)
        val participationRef = db.collection(FirestorePaths.PARTICIPATIONS).document(details.participationDocId)
        val reservationRef = db.collection(FirestorePaths.RESERVATIONS).document() // auto ID
        val nightRefs = sorted.map { db.collection(FirestorePaths.AVAILABILITY).document(it.docId) }

        return db.runTransaction<String> { tx ->
            // ---- All reads must come before any writes ----
            val participation = tx.get(participationRef).toParticipation()
            if (participation.entrantId != uid ||
                participation.status != ParticipationStatus.INVITED ||
                participation.reservationId != null
            ) {
                throw BookingException("This invitation has already been used or is no longer valid.")
            }

            val lotterySnap = tx.get(lotteryRef)
            if (!lotterySnap.exists()) throw BookingException("This lottery no longer exists.")
            lotterySnap.toLottery().bookingDeadline?.let {
                if (Instant.now().isAfter(it)) {
                    throw BookingException("The booking deadline for this invitation has passed.")
                }
            }

            val nightSnaps = nightRefs.map { tx.get(it) }
            nightSnaps.forEachIndexed { i, snap ->
                val day = snap.toAvailabilityDay()
                val capacity = day.guestCapacity
                if (!snap.exists() || capacity == null || capacity - day.bookedGuests < guestCount) {
                    throw BookingException(
                        "${sorted[i].date} no longer has room for $guestCount guests. " +
                                "Availability has been refreshed."
                    )
                }
            }

            // ---- Writes ----
            nightRefs.forEach { ref ->
                tx.update(ref, "bookedGuests", FieldValue.increment(guestCount.toLong()))
            }

            val now = Timestamp.now()
            tx.set(
                reservationRef,
                mapOf(
                    "campsiteId" to campsite.id,
                    "lotteryId" to details.lottery.id,
                    "entrantId" to uid,
                    "createdAt" to now,
                    "checkInDate" to checkIn.toString(),   // ISO yyyy-MM-dd
                    "checkOutDate" to checkOut.toString(),
                    "guestCount" to guestCount.toLong(),
                    "status" to ReservationStatus.CONFIRMED.name,
                    "statusChangedAt" to now,
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
    drawCompletedAt = get("drawCompletedAt").toInstantOrNull()
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
    reservationId = getString("reservationId")
)

private fun DocumentSnapshot.toAvailabilityDay() = AvailabilityDay(
    campsiteId = getString("campsiteId").orEmpty(),
    date = get("date").toLocalDateOrNull(),
    guestCapacity = getLong("guestCapacity")?.toInt(),
    bookedGuests = getLong("bookedGuests")?.toInt() ?: 0
)