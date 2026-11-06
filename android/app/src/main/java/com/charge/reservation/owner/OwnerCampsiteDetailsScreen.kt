package com.charge.reservation.owner

import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import com.charge.reservation.model.Lottery
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.AvailabilityDay

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import com.charge.reservation.repository.LotteryEntrantRow

import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import java.time.LocalDate


/**
 * Displays saved campsite details, availability and lottery configuration.
 * Registration status uses the device clock and refreshes
 * once per second while the screen is composed.
 *
 * @param lotteries saved lotteries associated with this campsite.
 * @param isLoadingLotteries whether a lottery request is pending.
 * @param lotteriesError load failure message, or empty when no error exists.
 * @param onRetryLotteries requests another attempt after a failed load.
 */
@Composable
fun OwnerCampsiteDetailsScreen(
    campsite: Campsite?,
    availabilityDays: List<AvailabilityDay>,
    isLoadingAvailability: Boolean,
    availabilityError: String,
    lotteries: List<Lottery>,
    isLoadingLotteries: Boolean,
    lotteriesError: String,
    onRetryLotteries: () -> Unit,
    onRetryAvailability: () -> Unit,
    onCreateLotteryClick: () -> Unit,
    onUpdateWaitlistCapacity: (
        String,
        Int?,
        () -> Unit,
        (String) -> Unit
    ) -> Unit,
    onLoadLotteryEntrants: (
        String,
        (List<LotteryEntrantRow>) -> Unit,
        (String) -> Unit
    ) -> Unit,
    bookedReservations: List<Reservation>,
    isLoadingBookings: Boolean,
    bookingsError: String
    ) {
    var now by remember(campsite?.id) {
        mutableStateOf(Instant.now())
    }

    LaunchedEffect(campsite?.id) {
        while (isActive) {
            now = Instant.now()
            delay(1_000L)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() //keep content below status bar
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Campsite Details",
            fontSize = 28.sp
        )

        if (campsite == null) {
            Text("Campsite not found.")
            return@Column
        }

        if (isLoadingAvailability) {
            Text("Loading campsite details...")
            return@Column
        }

        if (availabilityError.isNotEmpty()) {
            Text("Can not load campsite details. Please try again.")

            Button(onClick = onRetryAvailability) {
                Text("Retry")
            }

            return@Column
        }

        Text(
            text = campsite.name,
            fontSize = 24.sp
        )

        Button(onClick = onCreateLotteryClick) {
            Text("Create lottery")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Location: ${campsite.location}")
                Text("Description: ${campsite.description}")

                Text(
                    "Maximum occupancy: " +
                            (campsite.maxOccupancy?.toString()
                                ?: "Not specified")
                )

                Text(
                    "Price per night: " +
                            (campsite.pricePerNight?.let {
                                "$" + String.format("%.2f", it)
                            } ?: "Not specified")
                )

                Text(
                    "Average rating: " +
                            (campsite.averageRating?.toString()
                                ?: "No ratings yet")
                )

                Text(
                    "Tags: " +
                            campsite.tags.joinToString()
                                .ifBlank { "None" }
                )

                Text("Rules: ${campsite.rules}")
                Text(
                    "Cancellation policy: " +
                            campsite.cancellationPolicy
                )

                Text(
                    "Map coordinates: " +
                            if (
                                campsite.latitude != null &&
                                campsite.longitude != null
                            ) {
                                "${campsite.latitude}, ${campsite.longitude}"
                            } else {
                                "Not provided"
                            }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nightly availability")

                if (availabilityDays.isEmpty()) {
                    Text("No availability specified.")
                } else {
                    availabilityDays.forEach { day ->
                        val capacity = day.guestCapacity ?: 0
                        val remaining = capacity - day.bookedGuests

                        Text("Night of ${day.date}")
                        Text("Total guest spaces: $capacity")
                        Text("Booked guests: ${day.bookedGuests}")
                        Text("Remaining spaces: $remaining")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Promotional Image")

                Spacer(modifier = Modifier.height(8.dp))

                //display the first promotional image if available.
                val imageUrl = campsite.promoImageUrls.firstOrNull()

                if (imageUrl.isNullOrBlank()) {
                    Text("No promotional image available")
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Image of ${campsite.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Text(
            text = "Booked dates",
            fontSize = 20.sp
        )

        when {
            isLoadingBookings -> {
                Text("Loading reservations...")
            }

            bookingsError.isNotEmpty() -> {
                Text(
                    bookingsError,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                val confirmedBookings = bookedReservations.filter {
                    it.status == ReservationStatus.CONFIRMED
                }

                val bookedDates = confirmedBookings
                    .flatMap { booking ->
                        val start = booking.checkInDate
                        val end = booking.checkOutDate

                        if (start == null || end == null ||
                            !end.isAfter(start)
                        ) {
                            emptyList<LocalDate>()
                        } else {
                            generateSequence(start) { date ->
                                date.plusDays(1).takeIf {
                                    it.isBefore(end)
                                }
                            }.toList()
                        }
                    }
                    .distinct()
                    .sorted()

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Currently booked nights")

                        if (bookedDates.isEmpty()) {
                            Text("No dates are currently booked.")
                        } else {
                            bookedDates.forEach { date ->
                                val bookingsOnDate = confirmedBookings.filter {
                                        booking ->
                                    val start = booking.checkInDate
                                    val end = booking.checkOutDate

                                    start != null && end != null &&
                                            !date.isBefore(start) &&
                                            date.isBefore(end)
                                }

                                Text(
                                    "$date — ${bookingsOnDate.size} reservation(s)"
                                )
                            }
                        }
                    }
                }

                Text(
                    "Reservations",
                    fontSize = 18.sp
                )

                if (bookedReservations.isEmpty()) {
                    Text("No reservations for this campsite.")
                } else {
                    bookedReservations.forEach { booking ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Reservation: ${booking.id}")
                                Text("Entrant: ${booking.entrantId}")

                                Text(
                                    "Check-in: " +
                                            (booking.checkInDate ?: "Not set")
                                )

                                Text(
                                    "Check-out: " +
                                            (booking.checkOutDate ?: "Not set")
                                )

                                Text(
                                    "Guests: " +
                                            (booking.guestCount ?: "Not set")
                                )

                                Text("Status: ${booking.status}")

                                if (!booking.statusChangeReason.isNullOrBlank()) {
                                    Text(
                                        "Reason: ${booking.statusChangeReason}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        Text(
            text = "Saved lotteries",
            fontSize = 20.sp
        )

        when {
            isLoadingLotteries -> {
                Text("Loading lotteries...")
            }

            lotteriesError.isNotEmpty() -> {
                Text(
                    text = lotteriesError,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = onRetryLotteries) {
                    Text("Retry")
                }
            }

            lotteries.isEmpty() -> {
                Text("No lotteries published for this campsite.")
            }

            else -> {

                lotteries.forEach { lottery ->
                    val opens = lottery.registrationStartAt
                    val closes = lottery.registrationEndAt

                    val registrationStatus = when {
                        opens == null || closes == null -> "Unavailable"
                        now.isBefore(opens) -> "Not open yet"
                        now.isBefore(closes) -> "Open"
                        else -> "Closed"
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Registration: $registrationStatus")

                            Text(
                                "Registration opens: " +
                                        lotteryDateTimeText(
                                            lottery.registrationStartAt,
                                            lottery.timeZone
                                        )
                            )

                            Text(
                                "Registration closes: " +
                                        lotteryDateTimeText(
                                            lottery.registrationEndAt,
                                            lottery.timeZone
                                        )
                            )

                            Text("Earliest check-in: ${lottery.bookingStartDate}")
                            Text("Latest checkout: ${lottery.bookingEndDate}")

                            Text(
                                "Booking deadline: " +
                                        lotteryDateTimeText(
                                            lottery.bookingDeadline,
                                            lottery.timeZone
                                        )
                            )

                            Text("Number of winners: ${lottery.winnerCount}")
                            Text(
                                "Waitlist capacity: " +
                                        (lottery.waitlistCapacity?.toString() ?: "Unlimited")
                            )

                            var showCapacityDialog by remember(lottery.id) {
                                mutableStateOf(false)
                            }

                            var capacityInput by remember(lottery.id) {
                                mutableStateOf(lottery.waitlistCapacity?.toString() ?: "")
                            }

                            var capacityError by remember(lottery.id) {
                                mutableStateOf("")
                            }

                            var isUpdatingCapacity by remember(lottery.id) {
                                mutableStateOf(false)
                            }

                            Button(
                                onClick = {
                                    capacityInput = lottery.waitlistCapacity?.toString() ?: ""
                                    capacityError = ""
                                    showCapacityDialog = true
                                }
                            ) {
                                Text("Edit capacity")
                            }

                            if (showCapacityDialog) {
                                val newCapacity = if (capacityInput.isBlank()) {
                                    null
                                } else {
                                    capacityInput.toIntOrNull()
                                }

                                val invalidCapacity =
                                    capacityInput.isNotBlank() &&
                                            (newCapacity == null || newCapacity <= 0)

                                AlertDialog(
                                    onDismissRequest = {
                                        if (!isUpdatingCapacity) {
                                            showCapacityDialog = false
                                        }
                                    },
                                    title = {
                                        Text("Edit waitlist capacity")
                                    },
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = capacityInput,
                                                onValueChange = {
                                                    capacityInput = it
                                                    capacityError = ""
                                                },
                                                label = { Text("Capacity") },
                                                placeholder = {
                                                    Text("Leave blank for unlimited")
                                                },
                                                isError = invalidCapacity,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                singleLine = true
                                            )

                                            Text("Blank means unlimited entrants.")

                                            if (invalidCapacity) {
                                                Text(
                                                    "Enter a positive whole number.",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }

                                            if (capacityError.isNotEmpty()) {
                                                Text(
                                                    capacityError,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            enabled = !invalidCapacity && !isUpdatingCapacity,
                                            onClick = {
                                                if (!isUpdatingCapacity) {
                                                    isUpdatingCapacity = true
                                                    capacityError = ""

                                                    onUpdateWaitlistCapacity(
                                                        lottery.id,
                                                        newCapacity,
                                                        {
                                                            isUpdatingCapacity = false
                                                            showCapacityDialog = false
                                                        },
                                                        { error ->
                                                            isUpdatingCapacity = false
                                                            capacityError = error
                                                        }
                                                    )
                                                }
                                            }
                                        ) {
                                            Text(if (isUpdatingCapacity) "Saving..." else "Save")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            enabled = !isUpdatingCapacity,
                                            onClick = { showCapacityDialog = false }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    var showEntrants by remember(lottery.id) {
                        mutableStateOf(false)
                    }

                    var entrants by remember(lottery.id) {
                        mutableStateOf<List<LotteryEntrantRow>>(emptyList())
                    }

                    var loadingEntrants by remember(lottery.id) {
                        mutableStateOf(false)
                    }

                    var entrantsError by remember(lottery.id) {
                        mutableStateOf("")
                    }

                    Button(
                        onClick = {
                            if (showEntrants) {
                                showEntrants = false
                            } else {
                                showEntrants = true
                                loadingEntrants = true
                                entrantsError = ""

                                onLoadLotteryEntrants(
                                    lottery.id,
                                    { loaded ->
                                        entrants = loaded
                                        loadingEntrants = false
                                    },
                                    { error ->
                                        entrantsError = error
                                        loadingEntrants = false
                                    }
                                )
                            }
                        }
                    ) {
                        Text(if (showEntrants) "Hide entrants" else "View entrants")
                    }

                    if (showEntrants) {
                        when {
                            loadingEntrants -> Text("Loading entrants...")

                            entrantsError.isNotEmpty() -> {
                                Text(
                                    entrantsError,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            else -> {
                                val activeEntrants = entrants.count {
                                    it.status != "WITHDRAWN"
                                }

                                Text("Active entrants: $activeEntrants")
                                Text("Total participation records: ${entrants.size}")

                                if (entrants.isEmpty()) {
                                    Text("No entrants have joined this lottery.")
                                } else {
                                    entrants.forEach { entrant ->
                                        Text(
                                            "Entrant: ${entrant.entrantId}"
                                        )
                                        Text(
                                            "Status: ${entrant.status}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

/**
 * Formats a saved instant in the lottery's stored time zone.
 *
 * @return date, time and zone ID, or "Not specified" for missing values.
 */
private fun lotteryDateTimeText(
    instant: Instant?,
    timeZone: ZoneId?
): String {
    if (instant == null || timeZone == null) {
        return "Not specified"
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm VV")
    return instant.atZone(timeZone).format(formatter)
}