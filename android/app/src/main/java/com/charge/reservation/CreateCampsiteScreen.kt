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

import com.charge.reservation.model.Campsite

/**
 * Collects initial campsite details and passes them to the caller.
 *
 * The caller supplies a stable campsite ID, saves the listing,
 * and provides saving progress and error messages.
 */

@Composable
fun CreateCampsiteScreen(
    ownerId: String,
    campsiteId: String,
    isSaving: Boolean,
    errorMessage: String,
    onCreateCampsite: (Campsite) -> Unit
) {
    var name by rememberSaveable(campsiteId) { mutableStateOf("") }
    var description by rememberSaveable(campsiteId) { mutableStateOf("") }
    var location by rememberSaveable(campsiteId) { mutableStateOf("") }
    var rules by rememberSaveable(campsiteId) { mutableStateOf("") }
    var cancellationPolicy by rememberSaveable(campsiteId) { mutableStateOf("") }

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

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

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
                val campsite = Campsite(
                    id = campsiteId,
                    ownerId = ownerId,
                    name = name.trim(),
                    description = description.trim(),
                    location = location.trim(),
                    rules = rules.trim(),
                    cancellationPolicy = cancellationPolicy.trim()
                )

                onCreateCampsite(campsite)
            }
        ) {
            Text(if (isSaving) "Saving..." else "Create Campsite")
        }

    }
}

