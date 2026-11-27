package com.charge.reservation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/** Tests the owner campsite picker and entrant lottery picker for US 02.04.02. */
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
