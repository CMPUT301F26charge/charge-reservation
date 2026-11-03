package com.charge.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.charge.reservation.model.Campsite
import com.charge.reservation.ui.CampsiteCard

@Composable
fun HomeScreen(
    navController: NavController,
    onCampsiteClick: (Campsite) -> Unit
) {

    val campsite1 = Campsite(
        id = "1",
        name = "Pine Lake Campground",
        location = "Banff, Alberta",
        maxOccupancy = 4,
        averageRating = 4.6,
        description = "Fire pit, parking, washrooms",
        tags = listOf("Lakeside", "Tent camping"),
        pricePerNight = 65.0,
        rules = "Quiet hours 10 PM–7 AM. Fires in fire pits only.",
        cancellationPolicy = "Cancellation subject to campsite terms."
    )

    val campsite2 = Campsite(
        id = "2",
        name = "Crystal Lake Campground",
        location = "Jasper, Alberta",
        maxOccupancy = 6
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        Text(
            text = "Explore campsites",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF5F7F5)
        )

        Spacer(modifier = Modifier.height(24.dp))

        CampsiteCard(campsite1, onClick = {
            onCampsiteClick(campsite1)
        })

        Spacer(modifier = Modifier.height(16.dp))

        CampsiteCard(campsite2, onClick = {
            onCampsiteClick(campsite2)
        })
    }
}