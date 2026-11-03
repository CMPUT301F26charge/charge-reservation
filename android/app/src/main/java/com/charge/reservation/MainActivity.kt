package com.charge.reservation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.charge.reservation.ui.theme.ChargeReservationTheme
import android.util.Log
import com.charge.reservation.repository.AuthRepository
import com.google.android.gms.tasks.OnSuccessListener

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository.getOrCreateUserId(
            onSuccess = { userId ->
                Log.d("AuthCheck", "User ID: $userId" )
            },
            onFailure = { exception ->
                Log.e("AuthCheck", "Authentication failed", exception)
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

