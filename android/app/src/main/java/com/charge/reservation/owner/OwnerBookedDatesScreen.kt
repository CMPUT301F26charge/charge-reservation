package com.charge.reservation.owner

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.charge.reservation.model.Reservation
import com.charge.reservation.repository.ReservationRepository
import java.time.LocalDate

@Composable
fun OwnerBookedDatesScreen(
    campsiteId: String,
    campsiteName: String
) {
    val repository = remember { ReservationRepository() }
    var reservations by remember(campsiteId) {
        mutableStateOf<List<Reservation>>(emptyList())
    }
    var isLoading by remember(campsiteId) { mutableStateOf(true) }
    var loadFailed by remember(campsiteId) { mutableStateOf(false) }
    var retryCount by remember(campsiteId) { mutableIntStateOf(0) }

    DisposableEffect(campsiteId, retryCount) {
        isLoading = true
        loadFailed = false

        val listener = repository.listenToConfirmedCampsiteReservations(
            campsiteId = campsiteId,
            onSuccess = { updatedReservations ->
                reservations = updatedReservations
                isLoading = false
                loadFailed = false
            },
            onFailure = { exception ->
                Log.e("OwnerBookedDates", "Could not load booked dates", exception)
                isLoading = false
                loadFailed = true
            }
        )

        onDispose { listener.remove() }
    }

    val today = LocalDate.now()
    val bookedStays = reservations.filter { reservation ->
        val checkIn = reservation.checkInDate ?: return@filter false
        val checkOut = reservation.checkOutDate ?: return@filter false
        checkOut.isAfter(checkIn) && checkOut.isAfter(today)
    }.sortedBy { it.checkInDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Booked dates", style = MaterialTheme.typography.headlineSmall)
        Text(campsiteName, style = MaterialTheme.typography.titleMedium)

        when {
            isLoading -> CircularProgressIndicator()

            loadFailed -> {
                Text("Could not load booked dates.")
                Button(onClick = { retryCount++ }) {
                    Text("Retry")
                }
            }

            bookedStays.isEmpty() -> Text("No upcoming or ongoing bookings.")

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookedStays, key = { it.id }) { reservation ->
                    val checkIn = reservation.checkInDate ?: return@items
                    val checkOut = reservation.checkOutDate ?: return@items
                    val firstBookedNight = maxOf(checkIn, today)
                    val lastBookedNight = checkOut.minusDays(1)
                    val bookedNights = if (firstBookedNight == lastBookedNight) {
                        firstBookedNight.toString()
                    } else {
                        "$firstBookedNight to $lastBookedNight"
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Booked nights: $bookedNights",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Reservation ID: ${reservation.id}")
                            Text("Check-in: $checkIn")
                            Text("Check-out: $checkOut")
                        }
                    }
                }
            }
        }
    }
}