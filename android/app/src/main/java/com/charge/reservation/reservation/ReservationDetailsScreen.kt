package com.charge.reservation.reservation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.io.IOException

/**
 * Displays the saved details of a selected reservation.
 * Supports CSV export, cancellation of upcoming reservations,
 * and downloading cancellation confirmations.
 */
@Composable
fun ReservationDetailsScreen(
    reservation: Reservation?,
    campsiteName: String,
    allowCsvExport: Boolean = false,
    entrantId: String = "",
    onCancelReservation: ((String, () -> Unit, (Exception) -> Unit) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var exportMessage by rememberSaveable(reservation?.id) {
        mutableStateOf<String?>(null)
    }

    var pendingCsv by rememberSaveable(reservation?.id) {
        mutableStateOf<String?>(null)
    }

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

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = pendingCsv
                    ?: throw IOException("Missing reservation data.")

                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Unable to open file.")

                stream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(csv)
                }

                exportMessage = "Reservation CSV saved successfully."

            } catch (exception: Exception) {
                exportMessage = "Unable to save reservation CSV."
            }
        }

        pendingCsv = null
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

        // Reservation CSV export
        if (allowCsvExport) {
            Button(
                onClick = {
                    exportMessage = null

                    pendingCsv = ReservationCsv.generate(
                        reservation, campsiteName
                    )

                    try {
                        csvLauncher.launch(
                            "reservation-${reservation.id}.csv"
                        )
                    } catch (exception: Exception) {
                        pendingCsv = null
                        exportMessage = "Unable to open file picker."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download CSV")
            }

            if (reservation.status == ReservationStatus.CANCELLED &&
                reservation.statusChangedAt != null
            ) {
                Text("Cancelled on: ${reservation.statusChangedAt}")

                Button(
                    onClick = {
                        exportMessage = null

                        try {
                            pendingCsv = ReservationCsv.generateCancellation(
                                reservation, campsiteName
                            )

                            csvLauncher.launch(
                                "cancellation-${reservation.id}.csv"
                            )
                        } catch (exception: Exception) {
                            pendingCsv = null
                            exportMessage =
                                "Unable to download cancellation confirmation."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download Cancellation Confirmation")
                }
            }

            exportMessage?.let {
                Text(it)
            }
        }

        // Reservation cancellation
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
