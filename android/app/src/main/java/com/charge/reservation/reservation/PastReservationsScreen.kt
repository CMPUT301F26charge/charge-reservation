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
import com.charge.reservation.model.ReservationStatus
import java.time.LocalDate

/**
 * Displays the entrant's completed or cancelled reservations and allows
 * them to open the details of a previous reservation.
 */
@Composable
fun PastReservationsScreen(
    entrantId: String,
    reservations: List<Reservation>,
    campsiteNames: Map<String, String>,
    onReservationClick: (String) -> Unit,
    today: LocalDate = LocalDate.now(),
    showCancelled: Boolean = false,
    onBackClick: () -> Unit
) {
    val pastReservations = if (showCancelled) {
        reservations.filter {
            it.entrantId == entrantId &&
                    it.status == ReservationStatus.CANCELLED
        }
    } else {
        PastReservationFilter.filter(
            reservations = reservations,
            entrantId = entrantId,
            today = today
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBackClick) {
            Text("Back")
        }

        Text(
            text = if (showCancelled) "My Cancelled Reservations"
                   else "My Past Reservations",
            fontSize = 26.sp
        )

        if (pastReservations.isEmpty()) {
            Text(
                if (showCancelled) "You have no cancelled reservations."
                else "You have no past reservations."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = pastReservations,
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

                            if (showCancelled) {
                                Text("Status: ${reservation.status}")
                            }
                        }
                    }
                }
            }
        }
    }
}
