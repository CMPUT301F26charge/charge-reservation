package com.charge.reservation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.charge.reservation.model.Campsite
import com.charge.reservation.ui.CampsiteDetailsScreen
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.charge.reservation.model.Lottery
import java.time.Instant
@Composable
fun CampsiteApp() {

    val navcontroller = rememberNavController()
    val context = LocalContext.current

    val sampleLottery = remember {
        Lottery(
            id = "lottery-1",
            campsiteIds = listOf("1"),
            registrationStartAt = Instant.now().minusSeconds(3600),
            registrationEndAt = Instant.now().plusSeconds(86400),
            waitlistCapacity = 10
        )
    }

    var selectedCampsite by remember {
        mutableStateOf<Campsite?>(null)
    }

    NavHost(
        navController = navcontroller,
        startDestination = "home"
    ) {

        composable("home") {
            HomeScreen(
                navController = navcontroller,
                onCampsiteClick = { campsite ->
                    selectedCampsite = campsite
                    navcontroller.navigate("details")
                }
            )
        }

        composable("details") {
            selectedCampsite?.let { campsite ->
                CampsiteDetailsScreen(
                    campsite = campsite,
                    lottery = sampleLottery.takeIf {
                        campsite.id in it.campsiteIds
                    },
                    waitlistCount = 0,
                    onBack = {
                        navcontroller.popBackStack()
                    },
                    onJoinLottery = {
                        Toast.makeText(
                            context,
                            "Demo only: no waitlist entry was created",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}