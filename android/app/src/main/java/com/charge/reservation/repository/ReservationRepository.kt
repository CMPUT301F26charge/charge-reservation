
package com.charge.reservation.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.charge.reservation.reservation.ReservationCancellation
import java.time.ZoneId

/**
 * Reads reservation records and handles entrant cancellations
 * while maintaining campsite availability.
 */
class ReservationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Cancels an entrant's upcoming reservation and restores
     * the booked guest capacity for each reserved night.
     *
     * All changes are performed in one Firestore transaction.
     *
     * @param reservationId the reservation to cancel
     * @param onSuccess called after successful cancellation
     * @param onFailure receives validation or Firestore errors
     */
    fun cancelReservation(
        reservationId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val entrantId = FirebaseAuth.getInstance().currentUser?.uid

        if (entrantId == null) {
            onFailure(IllegalStateException("Please sign in first."))
            return
        }

        val reservationRef = firestore.collection("reservations")
            .document(reservationId)

        firestore.runTransaction { transaction ->

            val document = transaction.get(reservationRef)

            if (!document.exists()) {
                throw IllegalStateException("Reservation not found.")
            }

            val reservation = Reservation(
                id = document.id,
                campsiteId = document.getString("campsiteId") ?: "",
                lotteryId = document.getString("lotteryId") ?: "",
                entrantId = document.getString("entrantId") ?: "",
                checkInDate = ReservationValueParser.parseDate(
                    document.get("checkInDate")
                ),
                checkOutDate = ReservationValueParser.parseDate(
                    document.get("checkOutDate")
                ),
                guestCount = document.getLong("guestCount")?.let {
                    Math.toIntExact(it)
                },
                status = ReservationStatus.valueOf(
                    requireNotNull(document.getString("status"))
                )
            )

            // Use the lottery's timezone, as in the booking process.
            val lotteryRef = firestore.collection("lotteries")
                .document(reservation.lotteryId)

            val lottery = transaction.get(lotteryRef)

            val zone = lottery.getString("timeZone")?.let {
                ZoneId.of(it)
            } ?: ZoneId.systemDefault()

            if (!ReservationCancellation.canCancel(
                    reservation, entrantId, LocalDate.now(zone)
                )
            ) {
                throw IllegalStateException(
                    "This reservation cannot be cancelled."
                )
            }

            val nights = ReservationCancellation.bookedNights(
                requireNotNull(reservation.checkInDate),
                requireNotNull(reservation.checkOutDate)
            )

            val guestCount = requireNotNull(reservation.guestCount)

            val availability = firestore.collection("campsites")
                .document(reservation.campsiteId)
                .collection("availability")

            val nightRefs = nights.map { date ->
                availability.document(date.toString())
            }

            // Validate every night before performing any writes.
            nightRefs.forEachIndexed { index, reference ->

                val day = transaction.get(reference)
                val booked = day.getLong("bookedGuests")
                val capacity = day.getLong("guestCapacity")

                if (!day.exists() ||
                    day.getString("campsiteId") != reservation.campsiteId ||
                    ReservationValueParser.parseDate(day.get("date")) != nights[index] ||
                    booked == null ||
                    capacity == null ||
                    booked < guestCount ||
                    booked > capacity
                ) {
                    throw IllegalStateException(
                        "Unable to restore campsite availability."
                    )
                }
            }

            // Restore the guest capacity for each reserved night.
            nightRefs.forEach { reference ->
                transaction.update(
                    reference,
                    "bookedGuests",
                    FieldValue.increment(-guestCount.toLong())
                )
            }

            transaction.update(
                reservationRef,
                mapOf(
                    "status" to ReservationStatus.CANCELLED.name,
                    "statusChangedAt" to FieldValue.serverTimestamp(),
                    "statusChangeReason" to "Cancelled by entrant"
                )
            )

            Unit

        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }

    /**
     * Listens for Firestore reservations belonging to [entrantId].
     *
     * @param entrantId the authenticated user's Firebase UID
     * @param onSuccess receives updated reservation records
     * @param onFailure receives database or conversion errors
     * @return listener registration to remove when finished
     */
    fun listenToEntrantReservations(
        entrantId: String,
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {

        require(entrantId.isNotBlank()) {
            "Entrant ID cannot be blank."
        }

        val query = firestore.collection("reservations")
            .whereEqualTo("entrantId", entrantId)

        return listenToReservations(query, onSuccess, onFailure)
    }

    fun listenToConfirmedCampsiteReservations(
        campsiteId: String,
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        require(campsiteId.isNotBlank()) {
            "Campsite ID cannot be blank."
        }

        val query = firestore.collection("reservations")
            .whereEqualTo("campsiteId", campsiteId)
            .whereEqualTo("status", ReservationStatus.CONFIRMED.name)

        return listenToReservations(query, onSuccess, onFailure)
    }

    private fun listenToReservations(
        query: Query,
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return query.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                try {
                    val reservations = snapshot?.documents?.map { document ->

                        val statusValue = document.getString("status")
                            ?: throw IllegalStateException(
                                "Missing status for reservation ${document.id}"
                            )

                        Reservation(
                            id = document.id,
                            campsiteId = document.getString("campsiteId") ?: "",
                            lotteryId = document.getString("lotteryId") ?: "",
                            entrantId = document.getString("entrantId") ?: "",
                            checkInDate = ReservationValueParser.parseDate(
                                document.get("checkInDate")
                            ),
                            checkOutDate = ReservationValueParser.parseDate(
                                document.get("checkOutDate")
                            ),
                            guestCount = document.getLong("guestCount")?.toInt(),
                            status = ReservationStatus.valueOf(statusValue),
                            createdAt = ReservationValueParser.parseInstant(
                                document.get("createdAt")
                            ),
                            statusChangedAt = ReservationValueParser.parseInstant(
                                document.get("statusChangedAt")
                            ),
                            statusChangeReason = document.getString(
                                "statusChangeReason"
                            )
                        )
                    } ?: emptyList()

                    onSuccess(reservations)

                } catch (exception: Exception) {
                    onFailure(exception)
                }
            }
    }
}

/**
 * Converts Firestore reservation date and timestamp values.
 *
 * Supports ISO strings and Firestore timestamps.
 * Timestamp dates are interpreted in UTC.
 */
internal object ReservationValueParser {

    /**
     * Converts an ISO date string or Firestore timestamp to LocalDate.
     */
    fun parseDate(value: Any?): LocalDate? {
        return when (value) {
            null -> null

            is String -> LocalDate.parse(value)

            is Timestamp -> value.toDate()
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()

            else -> throw IllegalArgumentException(
                "Unsupported reservation date format."
            )
        }
    }

    /**
     * Converts a Firestore timestamp or ISO instant string.
     */
    fun parseInstant(value: Any?): Instant? {
        return when (value) {
            null -> null

            is Timestamp -> value.toDate().toInstant()

            is String -> Instant.parse(value)

            else -> throw IllegalArgumentException(
                "Unsupported reservation timestamp format."
            )
        }
    }
}
