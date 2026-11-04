package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.charge.reservation.model.ParticipationStatus
import java.time.Instant

/**
 * Opened when the user taps a lottery notification. Loads the entrant's
 * participation and shows the matching screen:
 *  - INVITED (deadline not passed)  -> won screen (with a decline button)
 *  - NOT_SELECTED                   -> lost screen
 *  - anything else / load problems  -> a status message screen
 *
 * @param lotteryId the notification's lotteryId
 * @param entrantId the signed-in user's id (UserProfile.id, the Firebase Auth UID)
 */
@Composable
fun LotteryNotificationRoute(
    lotteryId: String,
    entrantId: String?,
    vm: LotteryNotificationViewModel = viewModel(),
    navController: NavController
) {
    LaunchedEffect(lotteryId, entrantId) {
        if (entrantId != null) vm.load(lotteryId, entrantId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val declining by vm.declining.collectAsStateWithLifecycle()
    val declineError by vm.declineError.collectAsStateWithLifecycle()

    if (entrantId == null) {
        LotteryStatusMessageScreen(
            heading = "Not signed in",
            message = "Sign in to view this lottery notification.",
            navController = navController

        )
        return
    }

    when (val s = state) {
        LotteryNotificationState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF41D49A))
        }

        LotteryNotificationState.NotFound -> LotteryStatusMessageScreen(
            heading = "Lottery not found",
            message = "This lottery or your entry could not be found.",
            navController = navController
        )

        LotteryNotificationState.Error -> LotteryStatusMessageScreen(
            heading = "Something went wrong",
            message = "We couldn't load this notification. Please try again.",
            navController = navController
        )

        is LotteryNotificationState.Loaded -> LoadedLotteryNotification(
            data = s.data,
            isDeclining = declining,
            declineError = declineError,
            onDecline = { vm.declineInvitation(lotteryId, entrantId, navController) },
            navController
        )
    }
}

@Composable
private fun LoadedLotteryNotification(
    data: LotteryNotificationData,
    isDeclining: Boolean,
    declineError: Boolean,
    onDecline: () -> Unit,
    navController: NavController
) {
    val deadlinePassed = data.bookingDeadline?.let { Instant.now().isAfter(it) } ?: false

    when {
        data.status == ParticipationStatus.INVITED && !deadlinePassed ->
            LotteryWonNotificationScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                bookingDeadline = data.deadlineText,
                onDecline = onDecline,
                isDeclining = isDeclining,
                declineError = declineError,
                navController = navController
            )

        data.status == ParticipationStatus.NOT_SELECTED ->
            LotteryLostNotificationScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                navController = navController
            )

        else -> {
            val (heading, message) = data.unavailableText(deadlinePassed)
            LotteryStatusMessageScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                heading = heading,
                message = message,
                navController = navController
            )
        }
    }
}

private fun LotteryNotificationData.unavailableText(deadlinePassed: Boolean): Pair<String, String> =
    when {
        status == ParticipationStatus.EXPIRED ||
                (status == ParticipationStatus.INVITED && deadlinePassed) ->
            "Invitation expired" to "The booking deadline for this lottery has passed."

        status == ParticipationStatus.USED ->
            "Invitation used" to "You already booked a reservation with this invitation."

        status == ParticipationStatus.DECLINED ->
            "Invitation declined" to "You declined the invitation for this lottery."

        status == ParticipationStatus.CANCELLED ->
            "Participation cancelled" to "Your entry or invitation for this lottery was cancelled."

        status == ParticipationStatus.WITHDRAWN ->
            "Entry withdrawn" to "You left this lottery before the draw."

        else -> // WAITING
            "Awaiting the draw" to "Results for this lottery haven't been announced yet."
    }