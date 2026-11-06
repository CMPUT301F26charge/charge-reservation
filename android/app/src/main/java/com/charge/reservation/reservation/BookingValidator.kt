package com.charge.reservation.reservation

import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import java.time.Instant
import java.time.LocalDate

/**
 * Validates booking eligibility, dates, and campsite capacity.
 *
 * Firestore must repeat these checks inside a transaction
 * before creating a reservation.
 */
object BookingValidator {

    /**
     * Returns null for a valid request or an error message otherwise.
     */
    fun validate(
        entrantId: String,
        campsite: Campsite,
        lottery: Lottery,
        participation: LotteryParticipation,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guestCount: Int,
        availability: List<AvailabilityDay>,
        now: Instant = Instant.now()
    ): String? {

        if (entrantId.isBlank()) {
            return "You must be signed in to make a reservation."
        }

        if (campsite.id.isBlank() || campsite.removed) {
            return "This campsite is unavailable."
        }

        if (lottery.id.isBlank() ||
            campsite.id !in lottery.campsiteIds
        ) {
            return "This campsite is not part of the selected lottery."
        }

        if (participation.entrantId != entrantId ||
            participation.lotteryId != lottery.id
        ) {
            return "This invitation does not belong to you."
        }

        if (participation.status != ParticipationStatus.INVITED ||
            participation.reservationId != null
        ) {
            return "You do not have an unused booking invitation."
        }

        if (lottery.bookingDeadline == null ||
            !now.isBefore(lottery.bookingDeadline)
        ) {
            return "The booking deadline has passed."
        }

        if (!checkOut.isAfter(checkIn)) {
            return "Checkout must be after check-in."
        }

        if (lottery.bookingStartDate == null ||
            lottery.bookingEndDate == null ||
            checkIn.isBefore(lottery.bookingStartDate) ||
            checkOut.isAfter(lottery.bookingEndDate)
        ) {
            return "The requested dates are outside the lottery booking period."
        }

        if (guestCount <= 0) {
            return "Guest count must be greater than zero."
        }

        val maxOccupancy = campsite.maxOccupancy

        if (maxOccupancy != null && guestCount > maxOccupancy) {
            return "Guest count exceeds the campsite capacity."
        }

        // Checkout day is not an occupied night.
        var night = checkIn

        while (night.isBefore(checkOut)) {
            val day = availability.find {
                it.campsiteId == campsite.id && it.date == night
            } ?: return "Availability is missing for $night."

            val capacity = day.guestCapacity
                ?: return "Capacity is missing for $night."

            if (day.bookedGuests < 0 ||
                capacity < 0 ||
                capacity - day.bookedGuests < guestCount
            ) {
                return "Not enough space is available for $night."
            }

            night = night.plusDays(1)
        }

        return null
    }
}
