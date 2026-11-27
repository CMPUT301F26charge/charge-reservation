package com.charge.reservation.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.time.LocalDate

/**
 * Validates reservation cancellation and determines
 * which nights must have their availability restored.
 */
object ReservationCancellation {

    /**
     * Checks whether an entrant can cancel an upcoming reservation.
     */
    fun canCancel(
        reservation: Reservation,
        entrantId: String,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        return entrantId.isNotBlank() &&
                reservation.entrantId == entrantId &&
                reservation.status == ReservationStatus.CONFIRMED &&
                reservation.checkInDate != null &&
                reservation.checkInDate.isAfter(today) &&
                reservation.checkOutDate != null &&
                reservation.checkOutDate.isAfter(reservation.checkInDate) &&
                (reservation.guestCount ?: 0) > 0
    }

    /**
     * Returns all reserved nights, excluding the checkout date.
     */
    fun bookedNights(
        checkIn: LocalDate,
        checkOut: LocalDate
    ): List<LocalDate> {
        require(checkOut.isAfter(checkIn))

        val nights = mutableListOf<LocalDate>()
        var date = checkIn

        while (date.isBefore(checkOut)) {
            nights.add(date)
            date = date.plusDays(1)
        }

        return nights
    }
}