package com.charge.reservation.notifications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.charge.reservation.model.Notification
import com.charge.reservation.model.NotificationType

private val Green = Color(0xFF41D49A)
private val Grey = Color(0xFFB5B5B5)
private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFF333B37)
private val ScreenBg = Color(0xFFFFFFFF)

private enum class BottomTab(val label: String, val icon: ImageVector) {
    EXPLORE("Explore", Icons.Outlined.Home),
    ENTRIES("Entries", Icons.Outlined.Diamond),
    BOOKINGS("Bookings", Icons.Outlined.GridView),
    PROFILE("Profile", Icons.Outlined.Person)
}

/**
 * The notification list page.
 *
 * @param notifications the notifications to show, newest first
 * @param onOpenLottery called with the lottery id when a lottery notification's
 * "View lottery" button is tapped
 */
@Composable
fun NotificationsScreen(
    notifications: List<Notification>,
    onOpenLottery: (String) -> Unit,
    roleLabel: String = "",
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
    ) {
        // Top bar


        Text(
            roleLabel,
            color = Green,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Notification list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item { Text("No notifications yet", color = Grey) }
            }
            items(notifications, key = { it.id }) { n ->
                NotificationCard(n, onOpenLottery)
            }
        }

    }
}

@Composable
private fun NotificationCard(
    n: Notification,
    onOpenLottery: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(11.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                n.displayTitle(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            // One line of text per line break in the content
            n.content.split("\n").forEach { line ->
                Text(line, color = Grey, fontSize = 13.sp)
            }

            // Only lottery notifications get a button
            val lotteryId = n.lotteryId
            if (lotteryId != null) {
                TextButton(
                    onClick = { onOpenLottery(lotteryId) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View lottery →", color = Green, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: BottomTab) {
    Column(modifier = Modifier.background(ScreenBg)) {
        HorizontalDivider(color = CardBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            BottomTab.entries.forEach { tab ->
                val color = if (tab == selected) Green else Grey
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

private fun Notification.displayTitle(): String = when {
    type == NotificationType.OWNER_CUSTOM -> "From campsite owner"
    lotteryId != null -> "Lottery update"
    else -> "Notification"
}