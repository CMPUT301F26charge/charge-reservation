package com.charge.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.ReservationCancellation
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests cancellation eligibility and reserved night calculation.
 */
class ReservationCancellationTest {

    private val today = LocalDate.of(2026, 11, 26)

    private val reservation = Reservation(
        id = "res-1",
        entrantId = "entrant-1",
        campsiteId = "site-1",
        checkInDate = LocalDate.of(2026, 12, 10),
        checkOutDate = LocalDate.of(2026, 12, 12),
        guestCount = 3,
        status = ReservationStatus.CONFIRMED
    )

    @Test
    fun ownerCanCancelUpcomingReservation() {
        assertTrue(
            ReservationCancellation.canCancel(
                reservation, "entrant-1", today
            )
        )
    }

    @Test
    fun otherEntrantCannotCancelReservation() {
        assertFalse(
            ReservationCancellation.canCancel(
                reservation, "entrant-2", today
            )
        )
    }

    @Test
    fun cancelledReservationCannotBeCancelledAgain() {
        val cancelled = reservation.copy(
            status = ReservationStatus.CANCELLED
        )

        assertFalse(
            ReservationCancellation.canCancel(
                cancelled, "entrant-1", today
            )
        )
    }

    @Test
    fun rejectedReservationCannotBeCancelled() {
        val rejected = reservation.copy(
            status = ReservationStatus.REJECTED
        )

        assertFalse(
            ReservationCancellation.canCancel(
                rejected, "entrant-1", today
            )
        )
    }

    @Test
    fun pastReservationCannotBeCancelled() {
        val past = reservation.copy(
            checkInDate = today.minusDays(5),
            checkOutDate = today.minusDays(3)
        )

        assertFalse(
            ReservationCancellation.canCancel(
                past, "entrant-1", today
            )
        )
    }

    @Test
    fun reservationStartingTodayCannotBeCancelled() {
        val current = reservation.copy(
            checkInDate = today
        )

        assertFalse(
            ReservationCancellation.canCancel(
                current, "entrant-1", today
            )
        )
    }

    @Test
    fun invalidGuestCountPreventsCancellation() {
        val invalid = reservation.copy(guestCount = 0)

        assertFalse(
            ReservationCancellation.canCancel(
                invalid, "entrant-1", today
            )
        )
    }

    @Test
    fun missingDatesPreventCancellation() {
        val invalid = reservation.copy(checkInDate = null)

        assertFalse(
            ReservationCancellation.canCancel(
                invalid, "entrant-1", today
            )
        )
    }

    @Test
    fun bookedNightsExcludeCheckoutDate() {
        val nights = ReservationCancellation.bookedNights(
            LocalDate.of(2026, 12, 10),
            LocalDate.of(2026, 12, 12)
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 11)
            ),
            nights
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDateRangeIsRejected() {
        ReservationCancellation.bookedNights(today, today)
    }
}