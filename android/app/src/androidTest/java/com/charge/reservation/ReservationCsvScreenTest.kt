package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Reservation
import com.charge.reservation.reservation.ReservationDetailsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests reservation CSV export UI for US 01.05.02 (#15).
 */
@RunWith(AndroidJUnit4::class)
class ReservationCsvScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = LocalDate.of(2026, 11, 10),
        checkOutDate = LocalDate.of(2026, 11, 12),
        guestCount = 4
    )

    /** Checks that the download button is available when export is allowed. */
    @Test
    fun downloadButtonIsDisplayed() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                allowCsvExport = true,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Download CSV")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /** Checks that the download button is hidden when export is disabled. */
    @Test
    fun downloadButtonIsHiddenWhenDisabled() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = reservation,
                campsiteName = "Pine Lake",
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Download CSV")
            .assertDoesNotExist()
    }

    /** Checks that a missing reservation cannot be exported. */
    @Test
    fun missingReservationCannotBeExported() {
        composeRule.setContent {
            ReservationDetailsScreen(
                reservation = null,
                campsiteName = "Unknown campsite",
                allowCsvExport = true,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Reservation not found.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Download CSV")
            .assertDoesNotExist()
    }
}