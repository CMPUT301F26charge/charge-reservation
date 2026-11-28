package com.charge.reservation.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.InputChip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.AvailabilityDay

/**
 * Collects a new campsite listing, its nightly availability, and an optional image.
 *
 * @param ownerId the owner's Firebase Authentication UID.
 * @param campsiteId the ID reserved for this listing.
 * @param isSaving whether a save is in progress.
 * @param errorMessage the save error, or an empty string.
 * @param onCreateCampsite receives the listing, nights, and optional image URI.
 */
@Composable
fun CreateCampsiteScreen(
    ownerId: String,
    campsiteId: String,
    isSaving: Boolean,
    errorMessage: String,
    onCreateCampsite: (Campsite, List<AvailabilityDay>, String?) -> Unit
) {
    CampsiteListingForm(ownerId, campsiteId, null, isSaving, errorMessage) { campsite, nights, imageUri, _ ->
        onCreateCampsite(campsite, nights, imageUri)
    }
}

/**
 * Prefills the listing form using the owner's saved campsite.
 *
 * @param campsite the active campsite being edited.
 * @param isSaving whether to disable inputs while saving.
 * @param errorMessage the save error to display without discarding edits.
 * @param onSaveCampsite receives the edited listing, the fields the owner changed,
 * and the optional URI of a replacement promotional image.
 */
