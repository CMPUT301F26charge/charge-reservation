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
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.charge.reservation.R
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
    val green = Color(0xFF41D49A)
    val grey = Color(0xFFB5B5B5)
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
            .background(Color(0xFF050505))
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
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                "Campsite details",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF121413)),
        contentAlignment = Alignment.Center
    ) {
        if (campsite.id == "1") {
            Image(
                painter = painterResource(R.drawable.pine_lake),
                contentDescription = "Campsite photograph",
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
            color = Color.White
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
                color = Color(0xFFFFCF45),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121413)
            ),
            border = BorderStroke(1.dp, Color(0xFF333B37)),
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
                    color = Color.White
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121413)
            ),
            border = BorderStroke(1.dp, Color(0xFF333B37))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Available dates", color = Color.White)

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
            color = Color.White,
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
                containerColor = Color(0xFF14201A)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(campsite.location, color = Color.White)
                Text("View map location →", color = green)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Rules & cancellation",
            color = Color.White,
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
            color = Color.White,
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
                contentColor = Color.Black
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