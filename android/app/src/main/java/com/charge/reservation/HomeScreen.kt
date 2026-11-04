package com.charge.reservation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.charge.reservation.model.UserProfile

/**
 * Displays the home screen.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    profile: UserProfile
) {
    if (profile.isOwner && !profile.removed) {
        Button(
            onClick = { navController.navigate("createCampsite") }
        ) {
            Text("Register a campsite")
        }
    }
    
}