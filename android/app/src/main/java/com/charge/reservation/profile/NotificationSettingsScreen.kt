
package com.charge.reservation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag


/**
 * US 01.10.01: Allows entrants to opt in or out of notifications.
 *
 * The current preference is supplied from the user's Firestore profile.
 * Changes are only reflected after a successful save.
 *
 * @param notificationEnabled Current saved preference.
 * @param isSaving Whether a save operation is active.
 * @param errorMessage Any save error to display.
 * @param onSave Called when the user changes their preference.
 */
@Composable
fun NotificationSettingsScreen(
    notificationEnabled: Boolean,
    isSaving: Boolean,
    errorMessage: String,
    onSave: (Boolean) -> Unit
) {
    var requestedValue by remember(notificationEnabled) {
        mutableStateOf(notificationEnabled)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Notification Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Receive lottery results and campsite owner alerts."
        )

        Switch(
            checked = requestedValue,
            enabled = !isSaving,
            onCheckedChange = { requestedValue = it },
            modifier = Modifier.testTag("notificationToggle")
        )

        Text(
            text = if (requestedValue) {
                "Notifications enabled"
            } else {
                "Notifications disabled"
            }
        )

        Text(
            text = "Turning notifications back on will only " +
                    "affect future alerts.",
            style = MaterialTheme.typography.bodySmall
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { onSave(requestedValue) },
            enabled = !isSaving &&
                    requestedValue != notificationEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator()
            } else {
                Text("Save Preference")
            }
        }
    }
}
