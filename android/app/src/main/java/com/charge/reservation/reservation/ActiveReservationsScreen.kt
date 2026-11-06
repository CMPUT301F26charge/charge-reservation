
package com.charge.reservation.reservation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Reservation
import java.time.LocalDate

/**
 * Displays the entrant's confirmed upcoming and ongoing reservations.
 *
 * Cancelled, rejected, completed, and other entrants'
 * reservations are excluded.
 *
 * Outstanding: Connect to authenticated reservation data.
 */
@Composable
fun ActiveReservationsScreen(
    entrantId: String,
    reservations: List<Reservation>,
    campsiteNames: Map<String, String>,
    onReservationClick: (String) -> Unit,
    today: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit
) {
    val activeReservations = ActiveReservationFilter.filter(
        reservations = reservations,
        entrantId = entrantId,
        today = today
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ADDED: Back navigation
        Button(onClick = onBackClick) {
            Text("Back")
        }

        Text(
            text = "My Active Reservations",
            fontSize = 26.sp
        )

        if (activeReservations.isEmpty()) {
            Text("You have no active reservations.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = activeReservations,
                    key = { it.id }
                ) { reservation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onReservationClick(reservation.id)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = campsiteNames[
                                    reservation.campsiteId
                                ] ?: reservation.campsiteId,
                                fontSize = 20.sp
                            )

                            Text("Reservation: ${reservation.id}")
                            Text("Check-in: ${reservation.checkInDate}")
                            Text("Check-out: ${reservation.checkOutDate}")
                            Text("Status: ${reservation.status}")
                        }
                    }
                }
            }
        }
    }
}
