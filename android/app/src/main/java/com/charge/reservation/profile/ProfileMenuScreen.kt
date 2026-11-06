package com.charge.reservation.profile

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
fun ProfileMenuScreen(
    onPersonalInfoClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onPoliciesClick: () -> Unit
) {
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
    }
}