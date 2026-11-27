package com.charge.reservation.reservation

import com.charge.reservation.model.Reservation

/**
 * Generates CSV records for campsite reservations.
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
     * Handles commas, quotation marks and line breaks in CSV values.
     */
    private fun escape(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}