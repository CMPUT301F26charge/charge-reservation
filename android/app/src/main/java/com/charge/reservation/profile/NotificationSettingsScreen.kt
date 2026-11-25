package com.charge.reservation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun NotificationSettingsScreen(
    notificationEnabled: Boolean,
    isSaving: Boolean,
    saveError: String,
    onNotificationEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Notification settings", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lottery and owner notifications",
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationEnabledChange,
                enabled = !isSaving,
                modifier = Modifier.semantics {
                    contentDescription = "Lottery and owner notifications"
                }
            )
        }

        Text("You can view lottery results in My lotteries.")

        if (isSaving) {
            Text("Saving preference…")
        }

        if (saveError.isNotEmpty()) {
            Text(saveError, color = MaterialTheme.colorScheme.error)
        }
    }
}