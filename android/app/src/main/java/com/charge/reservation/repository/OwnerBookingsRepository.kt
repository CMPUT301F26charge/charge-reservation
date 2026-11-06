
package com.charge.reservation.repository

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

/**
 * Provides live reservation updates for a campsite.
 */
class OwnerBookingsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun listenToBookings(
        campsiteId: String,
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        require(campsiteId.isNotBlank()) {
            "Campsite ID cannot be blank."
        }

        return db.collection("reservations")
            .whereEqualTo("campsiteId", campsiteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                try {
                    val bookings = snapshot?.documents?.map { doc ->
                        val status = ReservationStatus.valueOf(
                            doc.getString("status") ?: "CONFIRMED"
                        )

                        Reservation(
                            id = doc.id,
                            campsiteId = doc.getString("campsiteId") ?: "",
                            lotteryId = doc.getString("lotteryId") ?: "",
                            entrantId = doc.getString("entrantId") ?: "",
                            checkInDate = OwnerReservationParser.parseDate(
                                doc.get("checkInDate")
                            ),
                            checkOutDate = OwnerReservationParser.parseDate(
                                doc.get("checkOutDate")
                            ),
                            guestCount = doc.getLong("guestCount")?.toInt(),
                            status = status,
                            createdAt = OwnerReservationParser.parseInstant(
                                doc.get("createdAt")
                            ),
                            statusChangedAt = OwnerReservationParser.parseInstant(
                                doc.get("statusChangedAt")
                            ),
                            statusChangeReason =
                                doc.getString("statusChangeReason")
                        )
                    } ?: emptyList()

                    onSuccess(
                        bookings.sortedBy { it.checkInDate }
                    )
                } catch (exception: Exception) {
                    onFailure(exception)
                }
            }
    }
}

/**
 * Converts saved Firestore reservation dates and timestamps.
 */
private object OwnerReservationParser {

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

