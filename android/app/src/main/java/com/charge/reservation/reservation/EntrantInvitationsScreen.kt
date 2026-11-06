package com.charge.reservation.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.model.Campsite
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

/**
 * Shows the entrant's unused invitations and starts the booking flow.
 */
@Composable
fun EntrantInvitationsScreen(
    entrantId: String,
    onBookingFinished: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }

    var lotteryIds by remember(entrantId) {
        mutableStateOf<List<String>>(emptyList())
    }
    var loading by remember(entrantId) { mutableStateOf(true) }
    var error by remember(entrantId) { mutableStateOf<String?>(null) }

    var selectedCampsite by remember { mutableStateOf<Campsite?>(null) }
    var selectedLotteryId by remember { mutableStateOf("") }
    var checkInText by remember { mutableStateOf("") }
    var checkOutText by remember { mutableStateOf("") }
    var guestText by remember { mutableStateOf("1") }
    var showBookingFlow by remember { mutableStateOf(false) }
    var loadingSelection by remember { mutableStateOf(false) }

    DisposableEffect(entrantId) {
        var active = true

        db.collection("lotteryParticipations")
            .whereEqualTo("entrantId", entrantId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (active) {
                    lotteryIds = snapshot.documents
                        .filter {
                            it.getString("status") == "INVITED" &&
                                it.getString("reservationId") == null
                        }
                        .mapNotNull { it.getString("lotteryId") }
                        .distinct()
                    loading = false
                }
            }
            .addOnFailureListener { exception ->
                if (active) {
                    error = exception.message ?: "Unable to load invitations."
                    loading = false
                }
            }

        onDispose { active = false }
    }

    val campsite = selectedCampsite
    val checkIn = runCatching { LocalDate.parse(checkInText) }.getOrNull()
    val checkOut = runCatching { LocalDate.parse(checkOutText) }.getOrNull()
    val guests = guestText.toIntOrNull()

    if (
        showBookingFlow &&
        campsite != null &&
        checkIn != null &&
        checkOut != null &&
        guests != null
    ) {
        ReservationBookingFlowScreen(
            campsite = campsite,
            lotteryId = selectedLotteryId,
            checkIn = checkIn,
            checkOut = checkOut,
            guestCount = guests,
            onBackClick = { showBookingFlow = false },
            onDoneClick = onBookingFinished
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My Lottery Invitations")

        if (loading || loadingSelection) {
            CircularProgressIndicator()
        }

        if (error != null) {
            Text(error ?: "")
        }

        if (!loading && error == null && lotteryIds.isEmpty()) {
            Text("You have no unused booking invitations.")
        }

        lotteryIds.forEach { lotteryId ->
            Button(
                enabled = !loadingSelection,
                onClick = {
                    loadingSelection = true
                    error = null

                    db.collection("lotteries")
                        .document(lotteryId)
                        .get()
                        .addOnSuccessListener { lottery ->
                            val campsiteId =
                                (lottery.get("campsiteIds") as? List<*>)
                                    ?.firstOrNull() as? String

                            if (!lottery.exists() || campsiteId == null) {
                                error = "Lottery campsite not found."
                                loadingSelection = false
                            } else {
                                db.collection("campsites")
                                    .document(campsiteId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        val found = document.toObject(
                                            Campsite::class.java
                                        )

                                        if (found == null ||
                                            found.removed
                                        ) {
                                            error = "Campsite unavailable."
                                        } else {
                                            selectedCampsite = found.copy(
                                                id = document.id
                                            )
                                            selectedLotteryId = lotteryId
                                            checkInText =
                                                lottery.getString(
                                                    "bookingStartDate"
                                                ).orEmpty()
                                            checkOutText =
                                                lottery.getString(
                                                    "bookingEndDate"
                                                ).orEmpty()
                                            guestText = "1"
                                        }
                                        loadingSelection = false
                                    }
                                    .addOnFailureListener {
                                        error = it.message
                                        loadingSelection = false
                                    }
                            }
                        }
                        .addOnFailureListener {
                            error = it.message
                            loadingSelection = false
                        }
                }
            ) {
                Text("Book invitation: ${lotteryId.take(8)}")
            }
        }

        if (campsite != null) {
            Text("Selected campsite: ${campsite.name}")

            OutlinedTextField(
                value = checkInText,
                onValueChange = { checkInText = it },
                label = { Text("Check-in (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = checkOutText,
                onValueChange = { checkOutText = it },
                label = { Text("Checkout (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = guestText,
                onValueChange = { guestText = it },
                label = { Text("Number of guests") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = checkIn != null &&
                    checkOut != null &&
                    checkOut.isAfter(checkIn) &&
                    guests != null &&
                    guests > 0,
                onClick = { showBookingFlow = true }
            ) {
                Text("Review campsite rules")
            }
        }
    }
}
