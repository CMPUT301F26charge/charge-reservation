
package com.charge.reservation.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.booking.RulesAcknowledgementScreen
import com.charge.reservation.model.Campsite
import com.charge.reservation.repository.ReservationBookingRepository
import java.time.LocalDate

/**
 * Connects rules acknowledgement to reservation creation and confirmation.
 *
 * The caller must provide the campsite, lottery, and selected booking dates.
 */
@Composable
fun ReservationBookingFlowScreen(
    campsite: Campsite,
    lotteryId: String,
    checkIn: LocalDate,
    checkOut: LocalDate,
    guestCount: Int,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val repository = remember { ReservationBookingRepository() }

    key(campsite.id, lotteryId, checkIn, checkOut, guestCount) {
        var submitting by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<BookingResult?>(null) }

        when {
            result != null -> {
                ReservationConfirmationScreen(
                    result = requireNotNull(result),
                    onDoneClick = onDoneClick
                )
            }

            submitting -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Creating reservation...")
                }
            }

            else -> {
                RulesAcknowledgementScreen(
                    campsite = campsite,
                    bookingId = "$lotteryId:$checkIn:$checkOut:$guestCount",
                    onConfirmClick = {
                        if (!submitting) {
                            submitting = true

                            repository.createReservation(
                                lotteryId = lotteryId,
                                campsiteId = campsite.id,
                                checkIn = checkIn,
                                checkOut = checkOut,
                                guestCount = guestCount,
                                onSuccess = { reservation ->
                                    submitting = false
                                    result = BookingResult.Success(
                                        reservation = reservation,
                                        campsiteName = campsite.name
                                    )
                                },
                                onFailure = { exception ->
                                    submitting = false
                                    result = BookingResult.Failure(
                                        message = exception.message
                                            ?: "Unable to create reservation."
                                    )
                                }
                            )
                        }
                    },
                    onBackClick = onBackClick
                )
            }
        }
    }
}
