package com.charge.reservation.reservation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log

import com.charge.reservation.repository.CampsiteRepository
import com.charge.reservation.model.Reservation
import com.charge.reservation.repository.ReservationRepository

/**
 * Displays the authenticated entrant's active, past and cancelled reservations.
 *
 * Loads reservations from Firestore and lets the entrant view details,
 * export CSV files and cancel upcoming reservations.
 */
@Composable
fun BookingsScreen(
    entrantId: String,
    onBackClick: () -> Unit
) {
    val repository = remember { ReservationRepository() }

    val campsiteRepository = remember { CampsiteRepository() }

    var campsiteNames by remember(entrantId) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

    var reservations by remember(entrantId) {
        mutableStateOf<List<Reservation>>(emptyList())
    }

    var loading by remember(entrantId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(entrantId) {
        mutableStateOf<String?>(null)
    }

    var selectedReservationId by remember(entrantId) {
        mutableStateOf<String?>(null)
    }

    var showingPastReservations by remember(entrantId) {
        mutableStateOf(false)
    }

    var showingCancelledReservations by remember(entrantId) {
        mutableStateOf(false)
    }

    var retryCount by remember(entrantId) {
        mutableIntStateOf(0)
    }

    // Listen for changes and remove the listener when disposed.
    DisposableEffect(entrantId, retryCount) {
        loading = true
        errorMessage = null

        // Resolve campsite names without blocking reservation loading.
        val campsiteListener = campsiteRepository.listenToActiveCampsites(
            onSuccess = { campsites ->
                campsiteNames = campsites.associate { campsite ->
                    campsite.id to campsite.name
                }
            },
            onFailure = { exception ->
                campsiteNames = emptyMap()
                Log.e(
                    "BookingsScreen",
                    "Unable to load campsite names.",
                    exception
                )
            }
        )

        val listener = repository.listenToEntrantReservations(
            entrantId = entrantId,
            onSuccess = { records ->
                reservations = records
                loading = false
                errorMessage = null
            },
            onFailure = { exception ->
                loading = false
                errorMessage = exception.message
                    ?: "Unable to load reservations."
            }
        )

        onDispose {
            listener.remove()
            campsiteListener.remove()
        }
    }

    BackHandler(
        enabled = selectedReservationId != null ||
                showingPastReservations ||
                showingCancelledReservations
    ) {
        if (selectedReservationId != null) {
            selectedReservationId = null
        } else {
            showingPastReservations = false
            showingCancelledReservations = false
        }
    }

    when {
        loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Loading reservations...")
            }
        }

        errorMessage != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Unable to load reservations.")
                Text(errorMessage ?: "")

                Button(onClick = { retryCount++ }) {
                    Text("Retry")
                }

                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
        }

        selectedReservationId != null -> {
            val selectedReservation = reservations.find {
                it.id == selectedReservationId &&
                        it.entrantId == entrantId
            }

            ReservationDetailsScreen(
                reservation = selectedReservation,
                campsiteName = selectedReservation?.let { reservation ->
                    campsiteNames[reservation.campsiteId]
                        ?: reservation.campsiteId
                } ?: "Unknown campsite",
                allowCsvExport = true,
                entrantId = entrantId,
                onCancelReservation = { reservationId, onSuccess, onFailure ->
                    repository.cancelReservation(
                        reservationId = reservationId,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                },
                onBackClick = {
                    selectedReservationId = null
                }
            )
        }

        showingCancelledReservations -> {
            PastReservationsScreen(
                entrantId = entrantId,
                reservations = reservations,
                campsiteNames = campsiteNames,
                onReservationClick = { reservationId ->
                    selectedReservationId = reservationId
                },
                showCancelled = true,
                onBackClick = {
                    showingCancelledReservations = false
                }
            )
        }

        showingPastReservations -> {
            PastReservationsScreen(
                entrantId = entrantId,
                reservations = reservations,
                campsiteNames = campsiteNames,
                onReservationClick = { reservationId ->
                    selectedReservationId = reservationId
                },
                onBackClick = {
                    showingPastReservations = false
                }
            )
        }

        else -> {
            ActiveReservationsScreen(
                entrantId = entrantId,
                reservations = reservations,
                campsiteNames = campsiteNames,
                onReservationClick = { reservationId ->
                    selectedReservationId = reservationId
                },
                onPastReservationsClick = {
                    showingPastReservations = true
                },
                onCancelledReservationsClick = {
                    showingCancelledReservations = true
                },
                onBackClick = onBackClick
            )
        }
    }
}
