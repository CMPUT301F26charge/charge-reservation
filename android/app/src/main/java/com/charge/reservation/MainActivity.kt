package com.charge.reservation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.charge.reservation.controller.AppLaunchState
import com.charge.reservation.ui.theme.ChargeReservationTheme
import com.charge.reservation.repository.AuthRepository
import com.charge.reservation.repository.UserProfileRepository

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val userProfileRepository = UserProfileRepository()
    private var appLaunchState by mutableStateOf<AppLaunchState>(AppLaunchState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository.getOrCreateUserId(
            onSuccess = { userId ->
                Log.d("Authentication", "User ID: $userId" )
                userProfileRepository.getProfile(
                    userId = userId,
                    onSuccess = { profile ->
                        appLaunchState = when {
                            profile == null ->
                                AppLaunchState.CreateProfile(userId)

                            profile.removed ->
                                AppLaunchState.ProfileRemoved

                            else ->
                                AppLaunchState.ProfileReady(profile)
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

        enableEdgeToEdge()
        setContent {
            ChargeReservationTheme {
                AppNavigation()
            }
        }
    }
}

