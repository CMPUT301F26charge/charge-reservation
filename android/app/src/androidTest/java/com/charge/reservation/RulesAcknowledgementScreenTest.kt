package com.charge.reservation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.booking.RulesAcknowledgementScreen
import com.charge.reservation.model.Campsite
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Intent tests for campsite rules acknowledgement.
 */
@RunWith(AndroidJUnit4::class)
class RulesAcknowledgementScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun confirmationRequiresAcknowledgement() {
        var confirmCount = 0

        val campsite = Campsite(
            id = "camp-001",
            name = "Pine Lake Campground",
            rules = "Quiet hours begin at 10 PM",
            cancellationPolicy = "Cancel 48 hours in advance"
        )

        rule.setContent {
            RulesAcknowledgementScreen(
                campsite = campsite,
                bookingId = "test-booking-001",
                onConfirmClick = {
                    confirmCount++
                },
                onBackClick = {}
            )
        }

        val confirmButton = rule.onNodeWithText(
            "Continue to Confirmation"
        )

        // Initially disabled.
        confirmButton.assertIsNotEnabled()

        // Rules and policies must be visible.
        rule.onNodeWithText(
            "Quiet hours begin at 10 PM"
        ).assertExists()

        rule.onNodeWithText(
            "Cancel 48 hours in advance"
        ).assertExists()

        // Check acknowledgement.
        rule.onNodeWithText(
            "I have read and agree to the campsite rules and policies."
        ).performClick()

        confirmButton.assertIsEnabled()
        confirmButton.performClick()

        rule.runOnIdle {
            assertEquals(1, confirmCount)
        }
    }
}
