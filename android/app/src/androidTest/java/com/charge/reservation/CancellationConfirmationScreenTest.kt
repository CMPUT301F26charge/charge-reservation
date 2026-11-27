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
import com.charge.reservation.reservation.PastReservationsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/**
 * Tests cancellation confirmation and cancelled reservation history.
 */
@RunWith(AndroidJUnit4::class)
class CancellationConfirmationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = LocalDate.now().plusDays(5),
        checkOutDate = LocalDate.now().plusDays(7),
        guestCount = 2,
        status = ReservationStatus.CANCELLED,
        statusChangedAt = Instant.parse("2026-11-27T06:00:00Z")
    )

    @Test
    fun cancelledReservationOffersConfirmationDownload() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                allowCsvExport = true,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText(
            "Download Cancellation Confirmation"
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun confirmedReservationDoesNotOfferConfirmation() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation.copy(
                    status = ReservationStatus.CONFIRMED
                ),
                campsiteName = "Pine Lake",
                allowCsvExport = true,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText(
            "Download Cancellation Confirmation"
        ).assertDoesNotExist()
    }

    @Test
    fun missingCancellationTimePreventsDownload() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation.copy(
                    statusChangedAt = null
                ),
                campsiteName = "Pine Lake",
                allowCsvExport = true,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText(
            "Download Cancellation Confirmation"
        ).assertDoesNotExist()
    }

    @Test
    fun cancelledReservationCanBeOpened() {
        var selectedId = ""

        composeRule.setContent {
            PastReservationsScreen(
                entrantId = "entrant-1",
                reservations = listOf(reservation),
                campsiteNames = mapOf("site-1" to "Pine Lake"),
                showCancelled = true,
                onReservationClick = {
                    selectedId = it
                },
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("My Cancelled Reservations")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Pine Lake")
            .performClick()

        composeRule.runOnIdle {
            assertEquals("res-1", selectedId)
        }
    }

    @Test
    fun otherEntrantsCancelledReservationsAreExcluded() {
        composeRule.setContent {
            PastReservationsScreen(
                entrantId = "entrant-2",
                reservations = listOf(reservation),
                campsiteNames = mapOf("site-1" to "Pine Lake"),
                showCancelled = true,
                onReservationClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText(
            "You have no cancelled reservations."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Pine Lake")
            .assertDoesNotExist()
    }
}