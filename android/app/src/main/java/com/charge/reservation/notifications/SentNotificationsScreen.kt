package com.charge.reservation.notifications

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.catch

import com.charge.reservation.model.Campsite

private const val TAG = "SentNotifications"

/**
 * Lists the announcements an owner has sent, newest first, with a button
 * at the bottom to write a new one.
 *
 * @param ownerId the signed-in owner's Firebase UID
 * @param campsites the owner's campsites, used to show campsite names
 * @param onCreateNotificationClick opens the notification form
 */
@Composable
fun SentNotificationsScreen(
    ownerId: String,
    campsites: List<Campsite>,
    onCreateNotificationClick: () -> Unit,
    repository: OwnerNotificationRepository = remember { OwnerNotificationRepository() }
) {
    var announcements by remember(ownerId) {
        mutableStateOf<List<SentAnnouncement>?>(null)
    }
    var errorMessage by remember(ownerId) { mutableStateOf("") }

    LaunchedEffect(ownerId) {
        repository.sentAnnouncements(ownerId)
            .catch { exception ->
                Log.e(TAG, "Could not load sent notifications", exception)
                errorMessage = "Could not load your sent notifications. Please try again."
            }
            .collect { loaded ->
                announcements = loaded
                errorMessage = ""
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Sent notifications",
            style = MaterialTheme.typography.headlineSmall
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val loaded = announcements

            when {
                errorMessage.isNotEmpty() -> Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                loaded == null -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                loaded.isEmpty() -> Text(
                    text = "You haven't sent any notifications yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(loaded, key = { it.id }) { announcement ->
                        SentAnnouncementCard(
                            announcement = announcement,
                            campsiteName = campsites
                                .firstOrNull { it.id == announcement.campsiteId }
                                ?.name
                                ?: "Removed campsite"
                        )
                    }
                }
            }
        }

        Button(
            onClick = onCreateNotificationClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create notification")
        }
    }
}

@Composable
private fun SentAnnouncementCard(
    announcement: SentAnnouncement,
    campsiteName: String
) {
    val dateFormat = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campsiteName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )

                announcement.createdAt?.let { sentAt ->
                    Text(
                        text = dateFormat.format(sentAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = if (announcement.toReservationGuests) {
                    "To guests with upcoming reservations"
                } else {
                    "To lottery entrants"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium
            )

            val people = if (announcement.sentCount == 1) "person" else "people"
            Text(
                text = "Delivered to ${announcement.sentCount} $people" +
                    if (announcement.skippedCount > 0) {
                        " · ${announcement.skippedCount} skipped (notifications off)"
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
