package com.charge.reservation.entrant

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.LotteryParticipationRepository
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.repository.LotteryLookupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Displays the entrant's lottery entries and their current statuses.
 * The list updates as entries change. Each entry shows the campsite name
 * when available and uses the lottery code as a fallback.
 * Entrants can leave a waitlist while their entry is waiting for the draw.
 *
 * @param entrantId the Firebase Authentication UID of the signed-in entrant.
 * @param onOpenLottery receives the lottery ID when the entrant taps an entry.
 */
@Composable
fun EntrantLotteriesScreen(
    entrantId: String,
    onOpenLottery: (String) -> Unit
) {
    val participationRepository = remember { LotteryParticipationRepository() }
    val lotteryRepository = remember { LotteryLookupRepository() }
    val scope = rememberCoroutineScope()

    var entries by remember(entrantId) {
        mutableStateOf<List<LotteryParticipation>?>(null)
    }
    var titles by remember(entrantId) {
        mutableStateOf<Map<String, String?>>(emptyMap())
    }
    var loadFailed by remember(entrantId) { mutableStateOf(false) }
    var retryCount by remember(entrantId) { mutableIntStateOf(0) }
    var leavingLotteryId by remember(entrantId) { mutableStateOf<String?>(null) }
    var leaveError by remember(entrantId) { mutableStateOf<String?>(null) }

    DisposableEffect(entrantId, retryCount) {
        entries = null
        loadFailed = false

        val listener = participationRepository.listenForEntrant(
            entrantId = entrantId,
            onSuccess = { updatedEntries ->
                entries = updatedEntries
                loadFailed = false
            },
            onFailure = { exception ->
                Log.e("EntrantLotteries", "Could not load lottery entries", exception)
                loadFailed = true
            }
        )

        onDispose { listener.remove() }
    }

    val loadedEntries = entries
    val lotteryIds = loadedEntries.orEmpty().map { it.lotteryId }.toSet()

    LaunchedEffect(entrantId, lotteryIds, retryCount) {
        for (lotteryId in lotteryIds) {
            if (titles.containsKey(lotteryId)) continue

            try {
                val title = lotteryRepository.getLotteryTitle(lotteryId)
                titles = titles + (lotteryId to title)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("EntrantLotteries", "Could not load campsite name", exception)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My lotteries", style = MaterialTheme.typography.headlineSmall)

        leaveError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        when {
            loadFailed -> {
                Text("Could not load your lottery entries.")
                TextButton(onClick = { retryCount++ }) {
                    Text("Retry")
                }
            }

            loadedEntries == null -> CircularProgressIndicator()
            loadedEntries.isEmpty() -> Text("You haven't joined a lottery yet.")

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(loadedEntries) { entry ->
                    val lotteryCode = "LOT-${entry.lotteryId.take(6).uppercase()}"
                    val title = titles[entry.lotteryId]
                    val status = when (entry.status) {
                        ParticipationStatus.WAITING -> "Waiting for draw"
                        ParticipationStatus.WITHDRAWN -> "Withdrawn"
                        ParticipationStatus.NOT_SELECTED -> "Not selected"
                        ParticipationStatus.INVITED -> "Won"
                        ParticipationStatus.DECLINED -> "Invitation declined"
                        ParticipationStatus.USED -> "Booked"
                        ParticipationStatus.EXPIRED -> "Invitation expired"
                        ParticipationStatus.CANCELLED -> "Cancelled"
                    }

                    Card(
                        onClick = { onOpenLottery(entry.lotteryId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = title ?: lotteryCode,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (title != null) {
                                Text(lotteryCode, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(status)

                            if (entry.status == ParticipationStatus.WAITING) {
                                TextButton(
                                    enabled = leavingLotteryId == null,
                                    onClick = {
                                        leavingLotteryId = entry.lotteryId
                                        leaveError = null

                                        scope.launch {
                                            try {
                                                participationRepository.leave(
                                                    entry.lotteryId,
                                                    entrantId
                                                )
                                            } catch (exception: CancellationException) {
                                                throw exception
                                            } catch (exception: Exception) {
                                                Log.e(
                                                    "EntrantLotteries",
                                                    "Could not leave the waitlist",
                                                    exception
                                                )
                                                leaveError = "Could not leave the waitlist. Try again."
                                            } finally {
                                                leavingLotteryId = null
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        if (leavingLotteryId == entry.lotteryId) {
                                            "Leaving..."
                                        } else {
                                            "Leave waitlist"
                                        }
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