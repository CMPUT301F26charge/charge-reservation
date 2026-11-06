package com.charge.reservation.owner

import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import com.charge.reservation.model.Lottery
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.AvailabilityDay


/**
 * Displays saved campsite details, availability and lottery configuration.
 * Registration status uses the device clock and refreshes
 * once per second while the screen is composed.
 *
 * @param lotteries saved lotteries associated with this campsite.
 * @param isLoadingLotteries whether a lottery request is pending.
 * @param lotteriesError load failure message, or empty when no error exists.
 * @param onRetryLotteries requests another attempt after a failed load.
 */
@Composable
fun OwnerCampsiteDetailsScreen(
    campsite: Campsite?,
    availabilityDays: List<AvailabilityDay>,
    isLoadingAvailability: Boolean,
    availabilityError: String,
    lotteries: List<Lottery>,
    isLoadingLotteries: Boolean,
    lotteriesError: String,
    onRetryLotteries: () -> Unit,
    onRetryAvailability: () -> Unit,
    onCreateLotteryClick: () -> Unit
) {
    var now by remember(campsite?.id) {
        mutableStateOf(Instant.now())
    }

    LaunchedEffect(campsite?.id) {
        while (isActive) {
            now = Instant.now()
            delay(1_000L)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() //keep content below status bar
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Campsite Details",
            fontSize = 28.sp
        )

        if (campsite == null) {
            Text("Campsite not found.")
            return@Column
        }

        if (isLoadingAvailability) {
            Text("Loading campsite details...")
            return@Column
        }

        if (availabilityError.isNotEmpty()) {
            Text("Can not load campsite details. Please try again.")

            Button(onClick = onRetryAvailability) {
                Text("Retry")
            }

            return@Column
        }

        Text(
            text = campsite.name,
            fontSize = 24.sp
        )

        Button(onClick = onCreateLotteryClick) {
            Text("Create lottery")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Location: ${campsite.location}")
                Text("Description: ${campsite.description}")

                Text(
                    "Maximum occupancy: " +
                            (campsite.maxOccupancy?.toString()
                                ?: "Not specified")
                )

                Text(
                    "Price per night: " +
                            (campsite.pricePerNight?.let {
                                "$" + String.format("%.2f", it)
                            } ?: "Not specified")
                )

                Text(
                    "Average rating: " +
                            (campsite.averageRating?.toString()
                                ?: "No ratings yet")
                )

                Text(
                    "Tags: " +
                            campsite.tags.joinToString()
                                .ifBlank { "None" }
                )

                Text("Rules: ${campsite.rules}")
                Text(
                    "Cancellation policy: " +
                            campsite.cancellationPolicy
                )

                Text(
                    "Map coordinates: " +
                            if (
                                campsite.latitude != null &&
                                campsite.longitude != null
                            ) {
                                "${campsite.latitude}, ${campsite.longitude}"
                            } else {
                                "Not provided"
                            }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nightly availability")

                if (availabilityDays.isEmpty()) {
                    Text("No availability specified.")
                } else {
                    availabilityDays.forEach { day ->
                        val capacity = day.guestCapacity ?: 0
                        val remaining = capacity - day.bookedGuests

                        Text("Night of ${day.date}")
                        Text("Total guest spaces: $capacity")
                        Text("Booked guests: ${day.bookedGuests}")
                        Text("Remaining spaces: $remaining")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Promotional Image")

                Spacer(modifier = Modifier.height(8.dp))

                //display the first promotional image if available.
                val imageUrl = campsite.promoImageUrls.firstOrNull()

                if (imageUrl.isNullOrBlank()) {
                    Text("No promotional image available")
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Image of ${campsite.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Text(
            text = "Saved lotteries",
            fontSize = 20.sp
        )

        when {
            isLoadingLotteries -> {
                Text("Loading lotteries...")
            }

            lotteriesError.isNotEmpty() -> {
                Text(
                    text = lotteriesError,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = onRetryLotteries) {
                    Text("Retry")
                }
            }

            lotteries.isEmpty() -> {
                Text("No lotteries published for this campsite.")
            }

            else -> {

                lotteries.forEach { lottery ->
                    val opens = lottery.registrationStartAt
                    val closes = lottery.registrationEndAt

                    val registrationStatus = when {
                        opens == null || closes == null -> "Unavailable"
                        now.isBefore(opens) -> "Not open yet"
                        now.isBefore(closes) -> "Open"
                        else -> "Closed"
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Registration: $registrationStatus")

                            Text(
                                "Registration opens: " +
                                        lotteryDateTimeText(
                                            lottery.registrationStartAt,
                                            lottery.timeZone
                                        )
                            )

                            Text(
                                "Registration closes: " +
                                        lotteryDateTimeText(
                                            lottery.registrationEndAt,
                                            lottery.timeZone
                                        )
                            )

                            Text("Earliest check-in: ${lottery.bookingStartDate}")
                            Text("Latest checkout: ${lottery.bookingEndDate}")

                            Text(
                                "Booking deadline: " +
                                        lotteryDateTimeText(
                                            lottery.bookingDeadline,
                                            lottery.timeZone
                                        )
                            )

                            Text("Number of winners: ${lottery.winnerCount}")
                        }
                    }
                }
            }
        }
    }

}

/**
 * Formats a saved instant in the lottery's stored time zone.
 *
 * @return date, time and zone ID, or "Not specified" for missing values.
 */
private fun lotteryDateTimeText(
    instant: Instant?,
    timeZone: ZoneId?
): String {
    if (instant == null || timeZone == null) {
        return "Not specified"
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm VV")
    return instant.atZone(timeZone).format(formatter)
}