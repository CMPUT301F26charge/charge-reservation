package com.charge.reservation.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Campsite
import java.util.Locale
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.AvailabilityDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.charge.reservation.LotteryButtonState

/**
 * Shows campsite details, availability, policies, and the lottery entry button.
 * @param campsite selected campsite
 * @param onBack returns to campsite browsing
 * @param lottery associated lottery, if available
 * @param waitlistCount current number of waitlisted entrants
 * @param lotteryButtonState current join/leave button state
 * @param isLotteryActionWorking whether a lottery action is running
 * @param lotteryActionError error displayed after a failed action
 * @param onLotteryAction called when the join/leave button is selected
 */
@Composable
fun CampsiteDetailsScreen(
    campsite: Campsite,
    onBack: () -> Unit,
    lottery: Lottery? = null,
    waitlistCount: Int = 0,
    lotteryButtonState: LotteryButtonState = LotteryButtonState.UNAVAILABLE,
    isLotteryActionWorking: Boolean = false,
    lotteryActionError: String = "",
    availabilityDays: List<AvailabilityDay> = emptyList(),
    isLoadingAvailability: Boolean = true,
    availabilityError: String = "",
    onLotteryAction: () -> Unit = {}
) {
    val green = Color(0xFF087F5B)
    val grey = Color(0xFF666666)
    val uriHandler = LocalUriHandler.current
    val now = Instant.now()

    val registrationOpen = lottery?.registrationStartAt?.let { start ->
        lottery.registrationEndAt?.let { end ->
            !now.isBefore(start) && now.isBefore(end)
        }
    } ?: false

    val waitlistFull = lottery?.waitlistCapacity?.let { capacity ->
        waitlistCount >= capacity
    } ?: false

    val availableDays = availabilityDays.filter { day ->
        day.date != null && !day.date.isBefore(LocalDate.now()) &&
                (day.guestCapacity ?: 0) > day.bookedGuests
    }.sortedBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "‹",
                    fontSize = 30.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                "Campsite details",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val imageUrl = campsite.promoImageUrls.firstOrNull()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Image of ${campsite.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "No promotional image",
                    color = grey
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            campsite.name,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = campsite.location,
                color = grey,
                fontSize = 14.sp
            )

            Text(
                text = campsite.averageRating?.let {
                    "★ ${String.format(Locale.US, "%.1f", it)}"
                } ?: "No ratings yet",
                color = Color(0xFF996A00),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
            shape = RoundedCornerShape(11.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                Text(
                    "Overview",
                    color = green,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Up to ${campsite.maxOccupancy ?: "?"} guests",
                    color = Color.Black
                )

                Text(
                    campsite.description.ifBlank {
                        "No amenities provided"
                    },
                    color = grey
                )

                Text(
                    "Tags: ${campsite.tags.joinToString().ifBlank { "None" }}",
                    color = grey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Availability & price",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Available dates", color = Color.Black)

                when {
                    isLoadingAvailability -> Text("Loading available dates...", color = grey)
                    availabilityError.isNotEmpty() -> Text(availabilityError, color = grey)
                    availableDays.isEmpty() -> Text("No available nights.", color = grey)
                    else -> availableDays.forEach { day ->
                        val spots = (day.guestCapacity ?: 0) - day.bookedGuests
                        Text("${day.date}: $spots guest spaces available", color = grey)
                    }
                }

                Text(
                    campsite.pricePerNight?.let {
                        "\$${String.format(Locale.US, "%.2f", it)} per night"
                    } ?: "Price unavailable",
                    color = grey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Location",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val query = if (
                        campsite.latitude != null &&
                        campsite.longitude != null
                    ) {
                        "${campsite.latitude},${campsite.longitude}"
                    } else {
                        campsite.location
                    }

                    uriHandler.openUri(
                        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(campsite.location, color = Color.Black)
                Text("View map location →", color = green)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Rules & cancellation",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Text(
            campsite.rules.ifBlank { "No rules provided" },
            color = grey
        )

        Text(
            campsite.cancellationPolicy.ifBlank {
                "No cancellation policy provided"
            },
            color = grey
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Lottery",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        if (lottery == null) {
            Text("No lottery available for this campsite.", color = grey)
        } else {
            val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                .withZone(lottery.timeZone ?: ZoneId.systemDefault())

            Text(
                "Registration opens: " + (lottery.registrationStartAt?.let {
                    dateFormat.format(it)
                } ?: "Not set"),
                color = grey
            )
            Text(
                "Registration closes: " + (lottery.registrationEndAt?.let {
                    dateFormat.format(it)
                } ?: "Not set"),
                color = grey
            )
            Text(
                "Waitlist: $waitlistCount" +
                        (lottery.waitlistCapacity?.let { " / $it" } ?: " (no limit)"),
                color = grey
            )

            val status = when {
                lottery.drawCompletedAt != null -> "Draw completed"
                registrationOpen && waitlistFull -> "Waitlist full"
                registrationOpen -> "Registration open"
                lottery.registrationStartAt != null &&
                        now.isBefore(lottery.registrationStartAt) -> "Not open yet"
                else -> "Registration closed"
            }
            Text("Status: $status", color = grey)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLotteryAction,
            enabled = lotteryButtonState.enabled && !isLotteryActionWorking,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF2A2F2C),
                disabledContentColor = grey
            )
        ) {
            if (isLotteryActionWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
            } else {
                Text(lotteryButtonState.label, fontWeight = FontWeight.Bold)
            }
        }

        if (lotteryActionError.isNotEmpty()) {
            Text(lotteryActionError, color = Color(0xFFFF6B6B), fontSize = 12.sp)
        }
    }
}
