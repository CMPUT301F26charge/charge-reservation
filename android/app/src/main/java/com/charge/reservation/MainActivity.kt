package com.charge.reservation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import com.charge.reservation.ui.theme.ChargeReservationTheme
import com.charge.reservation.controller.AppLaunchState
import com.charge.reservation.repository.AuthRepository
import com.charge.reservation.repository.UserProfileRepository
import com.charge.reservation.model.UserProfile
import com.charge.reservation.profile.CreateProfileScreen

/**
 * Signs in anonymously and loads the user's Firestore profile.
 * Displays profile setup, the main app, a removed profile message,
 * or an error with retry.
 */
class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val userProfileRepository = UserProfileRepository()
    private var appLaunchState by mutableStateOf<AppLaunchState>(AppLaunchState.Loading)

    private fun launchApp() {
        appLaunchState = AppLaunchState.Loading

        authRepository.getOrCreateUserId(
            onSuccess = { userId ->
                Log.d("Authentication", "User ID: $userId")

                userProfileRepository.getProfile(
                    userId,
                    onSuccess = { profile ->
                        appLaunchState = when {
                            profile == null -> AppLaunchState.CreateProfile(userId)
                            profile.removed -> AppLaunchState.ProfileRemoved
                            else -> AppLaunchState.ProfileReady(profile)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("ProfileLoading", "Can not load the profile", exception)
                        appLaunchState = AppLaunchState.Error("Can not load your profile. Please try again.")
                    }
                )
            },
            onFailure = { exception ->
                Log.e("Authentication", "Authentication failed", exception)
                appLaunchState = AppLaunchState.Error("Can not sign in. Please try again.")
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launchApp()

        enableEdgeToEdge()

        setContent {
            ChargeReservationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = appLaunchState) {
                            AppLaunchState.Loading -> CircularProgressIndicator()

                            is AppLaunchState.CreateProfile -> {
                                CreateProfileScreen(
                                    onCreateProfile = { name, email, phoneNumber ->
                                        val profile = UserProfile(
                                            id = state.userId,
                                            name = name,
                                            email = email,
                                            phoneNumber = phoneNumber
                                        )

                                        appLaunchState = AppLaunchState.Loading

                                        userProfileRepository.createProfile(
                                            profile = profile,
                                            onSuccess = { launchApp() },
                                            onFailure = { exception ->
                                                Log.e(
                                                    "ProfileCreation",
                                                    "Can not create the profile",
                                                    exception
                                                )
                                                appLaunchState =
                                                    AppLaunchState.Error("Can not create your profile. Please try again.")
                                            }
                                        )

                                    }
                                )

                            }

                            is AppLaunchState.ProfileReady -> {
                                AppNavigation(profile = state.profile)
                            }

                            AppLaunchState.ProfileRemoved -> Text("This profile has been removed.")

                            is AppLaunchState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(state.message)

                                    Button(
                                        onClick = { launchApp() }
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

