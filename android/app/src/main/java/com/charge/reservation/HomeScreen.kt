package com.charge.reservation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.charge.reservation.model.UserProfile

/**
 * Displays the home screen.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    profile: UserProfile
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (profile.isOwner && !profile.removed) {
            Button(
                onClick = {
                    navController.navigate("ownerCampsites")
                }
            ) {
                Text("My Campsites")
            }
        }
    }
}