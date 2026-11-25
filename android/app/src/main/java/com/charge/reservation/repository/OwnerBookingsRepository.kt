
package com.charge.reservation.repository

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

import android.util.Log
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentSnapshot

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

        val query = db.collection("reservations")
            .whereEqualTo("campsiteId", campsiteId)

        fun processDocuments(documents: List<DocumentSnapshot>) {
            try {
                val bookings = documents.map { doc ->
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
                        guestCount = when (val raw = doc.get("guestCount")) {
                            is Number -> raw.toInt()
                            is String -> raw.toIntOrNull()
                            else -> null
                        },
                        status = ReservationStatus.valueOf(
                            (doc.getString("status") ?: "CONFIRMED").uppercase()
                        ),
                        createdAt = OwnerReservationParser.parseInstant(
                            doc.get("createdAt")
                        ),
                        statusChangedAt = OwnerReservationParser.parseInstant(
                            doc.get("statusChangedAt")
                        ),
                        statusChangeReason =
                            doc.getString("statusChangeReason")
                    )
                }

                Log.d(
                    "OwnerBookings",
                    "Campsite $campsiteId: ${bookings.size} reservations"
                )

                onSuccess(bookings.sortedBy { it.checkInDate })

            } catch (exception: Exception) {
                Log.e("OwnerBookings", "Reservation parsing failed", exception)
                onFailure(exception)
            }
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error)
            } else if (snapshot != null && !snapshot.metadata.isFromCache) {
                processDocuments(snapshot.documents)
            }
        }

        query.get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                processDocuments(snapshot.documents)
            }
            .addOnFailureListener { error ->
                Log.e("OwnerBookings", "Server fetch failed", error)
                onFailure(error)
            }

        return listener
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

