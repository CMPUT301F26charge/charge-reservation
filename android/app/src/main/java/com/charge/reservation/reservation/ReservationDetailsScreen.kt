package com.charge.reservation.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Reservation

/**
 * Displays the saved details of a selected reservation.
 * Allows entrants to cancel their own upcoming reservations.
 */
@Composable
fun ReservationDetailsScreen(
    reservation: Reservation?,
    campsiteName: String,
    entrantId: String = "",
    onCancelReservation: ((String, () -> Unit, (Exception) -> Unit) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    var showConfirmation by remember(reservation?.id) {
        mutableStateOf(false)
    }

    var cancelling by remember(reservation?.id) {
        mutableStateOf(false)
    }

    var cancellationSuccessful by remember(reservation?.id) {
        mutableStateOf(false)
    }

    var cancellationMessage by remember(reservation?.id) {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBackClick) {
            Text("Back")
        }

        Text(
            text = "Reservation Details",
            fontSize = 28.sp
        )

        if (reservation == null) {
            Text("Reservation not found.")
            return@Column
        }

        HorizontalDivider()

        Text(
            text = campsiteName,
            fontSize = 22.sp
        )

        Text("Reservation ID: ${reservation.id}")
        Text("Status: ${reservation.status}")
        Text("Check-in: ${reservation.checkInDate ?: "Not specified"}")
        Text("Check-out: ${reservation.checkOutDate ?: "Not specified"}")
        Text("Guests: ${reservation.guestCount ?: "Not specified"}")

        HorizontalDivider()

        Text("Campsite ID: ${reservation.campsiteId}")
        Text("Lottery ID: ${reservation.lotteryId}")
        Text(
            "Reserved on: " +
                    (reservation.createdAt?.toString() ?: "Not specified")
        )

        if (onCancelReservation != null &&
            ReservationCancellation.canCancel(reservation, entrantId) &&
            !cancellationSuccessful
        ) {
            Button(
                onClick = { showConfirmation = true },
                enabled = !cancelling,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (cancelling) "Cancelling..."
                    else "Cancel Reservation"
                )
            }

            if (showConfirmation) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmation = false
                    },
                    title = {
                        Text("Cancel Reservation?")
                    },
                    text = {
                        Text(
                            "Are you sure you want to cancel this reservation? " +
                                    "This action cannot be undone."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmation = false
                                cancelling = true
                                cancellationMessage = null

                                onCancelReservation(
                                    reservation.id,
                                    {
                                        cancelling = false
                                        cancellationSuccessful = true
                                        cancellationMessage =
                                            "Reservation cancelled successfully."
                                    },
                                    {
                                        cancelling = false
                                        cancellationMessage =
                                            "Unable to cancel reservation."
                                    }
                                )
                            }
                        ) {
                            Text("Confirm Cancellation")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showConfirmation = false
                            }
                        ) {
                            Text("Keep Reservation")
                        }
                    }
                )
            }
        }

        cancellationMessage?.let {
            Text(it)
        }
    }
}