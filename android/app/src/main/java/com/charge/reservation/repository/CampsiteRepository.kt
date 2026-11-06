package com.charge.reservation.repository

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.LocalDate
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.AvailabilityDay

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
        availabilityDays: List<AvailabilityDay>,
        imageUri: String? = null,
        onSuccess:() -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val price = campsite.pricePerNight
        val latitude = campsite.latitude
        val longitude = campsite.longitude
        val maximumGuests = campsite.maxOccupancy?:0
        // Identify missing required information before contacting Firestore.
        val validationError = when {
            campsite.id.isBlank() -> "Campsite ID must not be blank."

            campsite.ownerId.isBlank() -> "Owner ID must not be blank."

            campsite.name.isBlank() -> "Please enter the campsite name."

            campsite.description.isBlank() -> "Please enter the campsite description."

            campsite.location.isBlank() -> "Please enter the campsite location."

            latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0 ->
                "Please enter a latitude between -90 and 90."

            longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0 ->
                "Please enter a longitude between -180 and 180."

            (campsite.maxOccupancy ?: 0) <= 0 ->
                "Please enter a positive whole number for maximum occupancy."

            price == null || !price.isFinite() || price < 0.0 ->
                "Please enter a valid price of 0 or more."

            campsite.rules.isBlank() -> "Please enter the campsite rules."

            campsite.cancellationPolicy.isBlank() -> "Please enter the cancellation policy."

            availabilityDays.isEmpty() ->
                "Please select at least one available night."

            availabilityDays.any {
                it.date == null || it.campsiteId != campsite.id
            } ->
                "Availability contains an invalid date or campsite ID."

            availabilityDays.map { it.date }.distinct().size != availabilityDays.size ->
                "Each available night must appear only once."

            availabilityDays.any {
                (it.guestCapacity ?: -1) !in 0..maximumGuests
            } ->
                "Each night's capacity must be a whole number from 0 to $maximumGuests."

            availabilityDays.any { it.bookedGuests != 0 } ->
                "New availability cannot contain existing bookings."
            else -> null
        }

        if (validationError != null) {
            onFailure(IllegalArgumentException(validationError))
            return
        }

        if (imageUri == null) {
            saveCampsite(
                campsite = campsite,
                availabilityDays = availabilityDays,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            return
        }

        try {
            val imageReference = FirebaseStorage.getInstance().reference
                .child("campsiteImages")
                .child(campsite.ownerId)
                .child(campsite.id)
                .child(UUID.randomUUID().toString())

            imageReference.putFile(Uri.parse(imageUri))
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw (
                                task.exception
                                    ?: IllegalStateException("Image upload failed.")
                                )
                    }

                    imageReference.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    saveCampsite(
                        campsite = campsite.copy(
                            promoImageUrls = listOf(downloadUri.toString())
                        ),
                        availabilityDays = availabilityDays,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    /**
     * Saves a validated campsite and its nightly records together.
     */
    private fun saveCampsite(
        campsite: Campsite,
        availabilityDays: List<AvailabilityDay>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val campsiteDocument = firestore.collection("campsites")
            .document(campsite.id)

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

        val creationTask = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(campsiteDocument)

            if (snapshot.exists()) {
                throw IllegalStateException(
                    "A campsite or removal record already exists with this ID."
                )
            }

            transaction.set(campsiteDocument, campsiteData)

            for (day in availabilityDays) {
                val date = requireNotNull(day.date).toString()

                val availabilityDocument = campsiteDocument
                    .collection("availability")
                    .document(date)

                transaction.set(
                    availabilityDocument,
                    mapOf(
                        "campsiteId" to campsite.id,
                        "date" to date,
                        "guestCapacity" to requireNotNull(day.guestCapacity),
                        "bookedGuests" to 0
                    )
                )
            }

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

    /**
     * Listens for all active campsites.
     */
    fun listenToActiveCampsites(
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {

        return firestore.collection("campsites")
            .whereEqualTo("removed", false)
            .addSnapshotListener { snapshot, exception ->

                if (exception != null) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                try {
                    val campsites = snapshot?.documents?.mapNotNull { document ->
                        document.toObject(Campsite::class.java)
                            ?.copy(id = document.id)
                    } ?: emptyList()

                    onSuccess(campsites)

                } catch (e: Exception) {
                    onFailure(e)
                }
            }
    }

    /**
     * Loads the selected campsite's saved nightly availability once.
     */
    fun getAvailability(
        campsiteId: String,
        onSuccess: (List<AvailabilityDay>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("campsites")
            .document(campsiteId)
            .collection("availability")
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val days = try {
                    snapshot.documents.map { document ->
                        val date = LocalDate.parse(
                            requireNotNull(document.getString("date"))
                        )
                        val capacity = Math.toIntExact(
                            requireNotNull(document.getLong("guestCapacity"))
                        )
                        val booked = Math.toIntExact(
                            requireNotNull(document.getLong("bookedGuests"))
                        )

                        require(document.id == date.toString())
                        require(document.getString("campsiteId") == campsiteId)
                        require(capacity >= 0 && booked in 0..capacity)

                        AvailabilityDay(
                            campsiteId = campsiteId,
                            date = date,
                            guestCapacity = capacity,
                            bookedGuests = booked
                        )
                    }.sortedBy { it.date }
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }

                onSuccess(days)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}