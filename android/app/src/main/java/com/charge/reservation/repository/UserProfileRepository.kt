package com.charge.reservation.repository

import com.charge.reservation.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * Handles access to user profiles stored in Cloud Firestore.
 */
class UserProfileRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Creates an initial profile using its Firebase UID as the document ID.
     *
     * Refuses to overwrite any existing profile or removal marker.
     * New profiles start as active entrants without owner or admin access.
     *
     * @param profile the initial profile details, including the Firebase UID.
     * @param onSuccess called after the profile is successfully saved.
     * @param onFailure receives validation, existing-document, or Firestore errors.
     */
    fun createProfile(
        profile: UserProfile,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (profile.id.isBlank()) {
            onFailure(
                IllegalStateException("The user ID must not be blank.")
            )
            return
        }

        val profileDocument = firestore.collection("users").document(profile.id)
        val profileData = mapOf<String, Any?>(
            "id" to profile.id,
            "name" to profile.name,
            "email" to profile.email,
            "phoneNumber" to profile.phoneNumber,
            "profileImageUrl" to profile.profileImageUrl,
            "notificationEnabled" to profile.notificationEnabled,

            "isEntrant" to true,
            "isOwner" to false,
            "isAdmin" to false,
            "removed" to false
        )

        val creationTask = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(profileDocument)

            if (snapshot.exists()) {
                throw IllegalStateException("A profile already exists for this user.")
            }

            transaction.set(profileDocument, profileData)
            Unit
        }

        creationTask.addOnSuccessListener {
            onSuccess()
        }

        creationTask.addOnFailureListener { exception ->
            onFailure(exception)
        }

    }

    /**
     * Reads a user's profile from Firestore.
     *
     * Returns the profile through onSuccess when the document exists.
     * Returns null through onSuccess when the document does not exist.
     * Removed profiles are returned with their removed flag for the caller to check.
     *
     * @param userId the user's Firebase Authentication UID.
     * @param onSuccess receives the profile, or null if no document exists.
     * @param onFailure receives validation, read, or data conversion errors.
     */
    fun getProfile(
        userId: String,
        onSuccess: (UserProfile?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userId.isBlank()) {
            onFailure(IllegalArgumentException("The user ID must not be blank."))
            return
        }

        val profileDocument = firestore.collection("users").document(userId)

        val readTask = profileDocument.get(Source.SERVER)

        readTask.addOnSuccessListener { document ->
            if (document.exists()) {
                val profile = try {
                    UserProfile(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        phoneNumber = document.getString("phoneNumber") ?: "",
                        profileImageUrl = document.getString("profileImageUrl"),
                        notificationEnabled = document.getBoolean("notificationEnabled") ?: true,
                        isEntrant = document.getBoolean("isEntrant") ?: true,
                        isOwner = document.getBoolean("isOwner") ?: false,
                        isAdmin = document.getBoolean("isAdmin") ?: false,
                        removed = document.getBoolean("removed") ?: false
                    )
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }

                onSuccess(profile)
            } else {
                onSuccess(null)
            }
        }

        readTask.addOnFailureListener {
            exception -> onFailure(exception)
        }
    }
}