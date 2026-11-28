package com.charge.reservation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.lottery.CreateLotteryScreen
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.ui.CampsiteDetailsScreen
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Tests owner lottery creation and editing, and entrant lottery selection. */
@RunWith(AndroidJUnit4::class)
class LotteryCampsiteSelectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ownerCanSelectTwoDistinctActiveCampsites() {
        val pine = Campsite(id = "pine", ownerId = "owner-1", name = "Pine Lake")
        val river = Campsite(id = "river", ownerId = "owner-1", name = "River Camp")
        val removed = Campsite(
            id = "removed", ownerId = "owner-1", name = "Old Camp", removed = true
        )
        val otherOwner = Campsite(id = "other", ownerId = "owner-2", name = "Other Camp")

        composeRule.setContent {
            ChargeReservationTheme {
                CreateLotteryScreen(
                    campsite = pine,
                    availableCampsites = listOf(pine, river, river, removed, otherOwner),
                    lotteryId = "test-lottery",
                    isSaving = false,
                    errorMessage = "",
                    onCreateLottery = {}
                )
            }
        }

        composeRule.onNodeWithText("Pine Lake").assertIsOn()
        composeRule.onAllNodesWithText("River Camp").assertCountEquals(1)
        composeRule.onNodeWithText("Old Camp").assertDoesNotExist()
        composeRule.onNodeWithText("Other Camp").assertDoesNotExist()

        val riverOption = composeRule.onNodeWithText("River Camp")
        riverOption.assertIsOff()
        riverOption.performClick()
        riverOption.assertIsOn()
        composeRule.onNodeWithText("Pine Lake").assertIsOn()

        riverOption.performClick()
        riverOption.assertIsOff()
    }

    @Test
    fun editingLotteryPrefillsSavedSettingsAndSubmitsChangesWithTheSameId() {
        val pine = Campsite(id = "pine", ownerId = "owner-1", name = "Pine Lake")
        val river = Campsite(id = "river", ownerId = "owner-1", name = "River Camp")
        val saved = Lottery(
            id = "lottery-1",
            ownerId = pine.ownerId,
            campsiteIds = listOf(pine.id, river.id),
            registrationStartAt = Instant.parse("2027-06-02T01:30:25Z"),
            registrationEndAt = Instant.parse("2027-06-04T01:30:00Z"),
            bookingDeadline = Instant.parse("2027-06-05T02:45:30Z"),
            bookingStartDate = LocalDate.of(2027, 6, 5),
            bookingEndDate = LocalDate.of(2027, 6, 7),
            timeZone = ZoneId.of("America/Edmonton"),
            winnerCount = 2,
            waitlistCapacity = 12
        )
        var submitted: Lottery? = null

        composeRule.setContent {
            ChargeReservationTheme {
                CreateLotteryScreen(
                    campsite = pine,
                    availableCampsites = listOf(pine, river),
                    lotteryId = saved.id,
                    isSaving = false,
                    errorMessage = "",
                    savedLottery = saved,
                    onCreateLottery = { submitted = it }
                )
            }
        }

        composeRule.onNodeWithText("Edit lottery").assertExists()
        composeRule.onNodeWithText("Pine Lake").assertIsOn()
        composeRule.onNodeWithText("River Camp").assertIsOn().performClick()
        composeRule.onNodeWithText("Registration opens: 2027-06-01").assertExists()
        composeRule.onNodeWithText("Opening time: 19:30").assertExists()
        composeRule.onNodeWithText("Number of winners").performScrollTo().performTextReplacement("3")
        composeRule.onNodeWithText("Save changes").performScrollTo().assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(saved.copy(campsiteIds = listOf(pine.id), winnerCount = 3), submitted)
        }
    }

    @Test
    fun entrantCanChooseSharedLotteryAlongsideAnotherListing() {
        val campsite = Campsite(id = "pine", name = "Pine Lake")
        val single = Lottery(id = "single", campsiteIds = listOf("pine"))
        val shared = Lottery(id = "shared", campsiteIds = listOf("pine", "river"))
        var selectedId: String? = null

        composeRule.setContent {
            ChargeReservationTheme {
                CampsiteDetailsScreen(
                    campsite = campsite,
                    onBack = {},
                    lottery = single,
                    lotteryChoices = listOf(single, shared),
                    onSelectLottery = { selectedId = it }
                )
            }
        }

        composeRule.onNodeWithText("Choose lottery (LOT-SINGLE)").performScrollTo().performClick()
        composeRule.onNodeWithText("LOT-SHARED").performClick()
        composeRule.runOnIdle { assertEquals(shared.id, selectedId) }
    }
}
