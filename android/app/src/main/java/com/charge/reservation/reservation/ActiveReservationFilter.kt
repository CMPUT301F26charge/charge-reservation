package com.charge.reservation.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.time.LocalDate

/**
 * Filters reservations for the entrant's active reservation list.
 *
 * Only confirmed reservations belonging to the current entrant
 * with checkout dates in the future are included.
 */
object ActiveReservationFilter {

    /**
     * Returns active reservations belonging to [entrantId].
     *
     * @param reservations all reservation records
     * @param entrantId the current entrant's profile ID
     * @param today the current date, injectable for testing
     * @return confirmed reservations that have not finished
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
                    it.checkOutDate.isAfter(today)
        }
    }
}
