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

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.AvailabilityDay


/**
 * Displays saved campsite details and its optional promotional image.
 */
@Composable
fun OwnerCampsiteDetailsScreen(
    campsite: Campsite?,
    availabilityDays: List<AvailabilityDay>,
    isLoadingAvailability: Boolean,
    availabilityError: String,
    onRetryAvailability: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() //keep content below status bar
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBackClick) {
            Text("Back")
        }

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
    }
}
