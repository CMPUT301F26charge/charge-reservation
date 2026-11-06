package com.charge.reservation.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus

/**
 * Represents the outcome of a reservation booking operation.
 */
sealed interface BookingResult {
    data class Success(
        val reservation: Reservation,
        val campsiteName: String
    ) : BookingResult

    data class Failure(
        val message: String
    ) : BookingResult
}

/**
 * Displays a successful reservation or a booking failure.
 *
 * Outstanding: Connect this screen to the team's reservation
 * creation flow and the active reservations data source.
 */
@Composable
fun ReservationConfirmationScreen(
    result: BookingResult,
    onDoneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (result) {
            is BookingResult.Success -> {
                val reservation = result.reservation

                if (
                    reservation.id.isBlank() ||
                    reservation.status != ReservationStatus.CONFIRMED ||
                    reservation.checkInDate == null ||
                    reservation.checkOutDate == null
                ) {
                    Text(
                        text = "Reservation could not be confirmed.",
                        fontSize = 22.sp
                    )
                } else {
                    Text(
                        text = "Reservation Confirmed!",
                        fontSize = 28.sp
                    )

                    Text("Reservation ID: ${reservation.id}")
                    Text("Campsite: ${result.campsiteName}")
                    Text("Check-in: ${reservation.checkInDate}")
                    Text("Check-out: ${reservation.checkOutDate}")
                    Text("Status: ${reservation.status}")
                }
            }

            is BookingResult.Failure -> {
                Text(
                    text = "Reservation Failed",
                    fontSize = 28.sp
                )

                Text(result.message)
            }
        }

        Button(
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}
