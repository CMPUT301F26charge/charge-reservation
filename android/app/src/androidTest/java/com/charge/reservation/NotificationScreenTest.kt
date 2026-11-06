package com.charge.reservation.notifications

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.charge.reservation.model.Notification
import com.charge.reservation.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Tests that notification screen is able to correctly display notifications
 */
class NotificationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lotteryNotification = Notification(
        id = "n1",
        recipientId = "u1",
        type = NotificationType.SYSTEM,
        content = "Pine Lake: You were selected!\nOpen your invitation to book dates",
        lotteryId = "lottery-1"
    )

    private val ownerNotification = Notification(
        id = "n2",
        recipientId = "u1",
        type = NotificationType.OWNER_CUSTOM,
        content = "Reminder: Review campsite arrival rules"
    )

    private val plainNotification = Notification(
        id = "n3",
        recipientId = "u1",
        type = NotificationType.SYSTEM,
        content = "Your reservation was updated"
    )

    @Test
    fun emptyList_showsEmptyMessage() {
        composeRule.setContent {
            NotificationsScreen(notifications = emptyList(), onOpenLottery = {}, navController = rememberNavController())
        }

        composeRule.onNodeWithText("No notifications yet").assertIsDisplayed()
    }


    @Test
    fun showsTitleAndRoleLabel() {
        composeRule.setContent {
            NotificationsScreen(
                notifications = emptyList(),
                onOpenLottery = {},
                roleLabel = "ENTRANT • TEST",
                navController = rememberNavController()
            )
        }

        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithText("ENTRANT • TEST").assertIsDisplayed()
    }

    @Test
    fun notificationContent_isShownOneLinePerLineBreak() {
        composeRule.setContent {
            NotificationsScreen(notifications = listOf(lotteryNotification), onOpenLottery = {}, navController = rememberNavController())
        }

        composeRule.onNodeWithText("Pine Lake: You were selected!").assertIsDisplayed()
        composeRule.onNodeWithText("Open your invitation to book dates").assertIsDisplayed()
    }

    @Test
    fun ownerNotification_showsOwnerTitle() {
        composeRule.setContent {
            NotificationsScreen(notifications = listOf(ownerNotification), onOpenLottery = {}, navController = rememberNavController())
        }

        composeRule.onNodeWithText("From campsite owner").assertIsDisplayed()
    }

    @Test
    fun lotteryNotification_showsLotteryTitle() {
        composeRule.setContent {
            NotificationsScreen(notifications = listOf(lotteryNotification), onOpenLottery = {}, navController = rememberNavController())
        }

        composeRule.onNodeWithText("Lottery update").assertIsDisplayed()
    }

    @Test
    fun plainNotification_showsGenericTitle() {
        composeRule.setContent {
            NotificationsScreen(notifications = listOf(plainNotification), onOpenLottery = {}, navController = rememberNavController())
        }

        composeRule.onNodeWithText("Notification").assertIsDisplayed()
    }

    @Test
    fun viewLotteryButton_appearsOnlyOnLotteryNotifications() {
        composeRule.setContent {
            NotificationsScreen(
                notifications = listOf(lotteryNotification, ownerNotification, plainNotification),
                onOpenLottery = {},
                navController = rememberNavController()
            )
        }

        composeRule.onAllNodesWithText("View lottery →").assertCountEquals(1)
    }

    @Test
    fun viewLotteryButton_callsCallbackWithLotteryId() {
        var openedId: String? = null
        composeRule.setContent {
            NotificationsScreen(
                notifications = listOf(lotteryNotification),
                onOpenLottery = { openedId = it },
                navController = rememberNavController()
            )
        }

        composeRule.onNodeWithText("View lottery →").performClick()

        assertEquals("lottery-1", openedId)
    }

    @Test
    fun tappingOwnerNotification_doesNotOpenLottery() {
        var openedId: String? = null
        composeRule.setContent {
            NotificationsScreen(
                notifications = listOf(ownerNotification),
                onOpenLottery = { openedId = it },
                navController = rememberNavController()
            )
        }

        composeRule.onNodeWithText("From campsite owner").performClick()

        assertNull(openedId)
    }

    @Test
    fun bottomBar_showsAllFourTabs() {
        composeRule.setContent {
            NotificationsScreen(notifications = emptyList(), onOpenLottery = {}, navController = rememberNavController())
        }

        listOf("Explore", "Entries", "Bookings", "Profile").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }
}