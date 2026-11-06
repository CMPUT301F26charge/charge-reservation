package com.charge.reservation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminMenuScreen(
    onProfilesClick: () -> Unit,
    onImagesClick: () -> Unit,
    onNotificationLogsClick: () -> Unit,
    onMessageLogsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Admin",
            fontSize = 28.sp
        )

        OutlinedButton(
            onClick = onProfilesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse profiles")
        }

        OutlinedButton(
            onClick = onImagesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse images")
        }

        OutlinedButton(
            onClick = onNotificationLogsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Notification logs")
        }

        OutlinedButton(
            onClick = onMessageLogsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Message logs")
        }
    }
}