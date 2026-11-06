package com.charge.reservation.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Reservation

/**
 * Displays the saved details of a selected reservation.
 *
 * Outstanding: Connect this screen to the team's authenticated
 * reservation data source and shared navigation.
 */
@Composable
fun ReservationDetailsScreen(
    reservation: Reservation?,
    campsiteName: String,
    onBackClick: () -> Unit
) {
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
    }
}
