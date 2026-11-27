package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.reservation.ReservationDetailsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests the reservation cancellation interface for #18.
 */
@RunWith(AndroidJUnit4::class)
class ReservationCancellationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = LocalDate.now().plusDays(5),
        checkOutDate = LocalDate.now().plusDays(7),
        guestCount = 2
    )

    @Test
    fun cancelButtonIsDisplayed() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                entrantId = "entrant-1",
                onCancelReservation = { _, _, _ -> },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun cancellationRequiresConfirmation() {
        var cancelCount = 0

        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                entrantId = "entrant-1",
                onCancelReservation = { _, _, _ ->
                    cancelCount++
                },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Cancel Reservation?")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Keep Reservation")
            .performClick()

        composeRule.onNodeWithText("Cancel Reservation?")
            .assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(0, cancelCount)
        }
    }

    @Test
    fun successfulCancellationShowsMessage() {
        var cancelledId = ""

        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                entrantId = "entrant-1",
                onCancelReservation = { id, onSuccess, _ ->
                    cancelledId = id
                    onSuccess()
                },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Confirm Cancellation")
            .performClick()

        composeRule.onNodeWithText(
            "Reservation cancelled successfully."
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Cancel Reservation")
            .assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals("res-1", cancelledId)
        }
    }

    @Test
    fun failedCancellationShowsError() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                entrantId = "entrant-1",
                onCancelReservation = { _, _, onFailure ->
                    onFailure(Exception("Test failure"))
                },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Confirm Cancellation")
            .performClick()

        composeRule.onNodeWithText(
            "Unable to cancel reservation."
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Cancel Reservation")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun otherEntrantCannotCancelReservation() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                entrantId = "entrant-2",
                onCancelReservation = { _, _, _ -> },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .assertDoesNotExist()
    }

    @Test
    fun cancelledReservationCannotBeCancelledAgain() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation.copy(
                    status = ReservationStatus.CANCELLED
                ),
                campsiteName = "Pine Lake",
                entrantId = "entrant-1",
                onCancelReservation = { _, _, _ -> },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Cancel Reservation")
            .assertDoesNotExist()
    }
}