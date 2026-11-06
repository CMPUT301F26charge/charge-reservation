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
import java.time.Instant

@Composable
fun CampsiteDetailsScreen(
    campsite: Campsite,
    onBack: () -> Unit,
    onJoinLottery: () -> Unit,
    lottery: Lottery? = null,
    waitlistCount: Int = 0
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

    val joinEnabled = registrationOpen && !waitlistFull

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

                Text(
                    "Availability will be loaded from campsite records",
                    color = grey
                )

                Text(
                    campsite.pricePerNight?.let {
                        "$${String.format(Locale.US, "%.2f", it)} per night"
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

        Text(
            "Lottery information will be connected next",
            color = grey
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onJoinLottery,
            enabled = joinEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.White
            )
        ) {
            Text("Join Lottery", fontWeight = FontWeight.Bold)
        }

        if (!joinEnabled) {
            Text(
                "Lottery joining is not available yet",
                color = grey,
                fontSize = 12.sp
            )
        }
    }
}