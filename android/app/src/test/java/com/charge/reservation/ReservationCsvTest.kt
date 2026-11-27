package com.charge.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.ReservationCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests reservation CSV generation for US 01.05.02 (#15).
 */
class ReservationCsvTest {

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = LocalDate.of(2026, 11, 10),
        checkOutDate = LocalDate.of(2026, 11, 12)
    )

    @Test
    fun csvContainsReservationDetails() {
        val result = ReservationCsv.generate(
            reservation, "Pine Lake"
        )

        val expected =
            "Reservation ID,Campsite,Check-in,Check-out,Status\r\n" +
                    "\"res-1\",\"Pine Lake\",\"2026-11-10\"," +
                    "\"2026-11-12\",\"CONFIRMED\"\r\n"

        assertEquals(expected, result)
    }

    @Test
    fun csvContainsCurrentStatus() {
        val cancelled = reservation.copy(
            status = ReservationStatus.CANCELLED
        )

        val result = ReservationCsv.generate(
            cancelled, "Pine Lake"
        )

        assertTrue(result.contains("\"CANCELLED\""))
    }

    @Test
    fun campsiteNameWithSpecialCharactersIsEscaped() {
        val result = ReservationCsv.generate(
            reservation, "Pine Lake, \"Alberta\""
        )

        assertTrue(
            result.contains("\"Pine Lake, \"\"Alberta\"\"\"")
        )
    }

    @Test
    fun missingDatesAreHandled() {
        val result = ReservationCsv.generate(
            reservation.copy(
                checkInDate = null,
                checkOutDate = null
            ),
            "Pine Lake"
        )

        assertTrue(result.contains("\"Pine Lake\",\"\",\"\""))
    }
}