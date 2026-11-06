package com.charge.reservation

import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.reservation.BookingValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BookingValidatorTest {

    private val now = Instant.parse("2026-11-06T12:00:00Z")
    private val checkIn = LocalDate.of(2026, 12, 10)
    private val checkOut = LocalDate.of(2026, 12, 12)

    private val campsite = Campsite(
        id = "camp-1",
        ownerId = "owner-1",
        name = "Test Campsite",
        maxOccupancy = 5
    )

    private val lottery = Lottery(
        id = "lottery-1",
        ownerId = "owner-1",
        campsiteIds = listOf("camp-1"),
        bookingDeadline = now.plusSeconds(86400),
        bookingStartDate = checkIn,
        bookingEndDate = checkOut
    )

    private val invitation = LotteryParticipation(
        lotteryId = "lottery-1",
        entrantId = "entrant-1",
        status = ParticipationStatus.INVITED
    )

    private val availability = listOf(
        AvailabilityDay("camp-1", checkIn, 5, 0),
        AvailabilityDay("camp-1", checkIn.plusDays(1), 5, 0)
    )

    private fun validate(
        campsiteValue: Campsite = campsite,
        lotteryValue: Lottery = lottery,
        invitationValue: LotteryParticipation = invitation,
        checkInValue: LocalDate = checkIn,
        checkOutValue: LocalDate = checkOut,
        guests: Int = 2,
        days: List<AvailabilityDay> = availability,
        currentTime: Instant = now
    ): String? {
        return BookingValidator.validate(
            entrantId = "entrant-1",
            campsite = campsiteValue,
            lottery = lotteryValue,
            participation = invitationValue,
            checkIn = checkInValue,
            checkOut = checkOutValue,
            guestCount = guests,
            availability = days,
            now = currentTime
        )
    }

    @Test
    fun validBookingIsAccepted() {
        assertNull(validate())
    }

    @Test
    fun invitationMustBelongToEntrant() {
        assertEquals(
            "This invitation does not belong to you.",
            validate(
                invitationValue = invitation.copy(entrantId = "someone-else")
            )
        )
    }

    @Test
    fun usedInvitationIsRejected() {
        assertEquals(
            "You do not have an unused booking invitation.",
            validate(
                invitationValue = invitation.copy(
                    status = ParticipationStatus.USED,
                    reservationId = "existing-reservation"
                )
            )
        )
    }

    @Test
    fun expiredInvitationIsRejected() {
        assertEquals(
            "The booking deadline has passed.",
            validate(currentTime = now.plusSeconds(86401))
        )
    }

    @Test
    fun checkoutMustFollowCheckIn() {
        assertEquals(
            "Checkout must be after check-in.",
            validate(checkOutValue = checkIn)
        )
    }

    @Test
    fun guestCountCannotExceedOccupancy() {
        assertEquals(
            "Guest count exceeds the campsite capacity.",
            validate(guests = 6)
        )
    }

    @Test
    fun overbookingIsRejected() {
        val nearlyFull = availability.map {
            it.copy(bookedGuests = 4)
        }

        assertEquals(
            "Not enough space is available for $checkIn.",
            validate(guests = 2, days = nearlyFull)
        )
    }

    @Test
    fun missingAvailabilityIsRejected() {
        assertEquals(
            "Availability is missing for ${checkIn.plusDays(1)}.",
            validate(days = availability.take(1))
        )
    }

    @Test
    fun removedCampsiteIsRejected() {
        assertEquals(
            "This campsite is unavailable.",
            validate(campsiteValue = campsite.copy(removed = true))
        )
    }
}
