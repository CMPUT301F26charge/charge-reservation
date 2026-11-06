package com.charge.reservation.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Campsite

/**
 * Displays campsite rules and policies before reservation confirmation.
 *
 * Entrants must acknowledge the displayed policies before continuing.
 * The acknowledgement resets when a different booking is opened.
 *
 * Outstanding: Connect confirmation to the team's reservation flow.
 */
@Composable
fun RulesAcknowledgementScreen(
    campsite: Campsite,
    bookingId: String,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // ADDED: Keep acknowledgement separate for each booking.
    key(campsite.id, bookingId) {
        var acknowledged by rememberSaveable {
            mutableStateOf(false)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Reservation Rules",
                fontSize = 28.sp
            )

            Text(
                text = campsite.name,
                fontSize = 22.sp
            )

            HorizontalDivider()

            Text(
                text = "Campsite Rules",
                fontSize = 20.sp
            )

            Text(
                text = campsite.rules.ifBlank {
                    "No additional campsite rules provided."
                }
            )

            HorizontalDivider()

            Text(
                text = "Cancellation Policy",
                fontSize = 20.sp
            )

            Text(
                text = campsite.cancellationPolicy.ifBlank {
                    "No cancellation policy provided."
                }
            )

            HorizontalDivider()

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { acknowledged = !acknowledged }
            ) {
                Checkbox(
                    checked = acknowledged,
                    onCheckedChange = null
                )

                Text(
                    text = "I have read and agree to the campsite rules and policies.",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // ADDED: Confirmation cannot proceed without acknowledgement.
            Button(
                onClick = onConfirmClick,
                enabled = acknowledged,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Confirmation")
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
