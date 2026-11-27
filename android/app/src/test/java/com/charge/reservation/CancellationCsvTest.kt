package com.charge.reservation

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.ReservationCsv
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Tests CSV generation for cancellation confirmations.
 */
class CancellationCsvTest {

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = LocalDate.of(2026, 12, 10),
        checkOutDate = LocalDate.of(2026, 12, 12),
        status = ReservationStatus.CANCELLED,
        statusChangedAt = Instant.parse("2026-11-27T06:00:00Z")
    )

    @Test
    fun csvContainsCancellationDetails() {
        val csv = ReservationCsv.generateCancellation(
            reservation, "Pine Lake"
        )

        val expected =
            "Reservation ID,Campsite,Check-in,Check-out,Status,Cancellation Time\r\n" +
                    "\"res-1\",\"Pine Lake\",\"2026-12-10\",\"2026-12-12\"," +
                    "\"CANCELLED\",\"2026-11-27T06:00:00Z\"\r\n"

        assertEquals(expected, csv)
    }

    @Test(expected = IllegalArgumentException::class)
    fun confirmedReservationCannotGenerateConfirmation() {
        ReservationCsv.generateCancellation(
            reservation.copy(
                status = ReservationStatus.CONFIRMED
            ),
            "Pine Lake"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectedReservationCannotGenerateConfirmation() {
        ReservationCsv.generateCancellation(
            reservation.copy(
                status = ReservationStatus.REJECTED
            ),
            "Pine Lake"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingCancellationTimeIsRejected() {
        ReservationCsv.generateCancellation(
            reservation.copy(statusChangedAt = null),
            "Pine Lake"
        )
    }

    @Test
    fun campsiteNameWithSpecialCharactersIsEscaped() {
        val csv = ReservationCsv.generateCancellation(
            reservation,
            "Pine Lake, \"North\""
        )

        assertTrue(
            csv.contains("\"Pine Lake, \"\"North\"\"\"")
        )
    }
}