package com.charge.reservation.entrant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import com.charge.reservation.repository.CampsiteRepository
import com.charge.reservation.ui.CampsiteCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ExploreCampsitesScreen(
    navController: NavController,
    onCampsiteClick: (Campsite) -> Unit
) {
    val repository = remember { CampsiteRepository() }

    var campsites by remember { mutableStateOf<List<Campsite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var searchText by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var locationText by rememberSaveable { mutableStateOf("") }
    var checkInText by rememberSaveable { mutableStateOf("") }
    var checkOutText by rememberSaveable { mutableStateOf("") }
    var guestsText by rememberSaveable { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf("") }

    // Only valid filters are applied to the results.
    var activeLocation by rememberSaveable { mutableStateOf("") }
    var activeCheckIn by rememberSaveable { mutableStateOf("") }
    var activeCheckOut by rememberSaveable { mutableStateOf("") }
    var activeGuests by rememberSaveable { mutableStateOf("") }
    var filterVersion by remember { mutableIntStateOf(0) }

    var availabilityById by remember {
        mutableStateOf<Map<String, List<AvailabilityDay>>>(emptyMap())
    }
    var isLoadingAvailability by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf("") }

    DisposableEffect(repository) {
        val listener = repository.listenToActiveCampsites(
            onSuccess = { result ->
                campsites = result
                isLoading = false
                errorMessage = ""
            },
            onFailure = {
                errorMessage = "Could not load campsites. Please try again."
                isLoading = false
            }
        )
        onDispose { listener.remove() }
    }

    val campsiteIds = campsites.map { it.id }

    // Availability is stored separately under each campsite in Firestore.
    // Only load it when both dates have been applied.
    DisposableEffect(activeCheckIn, activeCheckOut, activeGuests, campsiteIds, filterVersion) {
        if (activeCheckIn.isBlank() || activeCheckOut.isBlank() || campsiteIds.isEmpty()) {
            availabilityById = emptyMap()
            isLoadingAvailability = false
            availabilityError = ""
            onDispose { }
        } else {
            var active = true
            var remaining = campsiteIds.size
            val loaded = mutableMapOf<String, List<AvailabilityDay>>()

            availabilityById = emptyMap()
            availabilityError = ""
            isLoadingAvailability = true

            campsiteIds.forEach { campsiteId ->
                repository.getAvailability(
                    campsiteId = campsiteId,
                    onSuccess = { days ->
                        if (active) {
                            loaded[campsiteId] = days
                            remaining--
                            if (remaining == 0) {
                                availabilityById = loaded.toMap()
                                isLoadingAvailability = false
                            }
                        }
                    },
                    onFailure = {
                        if (active) {
                            availabilityError =
                                "Could not check available dates. Please check your internet connection and try again."
                            remaining--
                            if (remaining == 0) isLoadingAvailability = false
                        }
                    }
                )
            }
            onDispose { active = false }
        }
    }

    val normalizedSearch = searchText.trim().replace(Regex("\\s+"), " ")
    val normalizedLocation = activeLocation.trim().replace(Regex("\\s+"), " ")
    val requestedGuests = activeGuests.toIntOrNull()
    val checkIn = activeCheckIn.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    val checkOut = activeCheckOut.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

    val filteredCampsites = campsites.filter { campsite ->
        val matchesName = campsite.name.trim().replace(Regex("\\s+"), " ")
            .contains(normalizedSearch, ignoreCase = true)
        val matchesLocation = campsite.location.trim().replace(Regex("\\s+"), " ")
            .contains(normalizedLocation, ignoreCase = true)
        val matchesGuests = requestedGuests == null ||
                (campsite.maxOccupancy ?: 0) >= requestedGuests

        val matchesDates = if (checkIn == null || checkOut == null) {
            true
        } else {
            val days = availabilityById[campsite.id].orEmpty()
            val neededGuests = requestedGuests ?: 1
            val nights = ChronoUnit.DAYS.between(checkIn, checkOut)

            (0L until nights).all { offset ->
                val requestedDate = checkIn.plusDays(offset)
                days.any { day ->
                    day.date == requestedDate &&
                            (day.guestCapacity ?: 0) - day.bookedGuests >= neededGuests
                }
            }
        }

        matchesName && matchesLocation && matchesGuests && matchesDates
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Explore campsites",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search campsites") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    TextButton(onClick = { searchText = "" }) { Text("Clear") }
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { showFilters = !showFilters }) {
                Text(if (showFilters) "Hide filters" else "Filters")
            }
            TextButton(onClick = {
                searchText = ""
                locationText = ""
                checkInText = ""
                checkOutText = ""
                guestsText = ""
                activeLocation = ""
                activeCheckIn = ""
                activeCheckOut = ""
                activeGuests = ""
                validationMessage = ""
                showFilters = false
            }) {
                Text("Clear filters")
            }
        }

        if (showFilters) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = checkInText,
                    onValueChange = { checkInText = it },
                    label = { Text("Check-in (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = checkOutText,
                    onValueChange = { checkOutText = it },
                    label = { Text("Check-out (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = guestsText,
                    onValueChange = { guestsText = it },
                    label = { Text("Number of guests") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val guests = guestsText.trim().toIntOrNull()
                    val startText = checkInText.trim()
                    val endText = checkOutText.trim()
                    val start = runCatching { LocalDate.parse(startText) }.getOrNull()
                    val end = runCatching { LocalDate.parse(endText) }.getOrNull()

                    validationMessage = when {
                        guestsText.isNotBlank() && (guests == null || guests <= 0) ->
                            "Guest count must be a positive whole number."
                        startText.isBlank() != endText.isBlank() ->
                            "Enter both check-in and check-out dates."
                        startText.isNotBlank() && (start == null || end == null) ->
                            "Enter valid dates in YYYY-MM-DD format."
                        start != null && end != null && !end.isAfter(start) ->
                            "Check-out must be after check-in."
                        else -> ""
                    }

                    if (validationMessage.isEmpty()) {
                        activeLocation = locationText.trim()
                        activeCheckIn = start?.toString().orEmpty()
                        activeCheckOut = end?.toString().orEmpty()
                        activeGuests = guests?.toString().orEmpty()
                        filterVersion++
                        showFilters = false
                    }
                }) {
                    Text("Apply filters")
                }
            }
        }

        if (validationMessage.isNotEmpty()) {
            Text(validationMessage, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage.isNotEmpty() -> Text(errorMessage, color = Color.Black)
            campsites.isEmpty() -> Text("No campsites available.", color = Color.Black)
            isLoadingAvailability -> CircularProgressIndicator()
            availabilityError.isNotEmpty() -> Text(availabilityError, color = Color.Black)
            filteredCampsites.isEmpty() -> Text(
                "No campsites match your search or filters.",
                color = Color.Black
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCampsites, key = { it.id }) { campsite ->
                        CampsiteCard(
                            campsite = campsite,
                            onClick = { onCampsiteClick(campsite) }
                        )
                    }
                }
            }
        }
    }
}
