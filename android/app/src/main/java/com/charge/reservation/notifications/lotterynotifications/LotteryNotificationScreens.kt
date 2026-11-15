package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val Green = Color(0xFF41D49A)
private val Grey = Color(0xFFB5B5B5)
private val Red = Color(0xFFE5666E)
private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFF333B37)
private val WonCardBg = Color(0xFF10261C)
private val LostCardBg = Color(0xFF2A1618)
private val ScreenBg = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Lottery Won Notification
// ---------------------------------------------------------------------------

/**
 * Screen shown when the user taps a "lottery won" notification.
 * The default parameter values are sample data matching the design mockup;
 * pass real values from the Lottery / Notification objects.
 */
@Composable
fun LotteryWonNotificationScreen(
    lotteryName: String = "Pine Lake Autumn Lottery",
    lotteryCode: String = "LOT-204",
    bookingDeadline: String = "Oct 28, 2026",
    roleLabel: String = "",
    onDecline: () -> Unit = {},
    isDeclining: Boolean = false,
    declineError: Boolean = false,
    navController: NavController,
    lotteryId: String
) {
    var showDeclineDialog by remember { mutableStateOf(false) }

    LotteryNotificationScaffold(
        title = "Lottery Won Notification",
        roleLabel = roleLabel,
        buttonText = "Open winning invitation",
        onBack = {
            navController.popBackStack()
        },
        onPrimaryAction = {
            // TODO: navigate to winning invitation page
            navController.navigate("invitation/${lotteryId}")
        },
        secondaryButtonText = if (isDeclining) "Declining…" else "Decline invitation",
        onSecondaryAction = { if (!isDeclining) showDeclineDialog = true }
    ) {
        OutcomeCard(
            heading = "You won the lottery!",
            lines = listOf(
                "$lotteryName • $lotteryCode",
                "You were selected to book available dates."
            ),
            background = WonCardBg,
            borderColor = Green
        )

        Spacer(Modifier.height(10.dp))

        InfoCard(
            heading = "Booking invitation",
            lines = listOf(
                "Booking deadline: $bookingDeadline",
                "Tap to review the winning invitation."
            ),
            onClick = {
                // TODO: navigate to winning invitation page
                navController.navigate("invitation/${lotteryId}")
            }
        )

        if (declineError) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Couldn't decline the invitation. Please try again.",
                color = Red,
                fontSize = 12.sp
            )
        }
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Grey,
            title = { Text("Decline this invitation?", fontWeight = FontWeight.Bold) },
            text = {
                Text("You will give up your chance to book $lotteryName. This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeclineDialog = false
                        onDecline()
                    }
                ) {
                    Text("Decline", color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Keep invitation", color = Green)
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Lottery Lost Notification
// ---------------------------------------------------------------------------

/**
 * Screen shown when the user taps a "not selected" lottery notification.
 * The default parameter values are sample data matching the design mockup.
 */
@Composable
fun LotteryLostNotificationScreen(
    lotteryName: String = "Cedar Ridge Autumn Lottery",
    lotteryCode: String = "LOT-209",
    roleLabel: String = "ENTRANT • US 01.03.02",
    navController: NavController
) {
    LotteryNotificationScaffold(
        title = "Lottery Lost Notification",
        roleLabel = roleLabel,
        buttonText = "View lottery result",
        onBack = {
            navController.popBackStack()
        },
        onPrimaryAction = {
            // TODO: navigate to lottery result page
        }
    ) {
        OutcomeCard(
            heading = "Not selected this time",
            lines = listOf(
                "$lotteryName • $lotteryCode",
                "You were not drawn for this lottery."
            ),
            background = LostCardBg,
            borderColor = Red
        )

        Spacer(Modifier.height(10.dp))

        InfoCard(
            heading = "Result details",
            lines = listOf(
                "Your entered membership ended with this draw.",
                "Review other open campsite lotteries."
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Shared layout
// ---------------------------------------------------------------------------

@Composable
private fun LotteryNotificationScaffold(
    title: String,
    roleLabel: String,
    buttonText: String,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("‹", fontSize = 30.sp, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            roleLabel,
            color = Green,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Cards
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            content()
        }

        // Primary button
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Color.Black
            )
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }

        if (secondaryButtonText != null && onSecondaryAction != null) {
            OutlinedButton(
                onClick = onSecondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                    .height(48.dp),
                border = BorderStroke(1.dp, Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
            ) {
                Text(secondaryButtonText, fontWeight = FontWeight.Bold)
            }
        }

        EntrantBottomBar(selected = "Entries")
    }
}

@Composable
private fun OutcomeCard(
    heading: String,
    lines: List<String>,
    background: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(11.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(heading, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            lines.forEach { Text(it, color = Grey, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun InfoCard(
    heading: String,
    lines: List<String>,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(11.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(heading, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            lines.forEach { Text(it, color = Grey, fontSize = 12.sp) }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom navigation bar
// ---------------------------------------------------------------------------

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Explore", Icons.Outlined.Home),
    TabItem("Entries", Icons.Outlined.Diamond),
    TabItem("Bookings", Icons.Outlined.GridView),
    TabItem("Profile", Icons.Outlined.Person)
)

@Composable
private fun EntrantBottomBar(selected: String) {
    Column(modifier = Modifier.background(ScreenBg)) {
        HorizontalDivider(color = CardBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            tabs.forEach { tab ->
                val color = if (tab.label == selected) Green else Grey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // TODO: navigate to ${tab.label} page
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = color)
                    Text(tab.label, color = color, fontSize = 10.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Status message (shown when there is no won/lost screen for the entrant's status)
// ---------------------------------------------------------------------------

/**
 * Neutral screen for statuses that have no dedicated design, such as an
 * expired or already-used invitation, or a failed load.
 */
@Composable
fun LotteryStatusMessageScreen(
    heading: String,
    message: String,
    lotteryName: String? = null,
    lotteryCode: String? = null,
    roleLabel: String = "ENTRANT",
    navController: NavController
) {
    LotteryNotificationScaffold(
        title = "Lottery Notification",
        roleLabel = roleLabel,
        buttonText = "Back to notifications",
        onBack = {
            navController.popBackStack()
        },
        onPrimaryAction = {
            // TODO: navigate to notifications page
        }
    ) {
        InfoCard(
            heading = heading,
            lines = listOfNotNull(
                if (lotteryName != null && lotteryCode != null) "$lotteryName • $lotteryCode" else null,
                message
            )
        )
    }
}