@Composable
fun EditCampsiteScreen(
    campsite: Campsite,
    isSaving: Boolean,
    errorMessage: String,
    onSaveCampsite: (Campsite, Set<String>, String?) -> Unit
) {
    CampsiteListingForm(campsite.ownerId, campsite.id, campsite, isSaving, errorMessage) { listing, _, imageUri, fields ->
        onSaveCampsite(listing, fields, imageUri)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampsiteListingForm(
    ownerId: String,
    campsiteId: String,
    savedCampsite: Campsite?,
    isSaving: Boolean,
    errorMessage: String,
    onSubmit: (Campsite, List<AvailabilityDay>, String?, Set<String>) -> Unit
) {
    val isEditing = savedCampsite != null
    var name by rememberSaveable(campsiteId) { mutableStateOf(savedCampsite?.name.orEmpty()) }
    var description by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.description.orEmpty())
    }
    var location by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.location.orEmpty())
    }
    var latitude by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.latitude?.toString().orEmpty())
    }
    var longitude by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.longitude?.toString().orEmpty())
    }
    var maxOccupancy by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.maxOccupancy?.toString().orEmpty())
    }
    var pricePerNight by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.pricePerNight?.toString().orEmpty())
    }
    var tagInput by rememberSaveable(campsiteId) { mutableStateOf("") }
    var tags by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.tags.orEmpty())
    }
    var rules by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.rules.orEmpty())
    }
    var cancellationPolicy by rememberSaveable(campsiteId) {
        mutableStateOf(savedCampsite?.cancellationPolicy.orEmpty())
    }
    var editedFields by rememberSaveable(campsiteId) { mutableStateOf(emptyList<String>()) }
    var removeSavedImage by rememberSaveable(campsiteId) { mutableStateOf(false) }

    fun markEdited(field: String) {
        if (isEditing && field !in editedFields) {
            editedFields = editedFields + field
        }
    }

    var showCalendar by rememberSaveable(campsiteId) {
        mutableStateOf(false)
    }
    var availableNights by rememberSaveable(campsiteId) {
        mutableStateOf<List<String>>(emptyList())
    }
    var nightlyCapacity by rememberSaveable(campsiteId) {
        mutableStateOf(hashMapOf<String, String>())
    }
    var selectedImageUri by rememberSaveable(campsiteId) {
        mutableStateOf<String?>(null)
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            markEdited("promoImageUrls")
        }
    }
    val savedImageUrls = savedCampsite?.promoImageUrls.orEmpty()
    val previewImage = selectedImageUri ?: savedImageUrls.firstOrNull()
        ?.takeUnless { removeSavedImage }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        Text(
            text = if (isEditing) "Edit campsite" else "Register a campsite",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                markEdited("name")
            },
            label = { Text("Campsite name") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                markEdited("description")
            },
            label = { Text("Description") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Promotional image (up to 10 MiB)")

        Button(
            enabled = !isSaving,
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text(
                if (previewImage == null) {
                    "Choose image"
                } else {
                    "Change image"
                }
            )
        }

        previewImage?.let { image ->
            AsyncImage(
                model = image,
                contentDescription = "Campsite promotional image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            TextButton(
                enabled = !isSaving,
                onClick = {
                    selectedImageUri = null
                    removeSavedImage = true
                    markEdited("promoImageUrls")
                }
            ) {
                Text("Remove image")
            }
        }

        OutlinedTextField(
            value = location,
            onValueChange = {
                location = it
                markEdited("location")
            },
            label = { Text("Location") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latitude,
            onValueChange = {
                latitude = it
                markEdited("latitude")
            },
            label = { Text("Latitude") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = longitude,
            onValueChange = {
                longitude = it
                markEdited("longitude")
            },
            label = { Text("Longitude") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = {
                maxOccupancy = it
                markEdited("maxOccupancy")
            },
            label = { Text("Maximum occupancy") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pricePerNight,
            onValueChange = {
                pricePerNight = it
                markEdited("pricePerNight")
            },
            label = { Text("Price per night") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isEditing) {
            Button(
                enabled = !isSaving,
                onClick = { showCalendar = true }
            ) {
                Text("Add available nights")
            }

            if (availableNights.isNotEmpty()) {
                Text("Nightly guest capacity")

                availableNights.forEach { night ->
                    val maximum = maxOccupancy.trim().toIntOrNull()
                    val input = nightlyCapacity[night] ?: maxOccupancy.trim()
                    val capacity = input.trim().toIntOrNull()

                    OutlinedTextField(
                        value = input,
                        onValueChange = { newValue ->
                            nightlyCapacity =
                                HashMap(nightlyCapacity).apply {
                                    put(night, newValue)
                                }
                        },
                        label = { Text("Guest capacity on $night") },
                        supportingText = {
                            Text(
                                if (maximum == null || maximum <= 0) {
                                    "Enter a positive maximum occupancy first."
                                } else {
                                    "Enter 0 to $maximum guests. 0 means unavailable."
                                }
                            )
                        },
                        isError = maximum == null ||
                                maximum <= 0 ||
                                capacity == null ||
                                capacity !in 0..maximum,
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        enabled = !isSaving,
                        onClick = {
                            availableNights = availableNights - night
                            nightlyCapacity =
                                HashMap(nightlyCapacity).apply {
                                    remove(night)
                                }
                        }
                    ) {
                        Text("Remove $night")
                    }
                }
            }
        }

        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            label = { Text("Tag") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            enabled = !isSaving && tagInput.isNotBlank(),
            onClick = {
                val enterTag = tagInput.trim()

                if (enterTag.isNotBlank() && enterTag !in tags) {
                    tags = tags + enterTag
                    markEdited("tags")
                }

                tagInput = ""
            }
        ) {
            Text("Add tag")
        }

        if (tags.isNotEmpty()) {
            Text("Added tags (tap to remove)")
        }

        tags.forEach { tag ->
            InputChip(
                selected = true,
                enabled = !isSaving,
                onClick = {
                    tags = tags - tag
                    markEdited("tags")
                },
                label = {Text(tag)},
                trailingIcon = {Text("x")}
            )
        }

        OutlinedTextField(
            value = rules,
            onValueChange = {
                rules = it
                markEdited("rules")
            },
            label = { Text("Rules") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cancellationPolicy,
            onValueChange = {
                cancellationPolicy = it
                markEdited("cancellationPolicy")
            },
            label = { Text("Cancellation policy") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        if(errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !isSaving,
            onClick = {
                val enterTags = (tags + tagInput.trim()).filter { it.isNotEmpty() }.distinct()
                val campsite = Campsite(
                    id = campsiteId,
                    ownerId = ownerId,
                    name = name.trim(),
                    description = description.trim(),
                    location = location.trim(),
                    latitude = latitude.trim().toDoubleOrNull(),
                    longitude = longitude.trim().toDoubleOrNull(),
                    maxOccupancy = maxOccupancy.trim().toIntOrNull(),
                    pricePerNight = pricePerNight.trim().toDoubleOrNull(),
                    tags = enterTags,
                    promoImageUrls = if (removeSavedImage) emptyList() else savedImageUrls,
                    rules = rules.trim(),
                    cancellationPolicy = cancellationPolicy.trim()
                )

                val availabilityDays = availableNights.map { night ->
                    val input = nightlyCapacity[night] ?: maxOccupancy.trim()

                    AvailabilityDay(
                        campsiteId = campsiteId,
                        date = LocalDate.parse(night),
                        guestCapacity = input.trim().toIntOrNull(),
                        bookedGuests = 0
                    )
                }
                if (tagInput.isNotBlank() && enterTags != tags) {
                    markEdited("tags")
                }
                onSubmit(campsite, availabilityDays, selectedImageUri, editedFields.toSet())
            }
        ) {
            Text(
                when {
                    isSaving -> "Saving..."
                    isEditing -> "Save changes"
                    else -> "Create Campsite"
                }
            )
        }

        if (showCalendar) {
            val pickerState = rememberDateRangePickerState()

            DatePickerDialog(
                onDismissRequest = { showCalendar = false },
                confirmButton = {
                    TextButton(
                        enabled = !isSaving &&
                                pickerState.selectedStartDateMillis != null,
                        onClick = {
                            val startMillis = pickerState.selectedStartDateMillis

                            if (startMillis != null) {
                                val endMillis =
                                    pickerState.selectedEndDateMillis ?: startMillis

                                val firstNight = Instant.ofEpochMilli(startMillis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()

                                val lastNight = Instant.ofEpochMilli(endMillis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()

                                val addedNights = mutableListOf<String>()
                                var night = firstNight

                                while (!night.isAfter(lastNight)) {
                                    addedNights.add(night.toString())
                                    night = night.plusDays(1)
                                }

                                availableNights = (availableNights + addedNights)
                                    .distinct()
                                    .sorted()

                                showCalendar = false
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendar = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = pickerState,
                    modifier = Modifier.heightIn(max = 480.dp)
                )
            }
        }
    }
}