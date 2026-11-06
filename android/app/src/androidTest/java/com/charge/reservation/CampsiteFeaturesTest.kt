package com.charge.reservation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.entrant.filterCampsites
import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.ui.CampsiteCard
import com.charge.reservation.ui.CampsiteDetailsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Tests campsite discovery and details, US 01.01.01 through US 01.01.06. */
@RunWith(AndroidJUnit4::class)
class CampsiteFeaturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** US 01.01.01: Finds a campsite by name, ignoring case and extra spaces. */
    @Test
    fun searchByName() {
        val sites = listOf(
            Campsite(id = "1", name = "Pine Lake"),
            Campsite(id = "2", name = "River Camp")
        )
        val result = filterCampsites(sites, "  pInE  ", "", null, null, null, emptyMap())
        assertEquals(listOf("1"), result.map { it.id })
    }

    /** US 01.01.02: Combined location, guest and date filters all apply. */
    @Test
    fun combinedFilters() {
        val start = LocalDate.of(2027, 6, 10)
        val sites = listOf(
            Campsite(id = "1", name = "Pine Lake", location = "Edmonton", maxOccupancy = 5),
            Campsite(id = "2", name = "Pine Lake", location = "Calgary", maxOccupancy = 5),
            Campsite(id = "3", name = "Pine Lake", location = "Edmonton", maxOccupancy = 2)
        )
        val days = mapOf("1" to listOf(
            AvailabilityDay("1", start, 5, 1),
            AvailabilityDay("1", start.plusDays(1), 5, 1)
        ))
        val result = filterCampsites(sites, "pine", "edmonton", 4, start, start.plusDays(2), days)
        assertEquals(listOf("1"), result.map { it.id })
    }

    /** US 01.01.03: Only campsites matching the selected dates appear. */
    @Test
    fun unavailableDatesAreExcluded() {
        val start = LocalDate.of(2027, 6, 10)
        val sites = listOf(Campsite(id = "1", name = "Pine Lake", maxOccupancy = 4))
        val days = mapOf("1" to listOf(AvailabilityDay("1", start, 4, 0)))
        val result = filterCampsites(sites, "", "", 2, start, start.plusDays(2), days)
        assertTrue(result.isEmpty())
    }

    /** US 01.01.04: The summary card shows its information and can be selected. */
    @Test
    fun summaryCard() {
        var clicked = false
        composeRule.setContent {
            CampsiteCard(
                campsite = Campsite("1", name = "Pine Lake", location = "Edmonton",
                    maxOccupancy = 4, averageRating = 4.2),
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Edmonton • Maximum 4 guests").assertIsDisplayed()
        composeRule.onNodeWithText("Rating: ★ 4.2").assertIsDisplayed()
        composeRule.onNodeWithText("Pine Lake").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    /** US 01.01.05: The details screen displays price and campsite policies. */
    @Test
    fun campsiteDetails() {
        composeRule.setContent {
            CampsiteDetailsScreen(
                campsite = Campsite("1", name = "Pine Lake", location = "Edmonton",
                    maxOccupancy = 4, pricePerNight = 35.0,
                    rules = "No fires allowed", cancellationPolicy = "Cancel 24 hours early"),
                onBack = {}
            )
        }
        composeRule.onNodeWithText("Pine Lake").assertIsDisplayed()
        composeRule.onNodeWithText("$35.00 per night").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("No fires allowed").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancel 24 hours early").performScrollTo().assertIsDisplayed()
    }

    /** US 01.01.06: An open lottery enables the details screen's join action.
     * The Firebase write still needs a separate integration check.
     */
    @Test
    fun joinLotteryButton() {
        var clicked = false
        val now = Instant.now()
        val lottery = Lottery(
            id = "lottery-1", campsiteIds = listOf("1"),
            registrationStartAt = now.minusSeconds(60),
            registrationEndAt = now.plusSeconds(3600)
        )
        assertEquals(LotteryButtonState.JOIN, lotteryButtonState(lottery, null, 0, now))
        composeRule.setContent {
            CampsiteDetailsScreen(
                campsite = Campsite(id = "1", name = "Pine Lake"),
                onBack = {},
                lottery = lottery,
                lotteryButtonState = LotteryButtonState.JOIN,
                onLotteryAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Join lottery").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}
