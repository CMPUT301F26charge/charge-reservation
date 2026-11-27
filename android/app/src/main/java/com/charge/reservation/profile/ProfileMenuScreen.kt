package com.charge.reservation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shows the profile menu.
 *
 * @param isDeletingProfile whether the profile deletion is running
 * @param deleteProfileError error returned by the repository, or empty
 * @param onDeleteProfileConfirmed called after the user confirms deletion
 */
@Composable
fun ProfileMenuScreen(
    onPersonalInfoClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onPoliciesClick: () -> Unit,
    onNotificationSettingsClick: (() -> Unit)? = null,
    isDeletingProfile: Boolean = false,
    deleteProfileError: String = "",
    onDeleteProfileConfirmed: () -> Unit = {}
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 28.sp
        )

        OutlinedButton(
            onClick = onPersonalInfoClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Personal information")
        }

        OutlinedButton(
            onClick = onNotificationsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Notifications")
        }

        if (onNotificationSettingsClick != null) {
            OutlinedButton(
                onClick = onNotificationSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Notification settings")
            }
        }

        OutlinedButton(
            onClick = onMessagesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Messages")
        }

        OutlinedButton(
            onClick = onPoliciesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Policies")
        }

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            enabled = !isDeletingProfile,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isDeletingProfile) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("Delete profile")
            }
        }

        if (deleteProfileError.isNotEmpty()) {
            Text(
                text = deleteProfileError,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "This permanently deletes your profile. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteProfileConfirmed()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
