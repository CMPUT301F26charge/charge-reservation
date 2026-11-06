package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 *Tests to see if the lottery screens display correctly
 */
class LotteryNotificationScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tabLabels = listOf("Explore", "Entries", "Bookings", "Profile")

    private fun show(content: @Composable (NavHostController) -> Unit) {
        composeRule.setContent {
            val navController = rememberNavController()
            content(navController)
        }
    }

    // ------------------------------------------------------------------
    // LotteryWonNotificationScreen
    // ------------------------------------------------------------------

    private fun showWon(
        onDecline: () -> Unit = {},
        isDeclining: Boolean = false,
        declineError: Boolean = false
    ) = show { nav ->
        LotteryWonNotificationScreen(
            navController = nav,
            lotteryId = "test-lottery",
            lotteryName = "Test Lake Lottery",
            lotteryCode = "LOT-999",
            bookingDeadline = "Nov 1, 2026",
            onDecline = onDecline,
            isDeclining = isDeclining,
            declineError = declineError
        )
    }

    @Test
    fun won_showsTitleAndRoleLabel() {
        showWon()

        composeRule.onNodeWithText("Lottery Won Notification").assertIsDisplayed()
        composeRule.onNodeWithText("ENTRANT • US 01.03.01").assertIsDisplayed()
    }

    @Test
    fun won_showsOutcomeAndLotteryDetails() {
        showWon()

        composeRule.onNodeWithText("You won the lottery!").assertIsDisplayed()
        composeRule.onNodeWithText("Test Lake Lottery • LOT-999").assertIsDisplayed()
        composeRule.onNodeWithText("You were selected to book available dates.").assertIsDisplayed()
    }

    @Test
    fun won_showsBookingInvitationWithDeadline() {
        showWon()

        composeRule.onNodeWithText("Booking invitation").assertIsDisplayed()
        composeRule.onNodeWithText("Booking deadline: Nov 1, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to review the winning invitation.").assertIsDisplayed()
    }

    @Test
    fun won_showsOpenAndDeclineButtons() {
        showWon()

        composeRule.onNodeWithText("Open winning invitation").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Decline invitation").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun won_bookingInvitationCard_isClickable() {
        showWon()

        composeRule.onNodeWithText("Booking invitation").assertHasClickAction()
    }

    @Test
    fun won_backArrowAndBottomTabs_areClickable() {
        showWon()

        composeRule.onNodeWithText("‹").assertIsDisplayed().assertHasClickAction()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun won_declineDialog_isHiddenUntilDeclineIsTapped() {
        showWon()

        composeRule.onNodeWithText("Decline this invitation?").assertDoesNotExist()
    }

    @Test
    fun won_declineButton_opensConfirmationDialog() {
        showWon()

        composeRule.onNodeWithText("Decline invitation").performClick()

        composeRule.onNodeWithText("Decline this invitation?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep invitation").assertIsDisplayed()
        composeRule.onNodeWithText("Decline").assertIsDisplayed()
    }

    @Test
    fun won_keepInvitation_closesDialogWithoutDeclining() {
        var declineCalls = 0
        showWon(onDecline = { declineCalls++ })

        composeRule.onNodeWithText("Decline invitation").performClick()
        composeRule.onNodeWithText("Keep invitation").performClick()

        composeRule.onNodeWithText("Decline this invitation?").assertDoesNotExist()
        assertEquals(0, declineCalls)
    }

    @Test
    fun won_confirmDecline_callsOnDeclineOnceAndClosesDialog() {
        var declineCalls = 0
        showWon(onDecline = { declineCalls++ })

        composeRule.onNodeWithText("Decline invitation").performClick()
        composeRule.onNodeWithText("Decline").performClick()

        composeRule.onNodeWithText("Decline this invitation?").assertDoesNotExist()
        assertEquals(1, declineCalls)
    }

    @Test
    fun won_whileDeclining_showsProgressLabel() {
        showWon(isDeclining = true)

        composeRule.onNodeWithText("Declining…").assertIsDisplayed()
        composeRule.onNodeWithText("Decline invitation").assertDoesNotExist()
    }

    @Test
    fun won_whileDeclining_tappingDoesNotOpenDialog() {
        showWon(isDeclining = true)

        composeRule.onNodeWithText("Declining…").performClick()

        composeRule.onNodeWithText("Decline this invitation?").assertDoesNotExist()
    }

    @Test
    fun won_declineError_isHiddenByDefault() {
        showWon(declineError = false)

        composeRule.onNodeWithText("Couldn't decline the invitation. Please try again.")
            .assertDoesNotExist()
    }

    @Test
    fun won_declineError_showsMessageWhenSet() {
        showWon(declineError = true)

        composeRule.onNodeWithText("Couldn't decline the invitation. Please try again.")
            .assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // LotteryLostNotificationScreen
    // ------------------------------------------------------------------

    private fun showLost() = show { nav ->
        LotteryLostNotificationScreen(
            navController = nav,
            lotteryName = "Test Ridge Lottery",
            lotteryCode = "LOT-123"
        )
    }

    @Test
    fun lost_showsTitleAndRoleLabel() {
        showLost()

        composeRule.onNodeWithText("Lottery Lost Notification").assertIsDisplayed()
        composeRule.onNodeWithText("ENTRANT • US 01.03.02").assertIsDisplayed()
    }

    @Test
    fun lost_showsOutcomeAndLotteryDetails() {
        showLost()

        composeRule.onNodeWithText("Not selected this time").assertIsDisplayed()
        composeRule.onNodeWithText("Test Ridge Lottery • LOT-123").assertIsDisplayed()
        composeRule.onNodeWithText("You were not drawn for this lottery.").assertIsDisplayed()
    }

    @Test
    fun lost_showsResultDetailsCard() {
        showLost()

        composeRule.onNodeWithText("Result details").assertIsDisplayed()
        composeRule.onNodeWithText("Your entered membership ended with this draw.").assertIsDisplayed()
        composeRule.onNodeWithText("Review other open campsite lotteries.").assertIsDisplayed()
    }

    @Test
    fun lost_showsViewResultButton() {
        showLost()

        composeRule.onNodeWithText("View lottery result").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun lost_hasNoDeclineOrOpenInvitationButtons() {
        showLost()

        composeRule.onNodeWithText("Decline invitation").assertDoesNotExist()
        composeRule.onNodeWithText("Open winning invitation").assertDoesNotExist()
    }

    @Test
    fun lost_backArrowAndBottomTabs_areClickable() {
        showLost()

        composeRule.onNodeWithText("‹").assertIsDisplayed().assertHasClickAction()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
        }
    }

    // ------------------------------------------------------------------
    // LotteryStatusMessageScreen
    // ------------------------------------------------------------------

    @Test
    fun status_showsTitleHeadingAndMessage() {
        show { nav ->
            LotteryStatusMessageScreen(
                navController = nav,
                heading = "Invitation expired",
                message = "The booking deadline for this lottery has passed."
            )
        }

        composeRule.onNodeWithText("Lottery Notification").assertIsDisplayed()
        composeRule.onNodeWithText("Invitation expired").assertIsDisplayed()
        composeRule.onNodeWithText("The booking deadline for this lottery has passed.").assertIsDisplayed()
    }

    @Test
    fun status_showsLotteryLine_whenNameAndCodeProvided() {
        show { nav ->
            LotteryStatusMessageScreen(
                navController = nav,
                heading = "Invitation used",
                message = "You already booked a reservation with this invitation.",
                lotteryName = "Test Lake Lottery",
                lotteryCode = "LOT-999"
            )
        }

        composeRule.onNodeWithText("Test Lake Lottery • LOT-999").assertIsDisplayed()
    }

    @Test
    fun status_hidesLotteryLine_whenNameOrCodeMissing() {
        show { nav ->
            LotteryStatusMessageScreen(
                navController = nav,
                heading = "Lottery not found",
                message = "This lottery or your entry could not be found."
            )
        }

        composeRule.onNodeWithText("•", substring = true).assertDoesNotExist()
    }

    @Test
    fun status_showsBackButton_andNoWonOrLostButtons() {
        show { nav ->
            LotteryStatusMessageScreen(
                navController = nav,
                heading = "Awaiting the draw",
                message = "Not announced yet."
            )
        }

        composeRule.onNodeWithText("Back to notifications").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Decline invitation").assertDoesNotExist()
        composeRule.onNodeWithText("Open winning invitation").assertDoesNotExist()
        composeRule.onNodeWithText("View lottery result").assertDoesNotExist()
    }

    @Test
    fun status_backArrowAndBottomTabs_areClickable() {
        show { nav ->
            LotteryStatusMessageScreen(
                navController = nav,
                heading = "Awaiting the draw",
                message = "Not announced yet."
            )
        }

        composeRule.onNodeWithText("‹").assertIsDisplayed().assertHasClickAction()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
        }
    }
}