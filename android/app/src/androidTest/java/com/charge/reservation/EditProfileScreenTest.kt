
package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.UserProfile
import com.charge.reservation.profile.EditProfileScreen
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for US 01.07.03:
 * Editing personal information.
 */
@RunWith(AndroidJUnit4::class)
class EditProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedName: String? = null
    private var savedEmail: String? = null
    private var savedPhone: String? = null

    private val testProfile = UserProfile(
        id = "test-user-123",
        name = "Original Name",
        email = "original@example.com",
        phoneNumber = "1234567890"
    )

    @Before
    fun setUp() {
        savedName = null
        savedEmail = null
        savedPhone = null

        composeRule.setContent {
            ChargeReservationTheme {
                EditProfileScreen(
                    profile = testProfile,
                    isSaving = false,
                    saveError = "",
                    onSave = { name, email, phone, _ ->
                        savedName = name
                        savedEmail = email
                        savedPhone = phone
                    }
                )
            }
        }
    }

    @Test
    fun existingProfileInformationIsDisplayed() {
        composeRule
            .onNodeWithText("Original Name")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("original@example.com")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("1234567890")
            .assertIsDisplayed()
    }

    @Test
    fun blankNameIsRejected() {
        composeRule
            .onNodeWithText("Name")
            .performTextClearance()

        composeRule
            .onNodeWithText("Save Changes")
            .performClick()

        composeRule
            .onNodeWithText("Please enter your name.")
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertNull(savedName)
        }
    }

    @Test
    fun invalidEmailIsRejected() {
        composeRule
            .onNodeWithText("Email")
            .performTextClearance()

        composeRule
            .onNodeWithText("Email")
            .performTextInput("invalid-email")

        composeRule
            .onNodeWithText("Save Changes")
            .performClick()

        composeRule
            .onNodeWithText(
                "Please enter a valid email address."
            )
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertNull(savedEmail)
        }
    }

    @Test
    fun validChangesAreSubmitted() {
        composeRule
            .onNodeWithText("Name")
            .performTextClearance()

        composeRule
            .onNodeWithText("Name")
            .performTextInput("Updated Name")

        composeRule
            .onNodeWithText("Email")
            .performTextClearance()

        composeRule
            .onNodeWithText("Email")
            .performTextInput("updated@example.com")

        composeRule
            .onNodeWithText("Phone number (optional)")
            .performTextClearance()

        composeRule
            .onNodeWithText("Phone number (optional)")
            .performTextInput("9876543210")

        composeRule
            .onNodeWithText("Save Changes")
            .performClick()

        composeRule.runOnIdle {
            assertEquals("Updated Name", savedName)
            assertEquals("updated@example.com", savedEmail)
            assertEquals("9876543210", savedPhone)
        }
    }
}
