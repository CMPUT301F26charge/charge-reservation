package com.charge.reservation.qr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.charge.reservation.model.Campsite
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Shows an owner's promotional QR code on demand.
 * @param campsite the saved listing whose ID is encoded and whose name labels the dialog.
 */
@Composable
internal fun CampsiteQrButton(campsite: Campsite) {
    var isOpen by rememberSaveable(campsite.id) { mutableStateOf(false) }

    Button(onClick = { isOpen = true }) {
        Text("Show QR code")
    }

    if (isOpen) {
        val image = remember(campsite.id) {
            runCatching {
                BarcodeEncoder().encodeBitmap(
                    CampsiteQrCode.encode(campsite.id),
                    BarcodeFormat.QR_CODE,
                    512,
                    512,
                    mapOf(EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
                ).asImageBitmap()
            }.getOrNull()
        }

        AlertDialog(
            onDismissRequest = { isOpen = false },
            title = { Text(campsite.name) },
            text = {
                if (image == null) {
                    Text("Could not generate this campsite's QR code.")
                } else {
                    Image(
                        bitmap = image,
                        contentDescription = "Promotional QR code for ${campsite.name}",
                        modifier = Modifier.size(240.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isOpen = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Opens the camera scanner from campsite browsing and validates its result.
 * @param onCampsiteIdScanned receives the ID from a valid app QR code.
 * Cancelled scans leave the entrant on the browsing screen.
 */
@Composable
internal fun ScanCampsiteQrButton(onCampsiteIdScanned: (String) -> Unit) {
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        when {
            result.originalIntent?.getBooleanExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, false) == true -> {
                errorMessage = "Allow camera access to scan a campsite QR code."
            }
            contents != null -> {
                val campsiteId = CampsiteQrCode.decode(contents)
                if (campsiteId == null) {
                    errorMessage = "That QR code is not a Charge Reservation campsite code."
                } else {
                    onCampsiteIdScanned(campsiteId)
                }
            }
        }
    }

    TextButton(
        onClick = {
            errorMessage = ""
            scanner.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Scan a campsite QR code")
                    .setBeepEnabled(false)
                    .addExtra(Intents.Scan.SHOW_MISSING_CAMERA_PERMISSION_DIALOG, false)
            )
        }
    ) {
        Text("Scan campsite QR code")
    }

    if (errorMessage.isNotEmpty()) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
