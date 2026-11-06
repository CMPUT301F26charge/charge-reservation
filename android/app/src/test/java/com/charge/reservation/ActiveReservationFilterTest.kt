package com.charge.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.ActiveReservationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests filtering of an entrant's active reservations.
 */
class ActiveReservationFilterTest {

    private val today = LocalDate.of(2026, 11, 5)

    /**
     * Creates a reservation with configurable test values.
     */
    private fun reservation(
        id: String,
        entrantId: String = "entrant-1",
        status: ReservationStatus = ReservationStatus.CONFIRMED,
        checkOutDate: LocalDate? = today.plusDays(3)
    ): Reservation {
        return Reservation(
            id = id,
            entrantId = entrantId,
            status = status,
            checkInDate = today,
            checkOutDate = checkOutDate
        )
    }

    @Test
    fun upcomingConfirmedReservationIsIncluded() {
        val result = ActiveReservationFilter.filter(
            listOf(reservation("res-1")),
            "entrant-1",
            today
        )

        assertEquals(1, result.size)
        assertEquals("res-1", result.first().id)
    }

    @Test
    fun ongoingReservationIsIncluded() {
        val ongoing = reservation(
            id = "res-2",
            checkOutDate = today.plusDays(1)
        ).copy(checkInDate = today.minusDays(2))

        val result = ActiveReservationFilter.filter(
            listOf(ongoing),
            "entrant-1",
            today
        )

        assertEquals(1, result.size)
    }

    @Test
    fun completedReservationIsExcluded() {
        val result = ActiveReservationFilter.filter(
            listOf(reservation("res-3", checkOutDate = today.minusDays(1))),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun cancelledAndRejectedReservationsAreExcluded() {
        val records = listOf(
            reservation("res-4", status = ReservationStatus.CANCELLED),
            reservation("res-5", status = ReservationStatus.REJECTED)
        )

        val result = ActiveReservationFilter.filter(records, "entrant-1", today)

        assertTrue(result.isEmpty())
    }

    @Test
    fun anotherEntrantsReservationIsExcluded() {
        val result = ActiveReservationFilter.filter(
            listOf(reservation("res-6", entrantId = "entrant-2")),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun missingCheckoutDateIsExcluded() {
        val result = ActiveReservationFilter.filter(
            listOf(reservation("res-7", checkOutDate = null)),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun checkoutTodayIsExcluded() {
        val result = ActiveReservationFilter.filter(
            listOf(reservation("res-8", checkOutDate = today)),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }
}
