package com.charge.reservation.qr

/**
 * Creates and reads the QR payload used by campsite listings.
 * The payload contains the saved campsite ID so edits to the listing keep the code valid.
 */
internal object CampsiteQrCode {
    private const val PREFIX = "charge-reservation:campsite:"
    private const val MAX_ID_BYTES = 1500

    /**
     * Encodes the saved Firestore [campsiteId] for the app's QR scanner.
     * @return the text to render as a QR code.
     * @throws IllegalArgumentException if the ID is unsuitable for a campsite document.
     */
    fun encode(campsiteId: String): String {
        require(isValidId(campsiteId)) { "Invalid campsite ID." }
        return PREFIX + campsiteId
    }

    /**
     * Extracts a campsite ID from a scanned payload.
     * @param payload the text returned by the QR scanner.
     * @return the original ID, or null when the code is unrelated or malformed.
     */
    fun decode(payload: String): String? {
        if (!payload.startsWith(PREFIX)) return null
        return payload.removePrefix(PREFIX).takeIf(::isValidId)
    }

    private fun isValidId(id: String): Boolean =
        id.isNotBlank() &&
            '/' !in id &&
            id != "." && id != ".." &&
            !(id.length >= 4 && id.startsWith("__") && id.endsWith("__")) &&
            id.toByteArray(Charsets.UTF_8).size <= MAX_ID_BYTES
}
