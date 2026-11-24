package com.charge.reservation.notifications.lotterynotifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

// These screens sit inside AppNavigation's Scaffold, which already provides the
// top bar (with back button) and bottom bar, so they don't draw their own.
// Colours come from MaterialTheme to match the rest of the app.

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

// ---------------------------------------------------------------------------
// Lottery Won Notification
// ---------------------------------------------------------------------------

/**
 * Shown when the entrant is INVITED and the booking deadline hasn't passed.
 *
 * If the lottery has several campsites, each is listed so the winner can pick
 * the one to book at; [onOpenInvitation] receives the picked campsite's ID.
 * With one campsite (or from the main button) it receives that campsite's ID or null.
 */
@Composable
fun LotteryWonNotificationScreen(
    lotteryName: String,
    lotteryCode: String,
    bookingDeadline: String,
    campsites: List<CampsiteOption>,
    onOpenInvitation: (campsiteId: String?) -> Unit,
    onDecline: () -> Unit,
    isDeclining: Boolean,
    declineError: String?
) {
    var showDeclineDialog by remember { mutableStateOf(false) }

    val multipleCampsites = campsites.size > 1

    LotteryNotificationLayout(
        title = "Lottery Won",
        primaryText = "Open invitation & book",
        onPrimaryAction = { onOpenInvitation(campsites.singleOrNull()?.id) },
        primaryEnabled = !isDeclining,
        secondaryText = if (isDeclining) "Declining…" else "Decline invitation",
        onSecondaryAction = { if (!isDeclining) showDeclineDialog = true }
    ) {
        OutcomeCard(
            heading = "You won the lottery!",
            lines = listOf(
                "$lotteryName • $lotteryCode",
                "You were selected to book available dates."
            ),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )

        if (multipleCampsites) {
            InfoCard(
                heading = "Booking invitation",
                lines = listOf(
                    "Book by: $bookingDeadline",
                    "Your invitation can be used at one of ${campsites.size} campsites. " +
                        "Pick the one you want to book."
                )
            )

            Text("Choose a campsite", style = MaterialTheme.typography.titleMedium)

            campsites.forEach { campsite ->
                InfoCard(
                    heading = campsite.name,
                    lines = listOf("Tap to see available dates and book here."),
                    onClick = if (isDeclining) null else ({ onOpenInvitation(campsite.id) })
                )
            }
        } else {
            InfoCard(
                heading = "Booking invitation",
                lines = listOfNotNull(
                    campsites.singleOrNull()?.let { "Campsite: ${it.name}" },
                    "Book by: $bookingDeadline",
                    "Tap to see available dates and book your stay."
                ),
                onClick = if (isDeclining) null else ({ onOpenInvitation(campsites.singleOrNull()?.id) })
            )
        }

        if (declineError != null) {
            Text(
                declineError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline this invitation?") },
            text = {
                Text(
                    "You will give up your chance to book $lotteryName and won't be able " +
                        "to book with this invitation. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeclineDialog = false
                        onDecline()
                    }
                ) {
                    Text(
                        "Decline",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Keep invitation")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Lottery Lost Notification
// ---------------------------------------------------------------------------

/** Shown when the entrant is NOT_SELECTED. */
@Composable
fun LotteryLostNotificationScreen(
    lotteryName: String,
    lotteryCode: String,
    onBack: () -> Unit
) {
    LotteryNotificationLayout(
        title = "Lottery Result",
        primaryText = "Back to notifications",
        onPrimaryAction = onBack
    ) {
        OutcomeCard(
            heading = "Not selected this time",
            lines = listOf(
                "$lotteryName • $lotteryCode",
                "You were not drawn for this lottery."
            ),
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )

        InfoCard(
            heading = "Result details",
            lines = listOf(
                "Your entry ended with this draw.",
                "Explore other open campsite lotteries."
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Booking confirmed / invitation used
// ---------------------------------------------------------------------------

/** Shown when the invitation is USED, with the reservation's details if available. */
@Composable
fun LotteryBookingConfirmedScreen(
    lotteryName: String,
    lotteryCode: String,
    campsiteName: String?,
    reservation: Reservation?,
    onBack: () -> Unit
) {
    val cancelledOrRejected = reservation?.status == ReservationStatus.CANCELLED ||
        reservation?.status == ReservationStatus.REJECTED
    val heading = when (reservation?.status) {
        ReservationStatus.CANCELLED -> "Reservation cancelled"
        ReservationStatus.REJECTED -> "Reservation rejected"
        else -> "You're booked!"
    }

    LotteryNotificationLayout(
        title = "Invitation Used",
        primaryText = "Back to notifications",
        onPrimaryAction = onBack
    ) {
        OutcomeCard(
            heading = heading,
            lines = listOf(
                "$lotteryName • $lotteryCode",
                "Your winning invitation was used for this reservation."
            ),
            container = if (cancelledOrRejected) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            content = if (cancelledOrRejected) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )

        InfoCard(
            heading = "Reservation details",
            lines = if (reservation == null) {
                listOf("You already booked a reservation with this invitation.")
            } else {
                listOfNotNull(
                    campsiteName?.let { "Campsite: $it" },
                    reservation.checkInDate?.let { "Check-in: ${it.format(dateFormatter)}" },
                    reservation.checkOutDate?.let { "Check-out: ${it.format(dateFormatter)}" },
                    reservation.guestCount?.let { "Guests: $it" },
                    reservation.statusChangeReason
                        ?.takeIf { cancelledOrRejected }
                        ?.let { "Reason: $it" },
                    "Reservation ID: ${reservation.id}"
                )
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Status message (deadline passed, declined, cancelled, withdrawn, waiting, errors)
// ---------------------------------------------------------------------------

@Composable
fun LotteryStatusMessageScreen(
    heading: String,
    message: String,
    onBack: () -> Unit,
    lotteryName: String? = null,
    lotteryCode: String? = null,
    primaryText: String = "Back to notifications",
    onPrimaryAction: () -> Unit = onBack
) {
    LotteryNotificationLayout(
        title = "Lottery Notification",
        primaryText = primaryText,
        onPrimaryAction = onPrimaryAction
    ) {
        InfoCard(
            heading = heading,
            lines = listOfNotNull(
                if (lotteryName != null && lotteryCode != null) "$lotteryName • $lotteryCode" else null,
                message
            )
        )
    }
}

@Composable
fun LotteryLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// Shared layout
// ---------------------------------------------------------------------------

@Composable
private fun LotteryNotificationLayout(
    title: String,
    primaryText: String,
    onPrimaryAction: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onPrimaryAction,
            enabled = primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(primaryText)
        }

        if (secondaryText != null && onSecondaryAction != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSecondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(secondaryText)
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    heading: String,
    lines: List<String>,
    container: Color,
    content: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun InfoCard(
    heading: String,
    lines: List<String>,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
