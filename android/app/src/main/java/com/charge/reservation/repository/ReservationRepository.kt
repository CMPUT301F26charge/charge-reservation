
package com.charge.reservation.repository

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Reads reservation records belonging to the signed-in entrant.
 */
class ReservationRepository {

    private val firestore = FirebaseFirestore.getInstance()

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

        return firestore.collection("reservations")
            .whereEqualTo("entrantId", entrantId)
            .addSnapshotListener { snapshot, error ->

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
