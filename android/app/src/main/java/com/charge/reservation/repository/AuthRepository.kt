package com.charge.reservation.repository

import com.google.firebase.auth.FirebaseAuth

/**
 * Provides the Firebase Authentication identity.
 *
 * Reuses the current signed-in user or signs in anonymously when needed.
 */
class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Obtains the current user's Firebase UID.
     *
     * If a user is already signed in, calls onSuccess immediately.
     * Otherwise, starts anonymous sign-in and reports the result when it finishes.
     *
     * @param onSuccess receives the signed-in user's Firebase UID.
     * @param onFailure receives the exception if sign-in fails or the
     * successful sign-in result contains no user.
     */
    fun getOrCreateUserId(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val existingUser = auth.currentUser

        if (existingUser != null) {
            onSuccess(existingUser.uid)
            return
        }

        val signInTask = auth.signInAnonymously()

        signInTask.addOnSuccessListener { result ->
            val user = result.user

            if (user != null) {
                onSuccess(user.uid)
            } else {
                onFailure(
                    Exception("Anonymous sign-in succeeded, but result.user was null.")
                )
            }
        }

        signInTask.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }

}