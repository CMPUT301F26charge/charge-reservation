
package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.profile.NotificationSettingsScreen
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.platform.testTag

/**
 * UI tests for US 01.10.01 notification preferences.
 */
@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submittedValue: Boolean? = null

    @Test
    fun existingPreferenceIsDisplayed() {
        composeRule.setContent {
            ChargeReservationTheme {
                NotificationSettingsScreen(
                    notificationEnabled = true,
                    isSaving = false,
                    errorMessage = "",
                    onSave = { submittedValue = it }
                )
            }
        }

        composeRule
            .onNodeWithText("Notifications enabled")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Save Preference")
            .assertIsNotEnabled()
    }

    @Test
    fun disablingNotificationsSubmitsFalse() {
        composeRule.setContent {
            ChargeReservationTheme {
                NotificationSettingsScreen(
                    notificationEnabled = true,
                    isSaving = false,
                    errorMessage = "",
                    onSave = { submittedValue = it }
                )
            }
        }

        composeRule
            .onNodeWithTag("notificationToggle")
            .performClick()

        composeRule
            .onNodeWithText("Save Preference")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(false, submittedValue)
        }
    }

    @Test
    fun enablingNotificationsSubmitsTrue() {
        composeRule.setContent {
            ChargeReservationTheme {
                NotificationSettingsScreen(
                    notificationEnabled = false,
                    isSaving = false,
                    errorMessage = "",
                    onSave = { submittedValue = it }
                )
            }
        }

        composeRule
            .onNodeWithTag("notificationToggle")
            .performClick()

        composeRule
            .onNodeWithText("Save Preference")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(true, submittedValue)
        }
    }

    @Test
    fun unchangedPreferenceCannotBeSubmitted() {
        composeRule.setContent {
            ChargeReservationTheme {
                NotificationSettingsScreen(
                    notificationEnabled = false,
                    isSaving = false,
                    errorMessage = "",
                    onSave = { submittedValue = it }
                )
            }
        }

        composeRule
            .onNodeWithText("Save Preference")
            .assertIsNotEnabled()

        composeRule.runOnIdle {
            assertNull(submittedValue)
        }
    }
}
