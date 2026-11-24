package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.charge.reservation.model.ParticipationStatus

/**
 * Opened when the user taps "View lottery" on a notification. Always shows a screen:
 *  - INVITED, deadline not passed -> won screen: pick a campsite (if the lottery has
 *                                    several) and book, or decline
 *  - NOT_SELECTED                 -> lost screen
 *  - USED                         -> booking confirmed, with the reservation's details
 *  - INVITED past deadline/EXPIRED -> "Booking deadline passed"
 *  - DECLINED/CANCELLED/WITHDRAWN/WAITING -> a matching message
 *  - missing data / load failure  -> a message with a way back or to retry
 *
 * @param lotteryId the notification's lotteryId
 * @param entrantId the signed-in user's id (UserProfile.id, the Firebase Auth UID)
 * @param onOpenInvitation opens the booking flow (InvitationBookingScreen) for a lottery,
 * with the campsite the winner picked, or null to let them choose there
 * @param onBack returns to the notification list
 */
@Composable
fun LotteryNotificationRoute(
    lotteryId: String,
    entrantId: String?,
    onOpenInvitation: (lotteryId: String, campsiteId: String?) -> Unit,
    onBack: () -> Unit,
    vm: LotteryNotificationViewModel = viewModel(key = "lottery-notification-$lotteryId")
) {
    // Load when shown, and refresh when the user comes back from booking.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentEntrantId by rememberUpdatedState(entrantId)
    DisposableEffect(lifecycleOwner, lotteryId) {
        val observer = LifecycleEventObserver { _, event ->
            val id = currentEntrantId
            if (event == Lifecycle.Event.ON_RESUME && id != null) {
                vm.onScreenResumed(lotteryId, id)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by vm.state.collectAsStateWithLifecycle()
    val declining by vm.declining.collectAsStateWithLifecycle()
    val declineError by vm.declineError.collectAsStateWithLifecycle()

    if (entrantId == null) {
        LotteryStatusMessageScreen(
            heading = "Not signed in",
            message = "Sign in to view this lottery notification.",
            onBack = onBack
        )
        return
    }

    when (val s = state) {
        LotteryNotificationState.Loading -> LotteryLoadingScreen()

        LotteryNotificationState.NotFound -> LotteryStatusMessageScreen(
            heading = "Lottery not found",
            message = "This lottery or your entry in it could not be found. It may have been removed.",
            onBack = onBack
        )

        LotteryNotificationState.Error -> LotteryStatusMessageScreen(
            heading = "Something went wrong",
            message = "We couldn't load this notification. Check your connection and try again.",
            onBack = onBack,
            primaryText = "Try again",
            onPrimaryAction = vm::retry
        )

        is LotteryNotificationState.Loaded -> LoadedLotteryNotification(
            data = s.data,
            isDeclining = declining,
            declineError = declineError,
            onOpenInvitation = { campsiteId -> onOpenInvitation(lotteryId, campsiteId) },
            onDecline = vm::declineInvitation,
            onBack = onBack
        )
    }
}

@Composable
private fun LoadedLotteryNotification(
    data: LotteryNotificationData,
    isDeclining: Boolean,
    declineError: String?,
    onOpenInvitation: (campsiteId: String?) -> Unit,
    onDecline: () -> Unit,
    onBack: () -> Unit
) {
    val deadlinePassed = data.deadlinePassed()

    when {
        data.status == ParticipationStatus.INVITED && !deadlinePassed ->
            LotteryWonNotificationScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                bookingDeadline = data.deadlineText,
                campsites = data.campsites,
                onOpenInvitation = onOpenInvitation,
                onDecline = onDecline,
                isDeclining = isDeclining,
                declineError = declineError
            )

        data.status == ParticipationStatus.NOT_SELECTED ->
            LotteryLostNotificationScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                onBack = onBack
            )

        data.status == ParticipationStatus.USED ->
            LotteryBookingConfirmedScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                campsiteName = data.reservationCampsiteName,
                reservation = data.reservation,
                onBack = onBack
            )

        else -> {
            val (heading, message) = data.unavailableText(deadlinePassed)
            LotteryStatusMessageScreen(
                lotteryName = data.lotteryName,
                lotteryCode = data.lotteryCode,
                heading = heading,
                message = message,
                onBack = onBack
            )
        }
    }
}

private fun LotteryNotificationData.unavailableText(deadlinePassed: Boolean): Pair<String, String> =
    when {
        status == ParticipationStatus.EXPIRED ||
            (status == ParticipationStatus.INVITED && deadlinePassed) ->
            "Booking deadline passed" to
                "The booking deadline for this lottery ($deadlineText) has passed, so this invitation has expired."

        status == ParticipationStatus.DECLINED ->
            "Invitation declined" to
                "You declined the invitation for this lottery and can no longer book with it."

        status == ParticipationStatus.CANCELLED ->
            "Participation cancelled" to "Your entry or invitation for this lottery was cancelled."

        status == ParticipationStatus.WITHDRAWN ->
            "Entry withdrawn" to "You left this lottery before the draw."

        else -> // WAITING
            "Awaiting the draw" to "Results for this lottery haven't been announced yet."
    }
