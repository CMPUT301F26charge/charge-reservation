package com.charge.reservation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Campsite
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the campsite details submitted by the campsite creation screen.
 */
@RunWith(AndroidJUnit4::class)
class CreateCampsiteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun validDetails_submitsTrimmedValuesWithSuppliedIds() {
        var submittedCampsite: Campsite? = null

        composeRule.setContent {
            ChargeReservationTheme {
                CreateCampsiteScreen(
                    ownerId = "test-owner",
                    campsiteId = "test-campsite",
                    isSaving = false,
                    errorMessage = "",
                    onCreateCampsite = {
                        submittedCampsite = it
                    }
                )
            }
        }

        composeRule.onNodeWithText("Campsite name")
            .performScrollTo()
            .performTextInput("  Test Campsite  ")

        composeRule.onNodeWithText("Description")
            .performScrollTo()
            .performTextInput("  Test Description.  ")

        composeRule.onNodeWithText("Location")
            .performScrollTo()
            .performTextInput("  Edmonton, Alberta  ")

        composeRule.onNodeWithText("Maximum occupancy")
            .performScrollTo()
            .performTextInput("  4  ")

        composeRule.onNodeWithText("Price per night")
            .performScrollTo()
            .performTextInput("  25.50  ")

        composeRule.onNodeWithText("Rules")
            .performScrollTo()
            .performTextInput("  Keep the site clean.  ")

        composeRule.onNodeWithText("Cancellation policy")
            .performScrollTo()
            .performTextInput("  Cancel before check-in.  ")

        composeRule.onNodeWithText("Create Campsite")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            val campsite = submittedCampsite

            assertEquals("test-campsite", campsite?.id)
            assertEquals("test-owner", campsite?.ownerId)
            assertEquals("Test Campsite", campsite?.name)
            assertEquals("Test Description.", campsite?.description)
            assertEquals("Edmonton, Alberta", campsite?.location)
            assertEquals(4, campsite?.maxOccupancy)
            assertEquals(
                25.5,
                campsite?.pricePerNight ?: -1.0,
                0.001
            )
            assertEquals("Keep the site clean.", campsite?.rules)
            assertEquals(
                "Cancel before check-in.",
                campsite?.cancellationPolicy
            )
        }
    }
}