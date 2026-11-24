package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.charge.reservation.model.Reservation
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

    private val testLake = CampsiteOption(id = "c1", name = "Test Lake")
    private val testRidge = CampsiteOption(id = "c2", name = "Test Ridge")

    private fun showWon(
        onDecline: () -> Unit = {},
        isDeclining: Boolean = false,
        declineError: Boolean = false,
        campsites: List<CampsiteOption> = listOf(testLake),
        onOpenInvitation: (String?) -> Unit = {}
    ) = show { _ ->
        LotteryWonNotificationScreen(
            lotteryName = "Test Lake Lottery",
            lotteryCode = "LOT-999",
            bookingDeadline = "Nov 1, 2026",
            campsites = campsites,
            onOpenInvitation = onOpenInvitation,
            onDecline = onDecline,
            isDeclining = isDeclining,
            declineError = if (declineError) "Couldn't decline the invitation. Please try again." else null
        )
    }

    @Test
    fun won_showsTitle() {
        showWon()

        composeRule.onNodeWithText("Lottery Won").assertIsDisplayed()
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
        composeRule.onNodeWithText("Campsite: Test Lake").assertIsDisplayed()
        composeRule.onNodeWithText("Book by: Nov 1, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to see available dates and book your stay.").assertIsDisplayed()
    }

    @Test
    fun won_showsOpenAndDeclineButtons() {
        showWon()

        composeRule.onNodeWithText("Open invitation & book").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Decline invitation").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun won_singleCampsite_openButtonPassesThatCampsite() {
        var opened: String? = "not called"
        showWon(onOpenInvitation = { opened = it })

        composeRule.onNodeWithText("Open invitation & book").performClick()

        assertEquals("c1", opened)
        composeRule.onNodeWithText("Choose a campsite").assertDoesNotExist()
    }


    @Test
    fun won_multipleCampsites_tappingCampsitePassesItsId() {
        var opened: String? = "not called"
        showWon(campsites = listOf(testLake, testRidge), onOpenInvitation = { opened = it })

        composeRule.onNodeWithText("Test Ridge").performClick()

        assertEquals("c2", opened)
    }


    @Test
    fun won_bookingInvitationCard_isClickable() {
        showWon()

        composeRule.onNodeWithText("Booking invitation").assertHasClickAction()
    }

    @Test
    fun won_hasNoOwnBackArrowOrBottomTabs() {
        showWon()

        // The app's Scaffold provides the back arrow and bottom tabs, so the screen draws neither.
        composeRule.onNodeWithText("‹").assertDoesNotExist()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertDoesNotExist()
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

    private fun showLost(onBack: () -> Unit = {}) = show { _ ->
        LotteryLostNotificationScreen(
            lotteryName = "Test Ridge Lottery",
            lotteryCode = "LOT-123",
            onBack = onBack
        )
    }

    @Test
    fun lost_showsTitle() {
        showLost()

        composeRule.onNodeWithText("Lottery Result").assertIsDisplayed()
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
        composeRule.onNodeWithText("Your entry ended with this draw.").assertIsDisplayed()
        composeRule.onNodeWithText("Explore other open campsite lotteries.").assertIsDisplayed()
    }

    @Test
    fun lost_backButton_callsOnBack() {
        var backCalls = 0
        showLost(onBack = { backCalls++ })

        composeRule.onNodeWithText("Back to notifications").assertIsDisplayed().performClick()

        assertEquals(1, backCalls)
    }

    @Test
    fun lost_hasNoDeclineOrOpenInvitationButtons() {
        showLost()

        composeRule.onNodeWithText("Decline invitation").assertDoesNotExist()
        composeRule.onNodeWithText("Open invitation & book").assertDoesNotExist()
    }

    @Test
    fun lost_hasNoOwnBackArrowOrBottomTabs() {
        showLost()

        // The app's Scaffold provides the back arrow and bottom tabs, so the screen draws neither.
        composeRule.onNodeWithText("‹").assertDoesNotExist()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertDoesNotExist()
        }
    }

    // ------------------------------------------------------------------
    // LotteryStatusMessageScreen
    // ------------------------------------------------------------------

    @Test
    fun status_showsTitleHeadingAndMessage() {
        show { _ ->
            LotteryStatusMessageScreen(
                onBack = {},
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
        show { _ ->
            LotteryStatusMessageScreen(
                onBack = {},
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
        show { _ ->
            LotteryStatusMessageScreen(
                onBack = {},
                heading = "Lottery not found",
                message = "This lottery or your entry could not be found."
            )
        }

        composeRule.onNodeWithText("•", substring = true).assertDoesNotExist()
    }

    @Test
    fun status_showsBackButton_andNoWonOrLostButtons() {
        show { _ ->
            LotteryStatusMessageScreen(
                onBack = {},
                heading = "Awaiting the draw",
                message = "Not announced yet."
            )
        }

        composeRule.onNodeWithText("Back to notifications").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Decline invitation").assertDoesNotExist()
        composeRule.onNodeWithText("Open invitation & book").assertDoesNotExist()
    }

    @Test
    fun status_hasNoOwnBackArrowOrBottomTabs() {
        show { _ ->
            LotteryStatusMessageScreen(
                onBack = {},
                heading = "Awaiting the draw",
                message = "Not announced yet."
            )
        }

        // The app's Scaffold provides the back arrow and bottom tabs, so the screen draws neither.
        composeRule.onNodeWithText("‹").assertDoesNotExist()
        tabLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertDoesNotExist()
        }
    }

    // ------------------------------------------------------------------
    // LotteryBookingConfirmedScreen
    // ------------------------------------------------------------------

    @Test
    fun confirmed_showsReservationDetails() {
        show { _ ->
            LotteryBookingConfirmedScreen(
                lotteryName = "Test Lake Lottery",
                lotteryCode = "LOT-999",
                campsiteName = "Test Lake",
                reservation = Reservation(id = "res-1", campsiteId = "c1", guestCount = 2),
                onBack = {}
            )
        }

        composeRule.onNodeWithText("Invitation Used").assertIsDisplayed()
        composeRule.onNodeWithText("You're booked!").assertIsDisplayed()
        composeRule.onNodeWithText("Campsite: Test Lake").assertIsDisplayed()
        composeRule.onNodeWithText("Guests: 2").assertIsDisplayed()
        composeRule.onNodeWithText("Reservation ID: res-1").assertIsDisplayed()
    }
}
