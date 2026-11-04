package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests profile setup validation and the values submitted for saving.
 */
@RunWith(AndroidJUnit4::class)
class CreateProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var submittedName: String? = null
    private var submittedEmail: String? = null
    private var submittedPhoneNumber: String? = null

    @Before
    fun setUp() {
        composeRule.setContent {
            ChargeReservationTheme {
                CreateProfileScreen(
                    onCreateProfile = { name, email, phoneNumber ->
                        submittedName = name
                        submittedEmail = email
                        submittedPhoneNumber = phoneNumber
                    }
                )
            }
        }
    }

    @Test
    fun blankName_showsErrorWithoutSubmitting() {
        composeRule.onNodeWithText("Create Profile").performClick()

        composeRule.onNodeWithText("Please enter your name.").assertIsDisplayed()

        composeRule.runOnIdle { assertNull(submittedName) }
    }

    @Test
    fun invalidEmail_showsErrorWithoutSubmitting() {
        composeRule.onNodeWithText("Name").performTextInput("Test Name")

        composeRule.onNodeWithText("Email").performTextInput("test email")

        composeRule.onNodeWithText("Create Profile").performClick()

        composeRule.onNodeWithText("Please enter a valid email address.").assertIsDisplayed()

        composeRule.runOnIdle { assertNull(submittedName) }
    }

    @Test
    fun validInformationWithSpaces_submitsTrimmedValues() {
        composeRule.onNodeWithText("Name").performTextInput("  Test Name  ")

        composeRule.onNodeWithText("Email").performTextInput("  test@testmail.com  ")

        composeRule.onNodeWithText("Phone number (optional)").performTextInput("  1234567890  ")

        composeRule.onNodeWithText("Create Profile").performClick()

        composeRule.runOnIdle {
            assertEquals("Test Name", submittedName)
            assertEquals("test@testmail.com", submittedEmail)
            assertEquals("1234567890", submittedPhoneNumber)
        }
    }
}