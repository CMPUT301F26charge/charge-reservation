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

import com.charge.reservation.model.UserProfile
import com.charge.reservation.repository.CampsiteRepository


/**
 * Navigates between home and campsite registration.
 */
@Composable
fun AppNavigation(profile: UserProfile) {
    val navController = rememberNavController()
    val campsiteRepository = remember { CampsiteRepository() }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                navController = navController,
                profile = profile
            )
        }

        composable("createCampsite") { entry ->
            if (!profile.isOwner || profile.removed) {
                Text("Only active owners can register a campsite.")
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
                    onCreateCampsite = { campsite->
                        if (!isSaving) {
                            isSaving = true
                            errorMessage = ""

                            campsiteRepository.createCampsite(
                                campsite = campsite,
                                onSuccess = {
                                    isSaving = false

                                    if (navController.currentBackStackEntry == entry) {
                                        navController.popBackStack()
                                    }
                                },
                                onFailure = { exception ->
                                    Log.e("CampsiteCreation", "Could not create the campsite", exception)

                                    isSaving = false
                                    errorMessage =
                                        if (exception is IllegalArgumentException) {
                                            exception.message
                                                ?: "Please check the campsite details."
                                        } else {
                                            "Could not create the campsite. Please try again."
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