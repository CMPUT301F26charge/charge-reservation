package com.charge.reservation.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery
import com.charge.reservation.repository.LotteryRepository

/**
 * Lists an owner's created and shared lotteries with links to their own campsites.
 *
 * @param ownerId the signed-in owner whose lotteries are loaded.
 * @param campsites the owner's active campsite listings.
 * @param repository loads the saved lotteries or reports an error.
 * @param onCampsiteClick opens the details of one of the owner's campsites.
 */
@Composable
fun OwnerLotteriesScreen(
    ownerId: String,
    campsites: List<Campsite>,
    repository: LotteryRepository,
    onCampsiteClick: (String) -> Unit
) {
    var lotteries by remember(ownerId) { mutableStateOf<List<Lottery>?>(null) }
    var error by remember(ownerId) { mutableStateOf("") }
    var attempt by remember(ownerId) { mutableStateOf(0) }

    DisposableEffect(ownerId, repository, attempt) {
        var active = true
        lotteries = null
        error = ""
        repository.getOwnerLotteries(
            ownerId,
            onSuccess = { if (active) lotteries = it },
            onFailure = { if (active) error = "Could not load your lotteries." }
        )
        onDispose { active = false }
    }

    val savedLotteries = lotteries
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("My lotteries", style = MaterialTheme.typography.headlineSmall) }
        when {
            error.isNotBlank() -> item {
                Text(error)
                Button(onClick = { attempt++ }) { Text("Retry") }
            }
            savedLotteries == null -> item { CircularProgressIndicator() }
            savedLotteries.isEmpty() -> item { Text("You have no lotteries yet.") }
            else -> items(savedLotteries, key = { it.id }) { lottery ->
                val includedCampsites = campsites.filter { it.id in lottery.campsiteIds }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("LOT-${lottery.id.take(6).uppercase()}")
                        Text(if (lottery.ownerId == ownerId) "Created by you" else "Shared with you")
                        Text("Stay dates: ${lottery.bookingStartDate} to ${lottery.bookingEndDate}")
                        for (campsite in includedCampsites) {
                            OutlinedButton(onClick = { onCampsiteClick(campsite.id) }) {
                                Text(campsite.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generates a single-use code for the creator to share with another owner.
 *
 * @param lotteryId the undrawn lottery to share.
 * @param repository requests the code and reports a failed request.
 */
@Composable
fun ShareLotteryInvitationScreen(lotteryId: String, repository: LotteryRepository) {
    var code by rememberSaveable(lotteryId) { mutableStateOf("") }
    var busy by remember(lotteryId) { mutableStateOf(false) }
    var error by remember(lotteryId) { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Invite another owner", style = MaterialTheme.typography.headlineSmall)
        Text("Share the code with one owner. They can use it to add one active campsite.")
        if (code.isNotEmpty()) {
            SelectionContainer { Text(code, style = MaterialTheme.typography.titleLarge) }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                error = ""
                repository.createSharedLotteryInvitation(
                    lotteryId,
                    onSuccess = {
                        code = it
                        busy = false
                    },
                    onFailure = {
                        error = it.message ?: "Could not generate an invitation."
                        busy = false
                    }
                )
            }
        ) {
            Text(
                when {
                    busy -> "Generating..."
                    code.isNotEmpty() -> "Generate another code"
                    else -> "Generate code"
                }
            )
        }
    }
}

/**
 * Redeems a share code and contributes the campsite that opened this screen.
 *
 * @param campsite the signed-in owner's campsite to add to the lottery.
 * @param repository redeems the code or reports a failed request.
 * @param onJoined returns to the campsite after its contribution succeeds.
 */
@Composable
fun JoinSharedLotteryScreen(
    campsite: Campsite,
    repository: LotteryRepository,
    onJoined: () -> Unit
) {
    var code by rememberSaveable(campsite.id) { mutableStateOf("") }
    var busy by remember(campsite.id) { mutableStateOf(false) }
    var error by remember(campsite.id) { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Join shared lottery", style = MaterialTheme.typography.headlineSmall)
        Text("Campsite to add: ${campsite.name}")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Invitation code") },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            enabled = code.isNotBlank() && !busy,
            onClick = {
                busy = true
                error = ""
                repository.acceptSharedLotteryInvitation(
                    code.trim(),
                    campsite.id,
                    onSuccess = {
                        busy = false
                        onJoined()
                    },
                    onFailure = {
                        error = it.message ?: "Could not join the shared lottery."
                        busy = false
                    }
                )
            }
        ) {
            Text(if (busy) "Joining..." else "Add campsite")
        }
    }
}
