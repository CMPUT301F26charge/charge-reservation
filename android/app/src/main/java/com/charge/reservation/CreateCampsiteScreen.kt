package com.charge.reservation

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
 * Collects initial campsite details and passes them to the caller.
 *
 * The caller supplies a stable campsite ID, saves the listing,
 * and provides saving progress and error messages.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampsiteScreen(
    ownerId: String,
    campsiteId: String,
    isSaving: Boolean,
    errorMessage: String,
    onCreateCampsite: (Campsite, List<AvailabilityDay>) -> Unit
) {
    var name by rememberSaveable(campsiteId) { mutableStateOf("") }
    var description by rememberSaveable(campsiteId) { mutableStateOf("") }
    var location by rememberSaveable(campsiteId) { mutableStateOf("") }
    var latitude by rememberSaveable(campsiteId) { mutableStateOf("") }
    var longitude by rememberSaveable(campsiteId) { mutableStateOf("") }
    var maxOccupancy by rememberSaveable(campsiteId) { mutableStateOf("") }
    var pricePerNight by rememberSaveable(campsiteId) { mutableStateOf("") }
    var tagInput by rememberSaveable(campsiteId) { mutableStateOf("") }
    var tags by rememberSaveable(campsiteId) { mutableStateOf<List<String>>(emptyList()) }
    var rules by rememberSaveable(campsiteId) { mutableStateOf("") }
    var cancellationPolicy by rememberSaveable(campsiteId) { mutableStateOf("") }

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
        }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        Text(
            text = "Register a campsite",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Campsite name") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Promotional image")

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
                if (selectedImageUri == null) {
                    "Choose image"
                } else {
                    "Change image"
                }
            )
        }

        selectedImageUri?.let { imageUri ->
            AsyncImage(
                model = imageUri,
                contentDescription = "Selected campsite promotional image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            TextButton(
                enabled = !isSaving,
                onClick = { selectedImageUri = null }
            ) {
                Text("Remove image")
            }
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Latitude") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Longitude") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = { maxOccupancy = it },
            label = { Text("Maximum occupancy") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pricePerNight,
            onValueChange = { pricePerNight = it },
            label = { Text("Price per night") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

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
                },
                label = {Text(tag)},
                trailingIcon = {Text("x")}
            )
        }

        OutlinedTextField(
            value = rules,
            onValueChange = { rules = it },
            label = { Text("Rules") },
            minLines = 2,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cancellationPolicy,
            onValueChange = { cancellationPolicy = it },
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
                onCreateCampsite(campsite, availabilityDays)
            }
        ) {
            Text(if (isSaving) "Saving..." else "Create Campsite")
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