package com.charge.reservation.repository

import android.net.Uri
import android.util.Patterns
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
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

        // Check document existence and create the document.
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

        // Check the server for the current profile and removal status.
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
                        // New profiles always start with entrant access.
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

    /**
     * Updates a user's personal information in Firestore.
     *
     * Only changes editable profile fields. User roles,
     * notification preferences and removal status are preserved.
     *
     * If an image is provided, the image is uploaded first.
     * A failed image upload does not overwrite saved profile data.
     *
     * @param profile the user's current profile
     * @param name new display name
     * @param email new contact email
     * @param phoneNumber new phone number
     * @param imageUri optional new image selected from the device
     * @param onSuccess receives the updated profile
     * @param onFailure receives validation or Firebase errors
     */
    fun updateProfile(
        profile: UserProfile,
        name: String,
        email: String,
        phoneNumber: String,
        imageUri: Uri?,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updatedName = name.trim()
        val updatedEmail = email.trim()
        val updatedPhone = phoneNumber.trim()

        val error = when {
            profile.id.isBlank() ->
                "User ID cannot be empty."

            updatedName.isBlank() ->
                "Please enter your name."

            updatedEmail.isBlank() ->
                "Please enter your email address."

            !Patterns.EMAIL_ADDRESS.matcher(
                updatedEmail
            ).matches() ->
                "Please enter a valid email address."

            else -> null
        }

        if (error != null) {
            onFailure(IllegalArgumentException(error))
            return
        }

        /**
         * Updates only the editable fields.
         * Checks that the original profile still exists
         * and has not been removed.
         */
        fun saveProfile(imageUrl: String?) {
            val profileReference = firestore
                .collection("users")
                .document(profile.id)

            val updates = mapOf<String, Any?>(
                "name" to updatedName,
                "email" to updatedEmail,
                "phoneNumber" to updatedPhone,
                "profileImageUrl" to imageUrl
            )

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileReference)

                if (!snapshot.exists()) {
                    throw IllegalStateException(
                        "Your profile no longer exists."
                    )
                }

                if (snapshot.getBoolean("removed") == true) {
                    throw IllegalStateException(
                        "This profile has been removed."
                    )
                }

                transaction.update(profileReference, updates)

                Unit
            }
                .addOnSuccessListener {
                    onSuccess(
                        profile.copy(
                            name = updatedName,
                            email = updatedEmail,
                            phoneNumber = updatedPhone,
                            profileImageUrl = imageUrl
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        }

        // No new picture: retain the previously saved image.
        if (imageUri == null) {
            saveProfile(profile.profileImageUrl)
            return
        }

        // Upload the new picture before updating Firestore.
        try {
            val storageReference = FirebaseStorage
                .getInstance()
                .reference
                .child("profileImages")
                .child(profile.id)
                .child(UUID.randomUUID().toString())

            storageReference
                .putFile(imageUri)
                .continueWithTask { uploadTask ->
                    if (!uploadTask.isSuccessful) {
                        throw (
                                uploadTask.exception
                                    ?: IllegalStateException(
                                        "Profile image upload failed."
                                    )
                                )
                    }

                    storageReference.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    saveProfile(downloadUri.toString())
                }
                .addOnFailureListener { exception ->
                    // The previous saved image and profile
                    // remain unchanged if upload fails.
                    onFailure(exception)
                }
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }
}