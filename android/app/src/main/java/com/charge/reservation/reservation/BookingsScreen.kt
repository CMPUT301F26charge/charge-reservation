
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
 * Displays the authenticated entrant's active reservations.
 *
 * Loads reservations from Firestore and lets the entrant
 * open the details of a selected reservation.
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

    // Handle the device Back button while viewing details.
    BackHandler(enabled = selectedReservationId != null) {
        selectedReservationId = null
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
                onBackClick = {
                    selectedReservationId = null
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
                onBackClick = onBackClick
            )
        }
    }
}
