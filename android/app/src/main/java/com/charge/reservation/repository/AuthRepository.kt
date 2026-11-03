package com.charge.reservation.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getOrCreateUserId(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val existingUser = auth.currentUser

        if (existingUser != null) {
            onSuccess(existingUser.uid)
            return
        }

        auth.signInAnonymously().addOnSuccessListener { result ->
                val user = result.user

                if (user != null) {
                    onSuccess(user.uid)
                } else {
                    onFailure(
                        Exception("Anonymous sign-in succeeded, but result.user was null.")
                    )
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

}