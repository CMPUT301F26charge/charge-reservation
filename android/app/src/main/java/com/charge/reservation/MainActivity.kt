package com.charge.reservation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import com.charge.reservation.ui.theme.ChargeReservationTheme
import com.charge.reservation.repository.AuthRepository
import com.charge.reservation.repository.UserProfileRepository
import com.charge.reservation.model.UserProfile

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val userProfileRepository = UserProfileRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository.getOrCreateUserId(
            onSuccess = { userId ->
                Log.d("Authentication", "User ID: $userId" )
                userProfileRepository.getProfile(
                    userId = userId,
                    onSuccess = { profile ->
                        when {
                            profile == null -> {
                                Log.d("ProfileLoading", "No profile exists for this user.")
                            }

                            profile.removed -> {
                                Log.d("ProfileLoading", "This profile is removed.")
                            }

                            else -> {
                                Log.d("ProfileLoading", "Loaded profile ID: ${profile.id}, name:${profile.name}")
                            }
                        }

                    },
                    onFailure = { exception ->
                        Log.e("ProfileLoading", "Can not load the profile", exception)
                    }
                )
            },
            onFailure = { exception ->
                Log.e("Authentication", "Authentication failed", exception)
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

