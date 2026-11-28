package com.charge.reservation.qr

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charge.reservation.model.Campsite
import com.charge.reservation.ui.theme.ChargeReservationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the owner's button displays a generated QR image for the listing. */
@RunWith(AndroidJUnit4::class)
class CampsiteQrButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showQrCodeDisplaysImageAndCloseDismissesIt() {
        composeRule.setContent {
            ChargeReservationTheme {
                CampsiteQrButton(Campsite(id = "pine-lake", name = "Pine Lake"))
            }
        }

        val imageDescription = "Promotional QR code for Pine Lake"
        composeRule.onNodeWithText("Show QR code").performClick()
        composeRule.onNodeWithContentDescription(imageDescription).assertExists()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithContentDescription(imageDescription).assertDoesNotExist()
    }
}
