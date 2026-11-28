package com.charge.reservation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.charge.reservation.model.Notification
import com.charge.reservation.model.NotificationType

/**
 * The notification list page. It sits inside AppNavigation's Scaffold, which
 * provides the top and bottom bars.
 *
 * @param notifications the notifications to show, newest first
 * @param onOpenLottery called with the lottery id when a lottery notification's
 * "View lottery" button is tapped
 * @param onSentNotificationsClick opens the owner's sent notifications, or null
 * to hide the button for users not acting as an owner
 */
@Composable
fun NotificationsScreen(
    notifications: List<Notification>,
    onOpenLottery: (String) -> Unit,
    roleLabel: String = "",
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    onSentNotificationsClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (roleLabel.isNotBlank()) {
            Text(
                roleLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (onSentNotificationsClick != null) {
            OutlinedButton(
                onClick = onSentNotificationsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                Text("Sent notifications")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Text(
                        "No notifications yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                n.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // One line of text per line break in the content
            n.content.split("\n").forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Every lottery notification gets a button; the next screen shows the
            // right result (won, lost, deadline passed, booked, ...).
            val lotteryId = n.lotteryId
            if (!lotteryId.isNullOrBlank()) {
                TextButton(
                    onClick = { onOpenLottery(lotteryId) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View lottery →")
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
