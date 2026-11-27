package com.charge.reservation.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.time.LocalDate

/**
 * Filters completed reservations belonging to an entrant.
 */
object PastReservationFilter {

    /**
     * Returns confirmed reservations that have reached their checkout date.
     * Cancelled and rejected reservations are excluded.
     */
    fun filter(
        reservations: List<Reservation>,
        entrantId: String,
        today: LocalDate = LocalDate.now()
    ): List<Reservation> {
        return reservations.filter {
            it.entrantId == entrantId &&
                    it.status == ReservationStatus.CONFIRMED &&
                    it.checkOutDate != null &&
                    !it.checkOutDate.isAfter(today)
        }
    }
}