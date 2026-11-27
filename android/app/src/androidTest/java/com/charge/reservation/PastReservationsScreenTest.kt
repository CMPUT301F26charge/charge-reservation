package com.charge.reservation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Reservation
import com.charge.reservation.reservation.PastReservationsScreen
import com.charge.reservation.reservation.ReservationDetailsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests the past reservations screen for US 01.06.02 (#17).
 */
@RunWith(AndroidJUnit4::class)
class PastReservationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 26)

    private val reservation = Reservation(
        id = "res-1",
        campsiteId = "site-1",
        entrantId = "entrant-1",
        checkInDate = today.minusDays(5),
        checkOutDate = today.minusDays(3),
        guestCount = 4
    )

    /** Checks that past reservation information is displayed. */
    @Test
    fun pastReservationIsDisplayed() {
        composeRule.setContent {
            PastReservationsScreen(
                entrantId = "entrant-1",
                reservations = listOf(reservation),
                campsiteNames = mapOf("site-1" to "Pine Lake"),
                onReservationClick = {},
                today = today,
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Pine Lake").assertIsDisplayed()
        composeRule.onNodeWithText("Reservation: res-1").assertIsDisplayed()
        composeRule.onNodeWithText("Check-in: 2026-11-21").assertIsDisplayed()
        composeRule.onNodeWithText("Check-out: 2026-11-23").assertIsDisplayed()
    }

    /** Checks that selecting a reservation can open its details. */
    @Test
    fun openPastReservationDetails() {
        composeRule.setContent {
            val selected = remember {
                mutableStateOf(false)
            }

            if (selected.value) {
                ReservationDetailsScreen(
                    reservation = reservation,
                    campsiteName = "Pine Lake",
                    onBackClick = { selected.value = false }
                )
            } else {
                PastReservationsScreen(
                    entrantId = "entrant-1",
                    reservations = listOf(reservation),
                    campsiteNames = mapOf("site-1" to "Pine Lake"),
                    onReservationClick = {
                        selected.value = it == reservation.id
                    },
                    today = today,
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Pine Lake").performClick()
        composeRule.onNodeWithText("Reservation Details").assertIsDisplayed()
        composeRule.onNodeWithText("Reservation ID: res-1").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("My Past Reservations").assertIsDisplayed()
    }

    /** Checks the empty-state message and back button. */
    @Test
    fun emptyPastReservations() {
        var backClicked = false

        composeRule.setContent {
            PastReservationsScreen(
                entrantId = "entrant-1",
                reservations = emptyList(),
                campsiteNames = emptyMap(),
                onReservationClick = {},
                today = today,
                onBackClick = { backClicked = true }
            )
        }

        composeRule.onNodeWithText(
            "You have no past reservations."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.runOnIdle {
            assertTrue(backClicked)
        }
    }
}