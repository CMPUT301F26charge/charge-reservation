
package com.charge.reservation.profile

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.charge.reservation.model.UserProfile

/**
 * Allows an existing user to edit their personal information.
 *
 * This screen collects changes and validates them before invoking
 * the save callback. Firestore operations are handled separately.
 *
 * @param profile current saved user profile
 * @param isSaving whether the save operation is running
 * @param saveError error returned by the repository
 * @param onSave callback for submitting validated profile changes
 */
@Composable
fun EditProfileScreen(
    profile: UserProfile,
    isSaving: Boolean,
    saveError: String,
    onSave: (String, String, String, Uri?) -> Unit
) {
    var name by rememberSaveable(profile.id) {
        mutableStateOf(profile.name)
    }

    var email by rememberSaveable(profile.id) {
        mutableStateOf(profile.email)
    }

    var phoneNumber by rememberSaveable(profile.id) {
        mutableStateOf(profile.phoneNumber)
    }

    var validationError by remember {
        mutableStateOf("")
    }

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Update your profile details.",
            style = MaterialTheme.typography.bodyMedium
        )

        val imageToDisplay: Any? =
            selectedImageUri ?: profile.profileImageUrl

        if (imageToDisplay != null) {
            AsyncImage(
                model = imageToDisplay,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("No profile picture selected")
        }

        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose Profile Picture")
        }

        if (selectedImageUri != null) {
            Text(
                text = "New picture selected. Save to upload.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                validationError = ""
            },
            label = { Text("Name") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                validationError = ""
            },
            label = { Text("Email") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                validationError = ""
            },
            label = { Text("Phone number (optional)") },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        if (validationError.isNotEmpty()) {
            Text(
                text = validationError,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (saveError.isNotEmpty()) {
            Text(
                text = saveError,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val updatedName = name.trim()
                val updatedEmail = email.trim()
                val updatedPhone = phoneNumber.trim()

                validationError = when {
                    updatedName.isBlank() ->
                        "Please enter your name."

                    updatedEmail.isBlank() ->
                        "Please enter your email address."

                    !Patterns.EMAIL_ADDRESS.matcher(
                        updatedEmail
                    ).matches() ->
                        "Please enter a valid email address."

                    else -> ""
                }

                if (validationError.isEmpty()) {
                    onSave(
                        updatedName,
                        updatedEmail,
                        updatedPhone,
                        selectedImageUri
                    )
                }
            }
        ) {
            if (isSaving) {
                CircularProgressIndicator()
            } else {
                Text("Save Changes")
            }
        }
    }
}
