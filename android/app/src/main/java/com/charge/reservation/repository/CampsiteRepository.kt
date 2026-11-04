package com.charge.reservation.repository

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore

import com.charge.reservation.model.Campsite

/**
 * Handles campsite listings stored in Firestore.
 */
class CampsiteRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Generates a new campsite ID.
     */
    fun generateCampsiteId(): String {
        return firestore.collection("campsites").document().id
    }

    /**
     * Creates a campsite using its ID as the document ID.
     *
     * Refuses to overwrite an existing listing.
     *
     * @param campsite the listing, including its ID and owner's UID.
     * @param onSuccess called after the listing is successfully saved.
     * @param onFailure receives validation or Firestore errors.
     */
    fun createCampsite (
        campsite: Campsite,
        onSuccess:() -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val price = campsite.pricePerNight
        // Identify missing required information before contacting Firestore.
        val validationError = when {
            campsite.id.isBlank() -> "Campsite ID must not be blank."

            campsite.ownerId.isBlank() -> "Owner ID must not be blank."

            campsite.name.isBlank() -> "Please enter the campsite name."

            campsite.description.isBlank() -> "Please enter the campsite description."

            campsite.location.isBlank() -> "Please enter the campsite location."

            (campsite.maxOccupancy ?: 0) <= 0 -> "Please enter a positive whole number for maximum occupancy."

            price == null || !price.isFinite() || price < 0.0 -> "Please enter a valid price of 0 or more."

            campsite.rules.isBlank() -> "Please enter the campsite rules."

            campsite.cancellationPolicy.isBlank() -> "Please enter the cancellation policy."

            else -> null
        }

        if (validationError != null) {
            onFailure(IllegalArgumentException(validationError))
            return
        }

        // Point to campsites/campsite.id.
        val campsiteDocument = firestore.collection("campsites").document(campsite.id)

        val campsiteData = mapOf<String, Any?>(
            "id" to campsite.id,
            "ownerId" to campsite.ownerId,
            "name" to campsite.name,
            "description" to campsite.description,
            "promoImageUrls" to campsite.promoImageUrls,
            "tags" to campsite.tags,
            "maxOccupancy" to campsite.maxOccupancy,
            "pricePerNight" to campsite.pricePerNight,
            "location" to campsite.location,
            "latitude" to campsite.latitude,
            "longitude" to campsite.longitude,
            "rules" to campsite.rules,
            "cancellationPolicy" to campsite.cancellationPolicy,

            "averageRating" to null,
            "removed" to false
        )

        // Check document existence and create the document.
        val creationTask = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(campsiteDocument)

            if (snapshot.exists()) {
                throw IllegalStateException("A campsite or removal record already exists with this ID.")
            }

            transaction.set(campsiteDocument, campsiteData)
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
     * Listens for active campsite listings belonging to an owner.
     *
     * @param ownerId the signed-in owner's Firebase Authentication UID.
     * @param onSuccess receives the current list whenever it changes.
     * @param onFailure receives Firestore or data conversion errors.
     * @return the listener registration, remove it when no longer needed.
     */

    fun listenToOwnerCampsites(
        ownerId: String,
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("campsites")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("removed", false)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                val campsites = mutableListOf<Campsite>()
                try {
                    if (snapshot != null) {
                        for (document in snapshot.documents) {
                            // Convert the document into a Campsite.
                            val campsite = document.toObject(Campsite::class.java)

                            if (campsite != null) {
                                val campsiteWithId = campsite.copy(id = document.id)
                                campsites.add(campsiteWithId)
                            }
                        }
                    }
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                onSuccess(campsites)
            }
    }
}