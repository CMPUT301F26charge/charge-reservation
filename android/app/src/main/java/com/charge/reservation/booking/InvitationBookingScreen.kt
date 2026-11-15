package com.charge.reservation.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val nightFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val deadlineFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z")

/**
 * Stateful entry point. Navigate here when the user taps "Open invitation":
 *
 *     navController.navigate("invitation/$lotteryId")
 */
@Composable
fun InvitationBookingScreen(
    lotteryId: String,
    onBack: () -> Unit,
    onBooked: (reservationId: String) -> Unit,
    viewModel: InvitationBookingViewModel = viewModel(
        key = "invitation-$lotteryId",
        factory = InvitationBookingViewModel.factory(lotteryId)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InvitationBookingContent(
        state = state,
        onBack = onBack,
        onDone = onBooked,
        onRetry = viewModel::load,
        onSelectCampsite = viewModel::selectCampsite,
        onGuestCountChange = viewModel::setGuestCount,
        onNightTapped = viewModel::onNightTapped,
        onClearSelection = viewModel::clearSelection,
        onBook = viewModel::book,
        onMessageShown = viewModel::messageShown
    )
}

/** Stateless UI: easy to test with createComposeRule() and a hand-built [BookingUiState]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationBookingContent(
    state: BookingUiState,
    onBack: () -> Unit,
    onDone: (reservationId: String) -> Unit,
    onRetry: () -> Unit,
    onSelectCampsite: (String) -> Unit,
    onGuestCountChange: (Int) -> Unit,
    onNightTapped: (LocalDate) -> Unit,
    onClearSelection: () -> Unit,
    onBook: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your invitation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.showForm) {
                BookingBar(state = state, onClearSelection = onClearSelection, onBook = onBook)
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.fatalError != null -> MessageView(
                    text = state.fatalError,
                    primaryLabel = "Try again",
                    onPrimary = onRetry,
                    secondaryLabel = "Back",
                    onSecondary = onBack
                )

                state.confirmation != null -> ConfirmationView(state.confirmation, onDone)

                else -> BookingForm(
                    state = state,
                    onSelectCampsite = onSelectCampsite,
                    onGuestCountChange = onGuestCountChange,
                    onNightTapped = onNightTapped
                )
            }
        }
    }
}

@Composable
private fun BookingForm(
    state: BookingUiState,
    onSelectCampsite: (String) -> Unit,
    onGuestCountChange: (Int) -> Unit,
    onNightTapped: (LocalDate) -> Unit
) {
    val details = state.details ?: return
    val selectedDates = state.selectedNights.map { it.date }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        details.lottery.bookingDeadline?.let { deadline ->
            item {
                val zone = details.lottery.timeZone ?: ZoneId.systemDefault()
                Text(
                    "Book by ${deadline.atZone(zone).format(deadlineFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (details.campsites.size > 1) {
            item {
                Column {
                    Text("Campsite", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        details.campsites.forEach { campsite ->
                            FilterChip(
                                selected = campsite.id == state.selectedCampsiteId,
                                onClick = { onSelectCampsite(campsite.id) },
                                label = { Text(campsite.name) }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Text(details.campsites.first().name, style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Guests", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onGuestCountChange(state.guestCount - 1) },
                        enabled = state.guestCount > 1,
                        modifier = Modifier.semantics { contentDescription = "Fewer guests" }
                    ) { Text("−") }
                    Text(
                        "${state.guestCount}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OutlinedButton(
                        onClick = { onGuestCountChange(state.guestCount + 1) },
                        enabled = state.guestCount < state.maxGuests,
                        modifier = Modifier.semantics { contentDescription = "More guests" }
                    ) { Text("+") }
                }
            }
        }

        item {
            Column {
                Text("Choose your nights", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Tap your first night, then tap your last night.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            state.nightsLoading && state.nights.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.nights.isEmpty() -> item {
                Text(
                    "This campsite has no nights open for your invitation.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> items(state.nights, key = { it.date.toString() }) { night ->
                NightRow(
                    night = night,
                    bookable = state.isBookable(night),
                    selected = night.date in selectedDates,
                    onClick = { onNightTapped(night.date) }
                )
            }
        }
    }
}

@Composable
private fun NightRow(
    night: NightOption,
    bookable: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        bookable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val label = night.date.format(nightFormatter)

    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected; contentDescription = "Night of $label" }
            .clickable(enabled = bookable, onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (bookable) "${night.remaining} spots left" else "Unavailable",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BookingBar(
    state: BookingUiState,
    onClearSelection: () -> Unit,
    onBook: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val checkIn = state.checkIn
            val checkOut = state.checkOut
            if (checkIn != null && checkOut != null) {
                val nights = state.selectedNights.size
                Text(
                    "${checkIn.format(fullDateFormatter)} to ${checkOut.format(fullDateFormatter)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    buildString {
                        append("$nights ${if (nights == 1) "night" else "nights"}, ${state.guestCount} ${if (state.guestCount == 1) "guest" else "guests"}")
                        state.estimatedTotal?.let { append(", about ${NumberFormat.getCurrencyInstance().format(it)}") }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Select your nights to continue.", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (checkIn != null) {
                    TextButton(onClick = onClearSelection, enabled = !state.submitting) { Text("Clear") }
                }
                Button(
                    onClick = onBook,
                    enabled = checkIn != null && !state.submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Book these nights")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationView(confirmation: BookingConfirmation, onDone: (String) -> Unit) {
    MessageView(
        title = "Booking confirmed",
        text = "${confirmation.campsiteName}\n" +
                "${confirmation.checkIn.format(fullDateFormatter)} to ${confirmation.checkOut.format(fullDateFormatter)}\n" +
                "${confirmation.guestCount} ${if (confirmation.guestCount == 1) "guest" else "guests"}",
        primaryLabel = "Done",
        onPrimary = { onDone(confirmation.reservationId) }
    )
}

@Composable
private fun MessageView(
    text: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    title: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        title?.let {
            Text(it, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPrimary) { Text(primaryLabel) }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}