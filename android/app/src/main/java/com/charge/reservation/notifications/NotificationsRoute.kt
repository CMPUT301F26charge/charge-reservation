package com.charge.reservation.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Navigation entry point for the notification list: loads the user's
 * notifications and hands them to the screen.
 *
 * @param userId the signed-in user's id (UserProfile.id, the Firebase Auth UID)
 * @param onOpenLottery called with the lottery id when a lottery notification's
 * "View lottery" button is tapped
 */
@Composable
fun NotificationsRoute(
    userId: String?,
    onOpenLottery: (String) -> Unit,
    navController: NavController,
    vm: NotificationsViewModel = viewModel()
) {
    LaunchedEffect(userId) { vm.setUser(userId) }
    val notifications by vm.notifications.collectAsStateWithLifecycle()

    NotificationsScreen(
        notifications = notifications,
        onOpenLottery = onOpenLottery,
        navController = navController
    )
}