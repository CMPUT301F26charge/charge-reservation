package com.charge.reservation.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus

/**
 * Generates CSV records for reservations and cancellation confirmations.
 */
object ReservationCsv {

    /**
     * Converts a reservation into a CSV containing its saved details.
     */
    fun generate(
        reservation: Reservation,
        campsiteName: String
    ): String {
        val headings = "Reservation ID,Campsite,Check-in,Check-out,Status"

        val values = listOf(
            reservation.id,
            campsiteName,
            reservation.checkInDate?.toString() ?: "",
            reservation.checkOutDate?.toString() ?: "",
            reservation.status.name
        )

        return headings + "\r\n" +
                values.joinToString(",") { escape(it) } + "\r\n"
    }

    /**
     * Generates a cancellation confirmation for a cancelled reservation.
     */
    fun generateCancellation(
        reservation: Reservation,
        campsiteName: String
    ): String {
        require(reservation.status == ReservationStatus.CANCELLED) {
            "Reservation has not been cancelled."
        }

        val cancellationTime = requireNotNull(
            reservation.statusChangedAt
        ) {
            "Cancellation time is missing."
        }

        val headings =
            "Reservation ID,Campsite,Check-in,Check-out,Status,Cancellation Time"

        val values = listOf(
            reservation.id,
            campsiteName,
            reservation.checkInDate?.toString() ?: "",
            reservation.checkOutDate?.toString() ?: "",
            reservation.status.name,
            cancellationTime.toString()
        )

        return headings + "\r\n" +
                values.joinToString(",") { escape(it) } + "\r\n"
    }

    /**
     * Handles commas, quotation marks and line breaks in CSV values.
     */
    private fun escape(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}