
package com.charge.reservation.repository

import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.BookingValidator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Creates reservations using a Firestore transaction.
 *
 * The invitation, campsite, lottery and nightly capacity are
 * checked before any reservation or availability is changed.
 */
class ReservationBookingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Attempts to create a confirmed reservation.
     *
     * Calls onSuccess only after Firestore commits the transaction.
     * Calls onFailure if any validation or database operation fails.
     */
    fun createReservation(
        lotteryId: String,
        campsiteId: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guestCount: Int,
        onSuccess: (Reservation) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val entrantId = auth.currentUser?.uid

        if (entrantId.isNullOrBlank()) {
            onFailure(IllegalStateException("You must sign in first."))
            return
        }

        if (lotteryId.isBlank() || campsiteId.isBlank()) {
            onFailure(
                IllegalArgumentException("Lottery and campsite are required.")
            )
            return
        }

        if (!checkOut.isAfter(checkIn) ||
            guestCount <= 0 ||
            java.time.temporal.ChronoUnit.DAYS.between(
                checkIn, checkOut
            ) > 366
        ) {
            onFailure(
                IllegalArgumentException("Invalid booking dates or guest count.")
            )
            return
        }

        val lotteryRef = firestore.collection("lotteries")
            .document(lotteryId)

        val campsiteRef = firestore.collection("campsites")
            .document(campsiteId)

        // Confirm this participation path with the team.
        val participationRef = lotteryRef
            .collection("participations")
            .document(entrantId)

        val reservationRef = firestore.collection("reservations")
            .document()

        val nights = mutableListOf<LocalDate>()
        var date = checkIn

        while (date.isBefore(checkOut)) {
            nights.add(date)
            date = date.plusDays(1)
        }

        val availabilityRefs = nights.map { night ->
            campsiteRef.collection("availability")
                .document(night.toString())
        }

        firestore.runTransaction { transaction ->

            // Read every document before performing writes.
            val lotteryDoc = transaction.get(lotteryRef)
            val campsiteDoc = transaction.get(campsiteRef)
            val participationDoc = transaction.get(participationRef)
            val reservationDoc = transaction.get(reservationRef)
            val availabilityDocs = availabilityRefs.map {
                transaction.get(it)
            }

            require(lotteryDoc.exists()) {
                "Lottery not found."
            }

            require(campsiteDoc.exists()) {
                "Campsite not found."
            }

            require(participationDoc.exists()) {
                "You do not have a booking invitation."
            }

            check(!reservationDoc.exists()) {
                "Reservation ID already exists."
            }

            val now = Instant.now()

            val lottery = Lottery(
                id = lotteryDoc.id,
                ownerId = lotteryDoc.getString("ownerId") ?: "",
                campsiteIds = (
                        lotteryDoc.get("campsiteIds") as? List<*>
                        )?.filterIsInstance<String>() ?: emptyList(),
                bookingDeadline = lotteryDoc
                    .getTimestamp("bookingDeadline")
                    ?.toDate()?.toInstant(),
                bookingStartDate = lotteryDoc
                    .getString("bookingStartDate")
                    ?.let(LocalDate::parse),
                bookingEndDate = lotteryDoc
                    .getString("bookingEndDate")
                    ?.let(LocalDate::parse),
                timeZone = lotteryDoc.getString("timeZone")
                    ?.let(ZoneId::of)
            )

            val campsite = Campsite(
                id = campsiteDoc.id,
                ownerId = campsiteDoc.getString("ownerId") ?: "",
                name = campsiteDoc.getString("name") ?: "",
                removed = campsiteDoc.getBoolean("removed") ?: true,
                maxOccupancy = campsiteDoc.getLong("maxOccupancy")
                    ?.let(Math::toIntExact),
                rules = campsiteDoc.getString("rules") ?: "",
                cancellationPolicy = campsiteDoc
                    .getString("cancellationPolicy") ?: ""
            )

            val participationStatus = participationDoc
                .getString("status")
                ?.let(ParticipationStatus::valueOf)
                ?: throw IllegalStateException(
                    "Invitation status is missing."
                )

            val participation = LotteryParticipation(
                lotteryId = lotteryId,
                entrantId = entrantId,
                status = participationStatus,
                reservationId = participationDoc
                    .getString("reservationId")
            )

            val availability = availabilityDocs.mapIndexed {
                    index, document ->

                require(document.exists()) {
                    "Availability is missing for ${nights[index]}."
                }

                AvailabilityDay(
                    campsiteId = campsiteId,
                    date = nights[index],
                    guestCapacity = document
                        .getLong("guestCapacity")
                        ?.let(Math::toIntExact),
                    bookedGuests = document
                        .getLong("bookedGuests")
                        ?.let(Math::toIntExact)
                        ?: throw IllegalStateException(
                            "Booked guest count is missing."
                        )
                )
            }

            val validationError = BookingValidator.validate(
                entrantId = entrantId,
                campsite = campsite,
                lottery = lottery,
                participation = participation,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = guestCount,
                availability = availability,
                now = now
            )

            require(validationError == null) {
                validationError ?: "Invalid reservation."
            }

            val timestamp = Timestamp(
                now.epochSecond,
                now.nano
            )

            // Only write after all validation succeeds.
            transaction.set(
                reservationRef,
                mapOf(
                    "id" to reservationRef.id,
                    "lotteryId" to lotteryId,
                    "campsiteId" to campsiteId,
                    "entrantId" to entrantId,
                    "checkInDate" to checkIn.toString(),
                    "checkOutDate" to checkOut.toString(),
                    "guestCount" to guestCount,
                    "status" to ReservationStatus.CONFIRMED.name,
                    "createdAt" to timestamp,
                    "statusChangedAt" to timestamp,
                    "statusChangeReason" to null
                )
            )

            availabilityRefs.forEachIndexed { index, ref ->
                val day = availability[index]

                transaction.update(
                    ref,
                    "bookedGuests",
                    day.bookedGuests + guestCount
                )
            }

            transaction.update(
                participationRef,
                mapOf(
                    "status" to ParticipationStatus.USED.name,
                    "reservationId" to reservationRef.id
                )
            )

            Reservation(
                id = reservationRef.id,
                campsiteId = campsiteId,
                lotteryId = lotteryId,
                entrantId = entrantId,
                createdAt = now,
                checkInDate = checkIn,
                checkOutDate = checkOut,
                guestCount = guestCount,
                status = ReservationStatus.CONFIRMED,
                statusChangedAt = now
            )
        }
            .addOnSuccessListener { reservation ->
                onSuccess(reservation)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
