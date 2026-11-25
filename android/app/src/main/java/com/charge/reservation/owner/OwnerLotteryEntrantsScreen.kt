package com.charge.reservation.owner

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.LotteryActionException
import com.charge.reservation.LotteryParticipationRepository
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

@Composable
fun OwnerLotteryEntrantsScreen(
    lotteryId: String,
    ownerId: String
) {
    val repository = remember { LotteryParticipationRepository() }
    var entrants by remember(lotteryId, ownerId) {
        mutableStateOf<List<LotteryParticipation>?>(null)
    }
    var loadError by remember(lotteryId, ownerId) {
        mutableStateOf<String?>(null)
    }
    var retryCount by remember(lotteryId, ownerId) { mutableIntStateOf(0) }

    LaunchedEffect(lotteryId, ownerId, retryCount) {
        entrants = null
        loadError = null

        try {
            repository.ownerLotteryEntrants(lotteryId, ownerId).collect { updatedEntrants ->
                entrants = updatedEntrants
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("OwnerLotteryEntrants", "Could not load lottery entrants", exception)
            loadError = (exception as? LotteryActionException)?.message
                ?: "Could not load lottery entrants. Please try again."
        }
    }

    val currentEntrants = entrants
    val currentError = loadError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lottery entrants", style = MaterialTheme.typography.headlineSmall)
        Text("LOT-${lotteryId.take(6).uppercase()}")

        when {
            currentError != null -> {
                Text(currentError, color = MaterialTheme.colorScheme.error)
                Button(onClick = { retryCount++ }) {
                    Text("Retry")
                }
            }

            currentEntrants == null -> CircularProgressIndicator()

            else -> {
                Text("Entrants: ${currentEntrants.size}")

                if (currentEntrants.isEmpty()) {
                    Text("No entrants in this lottery.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currentEntrants, key = { it.entrantId }) { entrant ->
                            val name = entrant.entrantDisplayName
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                                ?: "Entrant ${entrant.entrantId.takeLast(6)}"

                            val status = when (entrant.status) {
                                ParticipationStatus.WAITING -> "Waiting for draw"
                                ParticipationStatus.WITHDRAWN -> "Withdrawn"
                                ParticipationStatus.NOT_SELECTED -> "Not selected"
                                ParticipationStatus.INVITED -> "Invited to book"
                                ParticipationStatus.DECLINED -> "Invitation declined"
                                ParticipationStatus.USED -> "Booked"
                                ParticipationStatus.EXPIRED -> "Invitation expired"
                                ParticipationStatus.CANCELLED -> "Cancelled"
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(name, style = MaterialTheme.typography.titleMedium)
                                    Text(status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}