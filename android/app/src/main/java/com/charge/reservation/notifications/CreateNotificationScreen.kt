package com.charge.reservation.notifications

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Lottery

private const val TAG = "CreateNotification"

/** The two kinds of recipients an owner can choose. */
private enum class AudienceType(val label: String) {
    LOTTERY("Lottery entrants"),
    GUESTS("Guests with upcoming reservations")
}

/**
 * Lets an owner write a notification for one of their campsites and choose
 * who receives it: entrants of one of the campsite's lotteries, or guests
 * with upcoming confirmed reservations.
 *
 * Shows how many people will receive it and asks for confirmation before
 * sending, because sent notifications cannot be edited or recalled.
 *
 * @param ownerId the signed-in owner's Firebase UID
 * @param campsites the owner's active campsites
 * @param onNotificationSent called after the notification is sent
 */
@Composable
fun CreateNotificationScreen(
    ownerId: String,
    campsites: List<Campsite>,
    onNotificationSent: () -> Unit,
    repository: OwnerNotificationRepository = remember { OwnerNotificationRepository() }
) {
    if (campsites.isEmpty()) {
        Text(
            text = "Register a campsite before sending notifications.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    val scope = rememberCoroutineScope()

    var selectedCampsiteId by rememberSaveable { mutableStateOf(campsites.first().id) }
    val campsite = campsites.firstOrNull { it.id == selectedCampsiteId } ?: campsites.first()

    var audienceType by rememberSaveable { mutableStateOf(AudienceType.LOTTERY) }
    var selectedLotteryId by rememberSaveable(campsite.id) { mutableStateOf<String?>(null) }
    var entrantGroup by rememberSaveable { mutableStateOf(EntrantGroup.ALL) }
    var message by rememberSaveable { mutableStateOf("") }

    var lotteries by remember(campsite.id) { mutableStateOf<List<Lottery>?>(null) }
    var lotteriesError by remember(campsite.id) { mutableStateOf("") }

    var recipients by remember { mutableStateOf<List<NotificationRecipient>?>(null) }
    var recipientsError by remember { mutableStateOf("") }

    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ownerId, campsite.id) {
        lotteries = null
        lotteriesError = ""

        try {
            val loaded = repository.getCampsiteLotteries(ownerId, campsite.id)
            lotteries = loaded

            if (loaded.none { it.id == selectedLotteryId }) {
                selectedLotteryId = loaded.firstOrNull()?.id
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Could not load lotteries", exception)
            lotteries = emptyList()
            lotteriesError = "Could not load this campsite's lotteries."
        }
    }

    val audience: NotificationAudience? = when (audienceType) {
        AudienceType.LOTTERY -> selectedLotteryId?.let {
            NotificationAudience.LotteryEntrants(it, entrantGroup)
        }

        AudienceType.GUESTS -> NotificationAudience.UpcomingGuests
    }

    // Preview the recipients whenever the choice changes.
    LaunchedEffect(ownerId, campsite.id, audience) {
        recipients = null
        recipientsError = ""

        if (audience == null) return@LaunchedEffect

        try {
            recipients = repository.findRecipients(ownerId, campsite.id, audience)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Could not find recipients", exception)
            recipientsError = exception.ownerNotificationMessage(
                "Could not find the recipients. Please try again."
            )
        }
    }

    val deliverCount = recipients?.count { it.notificationsEnabled } ?: 0
    val skipCount = recipients?.count { !it.notificationsEnabled } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create notification",
            style = MaterialTheme.typography.headlineSmall
        )

        SelectField(
            label = "Campsite",
            selectedText = campsite.name,
            options = campsites,
            optionText = { it.name },
            onSelect = {
                selectedCampsiteId = it.id
                sendError = ""
            },
            enabled = !isSending
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Send to", style = MaterialTheme.typography.labelLarge)

            Column(Modifier.selectableGroup()) {
                AudienceType.values().forEach { type ->
                    RadioOption(
                        text = type.label,
                        selected = audienceType == type,
                        enabled = !isSending,
                        onClick = {
                            audienceType = type
                            sendError = ""
                        }
                    )
                }
            }
        }

        if (audienceType == AudienceType.LOTTERY) {
            val loadedLotteries = lotteries

            when {
                loadedLotteries == null -> Text(
                    text = "Loading lotteries…",
                    style = MaterialTheme.typography.bodySmall
                )

                lotteriesError.isNotEmpty() -> Text(
                    text = lotteriesError,
                    color = MaterialTheme.colorScheme.error
                )

                loadedLotteries.isEmpty() -> Text(
                    text = "This campsite has no lotteries yet.",
                    style = MaterialTheme.typography.bodyMedium
                )

                else -> {
                    SelectField(
                        label = "Lottery",
                        selectedText = loadedLotteries
                            .firstOrNull { it.id == selectedLotteryId }
                            ?.displayLabel()
                            ?: "Select a lottery",
                        options = loadedLotteries,
                        optionText = { it.displayLabel() },
                        onSelect = {
                            selectedLotteryId = it.id
                            sendError = ""
                        },
                        enabled = !isSending
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Entrants", style = MaterialTheme.typography.labelLarge)

                        Column(Modifier.selectableGroup()) {
                            EntrantGroup.values().forEach { group ->
                                RadioOption(
                                    text = group.label,
                                    selected = entrantGroup == group,
                                    enabled = !isSending,
                                    onClick = {
                                        entrantGroup = group
                                        sendError = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { newMessage ->
                if (newMessage.length <= MAX_NOTIFICATION_LENGTH) {
                    message = newMessage
                    sendError = ""
                }
            },
            label = { Text("Message") },
            supportingText = { Text("${message.length} / $MAX_NOTIFICATION_LENGTH") },
            minLines = 4,
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Recipients see this as \"From campsite owner\", " +
                "so mention your campsite if it helps.",
            style = MaterialTheme.typography.bodySmall
        )

        val recipientSummary = when {
            audience == null -> ""
            recipientsError.isNotEmpty() -> recipientsError
            recipients == null -> "Finding recipients…"
            recipients?.isEmpty() == true -> "No one matches these recipients yet."
            skipCount > 0 -> "Will be delivered to ${peopleText(deliverCount)}. " +
                "${peopleText(skipCount).replaceFirstChar { it.uppercase() }} turned " +
                "notifications off and will be skipped."
            else -> "Will be delivered to ${peopleText(deliverCount)}."
        }

        if (recipientSummary.isNotEmpty()) {
            Text(
                text = recipientSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (recipientsError.isNotEmpty()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (sendError.isNotEmpty()) {
            Text(
                text = sendError,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { showConfirmDialog = true },
            enabled = !isSending &&
                    message.isNotBlank() &&
                    audience != null &&
                    recipients?.isNotEmpty() == true,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send notification")
            }
        }
    }

    if (showConfirmDialog && audience != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Send notification?") },
            text = {
                Text(
                    "This will be delivered to ${peopleText(deliverCount)}. " +
                        "Sent notifications can't be edited or recalled."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        isSending = true
                        sendError = ""

                        scope.launch {
                            try {
                                repository.send(ownerId, campsite.id, audience, message)
                                onNotificationSent()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                Log.e(TAG, "Could not send notification", exception)
                                sendError = exception.ownerNotificationMessage(
                                    "Could not send the notification. Please try again."
                                )
                            } finally {
                                isSending = false
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** "1 person" or "N people". */
private fun peopleText(count: Int): String =
    if (count == 1) "1 person" else "$count people"

/** A short description of a lottery, such as "Stays Jun 1, 2027 – Jun 30, 2027 (draw pending)". */
private fun Lottery.displayLabel(): String {
    val format = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val start = bookingStartDate
    val end = bookingEndDate

    val stay = if (start != null && end != null) {
        "Stays ${start.format(format)} – ${end.format(format)}"
    } else {
        "Lottery"
    }

    val drawState = if (drawCompletedAt != null) "draw done" else "draw pending"
    return "$stay ($drawState)"
}

/** A labelled button that opens a list of options to pick from. */
@Composable
private fun <T> SelectField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("▾")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
    }
}

/** One choice in a group of radio buttons; the whole row is tappable. */
@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
