package com.charge.reservation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri

import com.charge.reservation.owner.OwnerCampsiteDetailsScreen
import com.charge.reservation.model.Campsite
import com.charge.reservation.owner.OwnerCampsiteListScreen
import com.charge.reservation.model.UserProfile
import com.charge.reservation.repository.CampsiteRepository
import com.charge.reservation.model.AvailabilityDay


/**
 * Navigates between home and campsite registration.
 */
@Composable
fun AppNavigation(profile: UserProfile) {
    val navController = rememberNavController()
    val campsiteRepository = remember { CampsiteRepository() }

    var ownerCampsites by remember(profile.id) {
        mutableStateOf<List<Campsite>>(emptyList())
    }
    var isLoadingOwnerCampsites by remember(profile.id) {
        mutableStateOf(true)
    }
    var ownerCampsitesError by remember(profile.id) {
        mutableStateOf("")
    }
    var ownerListRetry by remember(profile.id) {
        mutableStateOf(0)
    }

    if (profile.isOwner && !profile.removed) {
        DisposableEffect(profile.id, campsiteRepository, ownerListRetry) {
            isLoadingOwnerCampsites = true
            ownerCampsitesError = ""

            val listener = campsiteRepository.listenToOwnerCampsites(
                ownerId = profile.id,
                onSuccess = { campsites ->
                    ownerCampsites = campsites
                    ownerCampsitesError = ""
                    isLoadingOwnerCampsites = false
                },
                onFailure = { exception ->
                    Log.e("OwnerCampsites","Can not load owner campsites",exception)

                    ownerCampsites = emptyList()
                    ownerCampsitesError = "Can not load your campsites. Please try again."
                    isLoadingOwnerCampsites = false
                }
            )

            onDispose { listener.remove() }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        composable("home") {
            HomeScreen(
                navController = navController,
                profile = profile
            )
        }

        composable ("ownerCampsites"){
            if (!profile.isOwner || profile.removed) {
                Text("Only active owners can view their campsite listings.")
            } else {
                when {
                    isLoadingOwnerCampsites -> {
                        CircularProgressIndicator()
                    }

                    ownerCampsitesError.isNotEmpty() -> {
                        Column {
                            Text(ownerCampsitesError)

                            Button(
                                onClick = { ownerListRetry++ }
                            ) {
                                Text("Retry")
                            }

                            Button (
                                onClick = { navController.popBackStack() }
                            ) {
                                Text("Back")
                            }
                        }
                    }

                    else -> {
                        OwnerCampsiteListScreen(
                            currentOwnerId = profile.id,
                            campsites = ownerCampsites,
                            onCampsiteClick = { campsiteId ->
                                navController.navigate(
                                    "ownerCampsiteDetails/${Uri.encode(campsiteId)}"
                                )
                            },
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onRegisterCampsiteClick = {
                                navController.navigate("createCampsite")
                            }
                        )
                    }
                }


            }
        }

        composable("ownerCampsiteDetails/{campsiteId}") { entry ->
            val campsiteId = entry.arguments?.getString("campsiteId")

            when {
                !profile.isOwner || profile.removed -> {
                    Text("Only campsite owners can view their campsite listings.")
                }

                isLoadingOwnerCampsites -> {
                    CircularProgressIndicator()
                }

                ownerCampsitesError.isNotEmpty() -> {
                    Column {
                        Text(ownerCampsitesError)

                        Button(onClick = { ownerListRetry++ }) {
                            Text("Retry")
                        }

                        Button(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Back")
                        }
                    }
                }

                else -> {
                    val campsite = ownerCampsites.find {
                        it.id == campsiteId
                    }

                    var availabilityDays by remember(campsite?.id) {
                        mutableStateOf<List<AvailabilityDay>>(emptyList())
                    }
                    var isLoadingAvailability by remember(campsite?.id) {
                        mutableStateOf(true)
                    }
                    var availabilityError by remember(campsite?.id) {
                        mutableStateOf("")
                    }
                    var availabilityRetry by remember(campsite?.id) {
                        mutableStateOf(0)
                    }

                    if (campsite != null) {
                        DisposableEffect(campsite.id, availabilityRetry) {
                            var active = true

                            isLoadingAvailability = true
                            availabilityError = ""

                            campsiteRepository.getAvailability(
                                campsiteId = campsite.id,
                                onSuccess = { days ->
                                    if (active) {
                                        availabilityDays = days
                                        isLoadingAvailability = false
                                    }
                                },
                                onFailure = { exception ->
                                    if (active) {
                                        Log.e(
                                            "CampsiteAvailability",
                                            "Can not load availability",
                                            exception
                                        )

                                        availabilityDays = emptyList()
                                        availabilityError =
                                            "Can not load availability. Please try again."
                                        isLoadingAvailability = false
                                    }
                                }
                            )

                            onDispose { active = false }
                        }
                    }

                    OwnerCampsiteDetailsScreen(
                        campsite = campsite,
                        availabilityDays = availabilityDays,
                        isLoadingAvailability = isLoadingAvailability,
                        availabilityError = availabilityError,
                        onRetryAvailability = { availabilityRetry++ },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        composable("createCampsite") { entry ->
            if (!profile.isOwner || profile.removed) {
                Text("Only campsite owners can register a campsite.")
            } else {
                val campsiteId = rememberSaveable(profile.id) {
                    campsiteRepository.generateCampsiteId()
                }

                var isSaving by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                CreateCampsiteScreen(
                    ownerId = profile.id,
                    campsiteId = campsiteId,
                    isSaving = isSaving,
                    errorMessage = errorMessage,
                    onCreateCampsite = { campsite, availabilityDays ->
                        if (!isSaving) {
                            isSaving = true
                            errorMessage = ""

                            campsiteRepository.createCampsite(
                                campsite = campsite,
                                availabilityDays = availabilityDays,
                                onSuccess = {
                                    isSaving = false

                                    if (navController.currentBackStackEntry == entry) {
                                        navController.popBackStack()
                                    }
                                },
                                onFailure = { exception ->
                                    Log.e("CampsiteCreation", "Can not create campsite", exception)

                                    isSaving = false
                                    errorMessage =
                                        if (exception is IllegalArgumentException) {
                                            exception.message
                                                ?: "Please check the campsite details."
                                        } else {
                                            "Can not create campsite. Please try again."
                                        }
                                }
                            )
                        }
                    }
                )
            }

        }
    }

}