package com.charge.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.PastReservationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests filtering of past reservations for US 01.06.02 (#17).
 */
class PastReservationFilterTest {

    private val today = LocalDate.of(2026, 11, 26)

    /**
     * Creates a reservation with configurable test values.
     */
    private fun reservation(
        id: String,
        entrantId: String = "entrant-1",
        status: ReservationStatus = ReservationStatus.CONFIRMED,
        checkInDate: LocalDate = today.minusDays(5),
        checkOutDate: LocalDate? = today.minusDays(2)
    ): Reservation {
        return Reservation(
            id = id,
            entrantId = entrantId,
            status = status,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate
        )
    }

    @Test
    fun completedReservationIsIncluded() {
        val result = PastReservationFilter.filter(
            listOf(reservation("res-1")),
            "entrant-1",
            today
        )

        assertEquals(1, result.size)
        assertEquals("res-1", result.first().id)
    }

    @Test
    fun upcomingAndOngoingReservationsAreExcluded() {
        val records = listOf(
            reservation(
                "res-2",
                checkInDate = today.plusDays(1),
                checkOutDate = today.plusDays(3)
            ),
            reservation(
                "res-3",
                checkOutDate = today.plusDays(1)
            )
        )

        val result = PastReservationFilter.filter(
            records, "entrant-1", today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun cancelledAndRejectedReservationsAreExcluded() {
        val records = listOf(
            reservation("res-4", status = ReservationStatus.CANCELLED),
            reservation("res-5", status = ReservationStatus.REJECTED)
        )

        val result = PastReservationFilter.filter(
            records, "entrant-1", today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun anotherEntrantsReservationIsExcluded() {
        val result = PastReservationFilter.filter(
            listOf(reservation("res-6", entrantId = "entrant-2")),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun missingCheckoutDateIsExcluded() {
        val result = PastReservationFilter.filter(
            listOf(reservation("res-7", checkOutDate = null)),
            "entrant-1",
            today
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun checkoutTodayIsIncluded() {
        val result = PastReservationFilter.filter(
            listOf(reservation("res-8", checkOutDate = today)),
            "entrant-1",
            today
        )

        assertEquals(1, result.size)
    }
}