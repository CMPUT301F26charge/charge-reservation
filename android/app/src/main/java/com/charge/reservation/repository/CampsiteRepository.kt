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
 * Creates, updates, and reads campsite listings stored in Firestore.
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
     * Saves a new campsite and its nightly availability without overwriting
     * an existing listing. If an image is provided, uploads it before saving.
     *
     * @param campsite the listing, including its ID and the owner's Firebase UID.
     * @param availabilityDays the nightly availability records to save.
     * At least one night is required.
     * @param imageUri the selected image's URI, or null to save without an upload.
     * @param onSuccess called after the campsite and its availability are saved.
     * @param onFailure receives validation, image upload, or Firestore errors.
     */
    fun createCampsite (
        campsite: Campsite,
        availabilityDays: List<AvailabilityDay>,
        imageUri: String? = null,
        onSuccess:() -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val maximumGuests = campsite.maxOccupancy ?: 0
        val validationError = listingValidationError(campsite, listingFields(campsite).keys) ?: when {
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
            saveCampsite(campsite, availabilityDays, onSuccess, onFailure)
            return
        }

        uploadCampsiteImage(campsite, imageUri, onSuccess = { url ->
            saveCampsite(
                campsite.copy(promoImageUrls = listOf(url)),
                availabilityDays,
                onSuccess,
                onFailure
            )
        }, onFailure = onFailure)
    }

    /**
     * Updates the selected listing fields on an existing campsite document.
     *
     * @param campsite the edited listing, including its original ID and owner.
     * @param editedFields the listing fields the owner changed in the form.
     * @param imageUri the selected replacement image URI, or null to skip uploading.
     * @param onSuccess called after the update succeeds, including an edit with no changes.
     * @param onFailure receives validation, upload, permission, or Firestore errors.
     */
    fun updateCampsite(
        campsite: Campsite,
        editedFields: Set<String>,
        imageUri: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fields = if (imageUri == null) editedFields else editedFields + "promoImageUrls"
        val listing = listingFields(campsite)

        val validationError = if (!listing.keys.containsAll(fields)) {
            "This update contains an invalid campsite field."
        } else {
            listingValidationError(campsite, fields)
        }
        if (validationError != null) {
            onFailure(IllegalArgumentException(validationError))
            return
        }

        val changes = listing.filterKeys { it in fields }
        if (changes.isEmpty()) {
            onSuccess()
            return
        }

        val campsiteDocument = firestore.collection("campsites").document(campsite.id)
        fun save(replacementUrl: String?) {
            val values = if (replacementUrl == null) {
                changes
            } else {
                changes + ("promoImageUrls" to listOf(replacementUrl))
            }
            campsiteDocument.update(values)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }

        if (imageUri == null) {
            save(null)
        } else {
            uploadCampsiteImage(campsite, imageUri, onSuccess = { url -> save(url) }, onFailure)
        }
    }

    private fun listingFields(campsite: Campsite): Map<String, Any?> = mapOf(
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
        "cancellationPolicy" to campsite.cancellationPolicy
    )

    /** Validates all listing fields on creation and the edited fields on update. */
    private fun listingValidationError(campsite: Campsite, fields: Set<String>): String? {
        val latitude = campsite.latitude
        val longitude = campsite.longitude
        val price = campsite.pricePerNight

        return when {
            campsite.id.isBlank() -> "Campsite ID must not be blank."
            campsite.ownerId.isBlank() -> "Owner ID must not be blank."
            "name" in fields && campsite.name.isBlank() -> "Please enter the campsite name."
            "description" in fields && campsite.description.isBlank() ->
                "Please enter the campsite description."
            "location" in fields && campsite.location.isBlank() ->
                "Please enter the campsite location."
            "latitude" in fields &&
                    (latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0) ->
                "Please enter a latitude between -90 and 90."
            "longitude" in fields &&
                    (longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0) ->
                "Please enter a longitude between -180 and 180."
            "maxOccupancy" in fields && (campsite.maxOccupancy ?: 0) <= 0 ->
                "Please enter a positive whole number for maximum occupancy."
            "pricePerNight" in fields && (price == null || !price.isFinite() || price < 0.0) ->
                "Please enter a valid price of 0 or more."
            "rules" in fields && campsite.rules.isBlank() ->
                "Please enter the campsite rules."
            "cancellationPolicy" in fields && campsite.cancellationPolicy.isBlank() ->
                "Please enter the cancellation policy."
            else -> null
        }
    }

    /** Uploads one image to a new Storage object and returns its download URL. */
    private fun uploadCampsiteImage(
        campsite: Campsite,
        imageUri: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val imageReference = FirebaseStorage.getInstance().reference
                .child("campsiteImages")
                .child(campsite.ownerId)
                .child(campsite.id)
                .child(UUID.randomUUID().toString())

            imageReference.putFile(Uri.parse(imageUri))
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw (task.exception ?: IllegalStateException("Image upload failed."))
                    }
                    imageReference.downloadUrl
                }
                .addOnSuccessListener { downloadUri -> onSuccess(downloadUri.toString()) }
                .addOnFailureListener { onFailure(it) }
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

        val campsiteData = listingFields(campsite) + mapOf(
            "id" to campsite.id,
            "ownerId" to campsite.ownerId,
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
     * Loads the campsite's saved nightly availability from Firestore once.
     *
     * @param campsiteId the ID of the campsite to load.
     * @param onSuccess receives the nights in date order, or an empty list
     * if the campsite has no saved nights.
     * @param onFailure receives the error if the read fails or a saved
     * availability record is invalid.
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