package com.charge.reservation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.BookingResult
import com.charge.reservation.reservation.ReservationConfirmationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI tests for reservation confirmation outcomes.
 */
@RunWith(AndroidJUnit4::class)
class ReservationConfirmationScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successfulBookingDisplaysConfirmation() {
        val reservation = Reservation(
            id = "RES-001",
            campsiteId = "camp-001",
            entrantId = "entrant-001",
            checkInDate = LocalDate.of(2026, 11, 10),
            checkOutDate = LocalDate.of(2026, 11, 12),
            status = ReservationStatus.CONFIRMED
        )

        rule.setContent {
            ReservationConfirmationScreen(
                result = BookingResult.Success(
                    reservation = reservation,
                    campsiteName = "Pine Lake Campground"
                ),
                onDoneClick = {}
            )
        }

        rule.onNodeWithText(
            "Reservation Confirmed!"
        ).assertExists()

        rule.onNodeWithText(
            "Reservation ID: RES-001"
        ).assertExists()

        rule.onNodeWithText(
            "Campsite: Pine Lake Campground"
        ).assertExists()

        rule.onNodeWithText(
            "Check-in: 2026-11-10"
        ).assertExists()

        rule.onNodeWithText(
            "Check-out: 2026-11-12"
        ).assertExists()
    }

    @Test
    fun failedBookingDoesNotDisplaySuccess() {
        rule.setContent {
            ReservationConfirmationScreen(
                result = BookingResult.Failure(
                    "Selected dates are unavailable."
                ),
                onDoneClick = {}
            )
        }

        rule.onNodeWithText(
            "Reservation Failed"
        ).assertExists()

        rule.onNodeWithText(
            "Selected dates are unavailable."
        ).assertExists()

        rule.onNodeWithText(
            "Reservation Confirmed!"
        ).assertDoesNotExist()
    }

    @Test
    fun cancelledReservationDoesNotDisplaySuccess() {
        val reservation = Reservation(
            id = "RES-002",
            status = ReservationStatus.CANCELLED
        )

        rule.setContent {
            ReservationConfirmationScreen(
                result = BookingResult.Success(
                    reservation,
                    "Pine Lake Campground"
                ),
                onDoneClick = {}
            )
        }

        rule.onNodeWithText(
            "Reservation Confirmed!"
        ).assertDoesNotExist()

        rule.onNodeWithText(
            "Reservation could not be confirmed."
        ).assertExists()
    }
}
