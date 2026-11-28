package com.charge.reservation.qr

import com.charge.reservation.model.Campsite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests the campsite identifiers embedded in the promotional QR codes. */
class CampsiteQrCodeTest {
    @Test
    fun encodedCodeResolvesToTheOriginalCampsite() {
        val id = "lac-à-lours%01"
        val payload = CampsiteQrCode.encode(id)

        assertEquals(id, CampsiteQrCode.decode(payload))
    }

    @Test
    fun listingEditsKeepTheSameCodeAndDifferentListingsGetDifferentCodes() {
        val campsite = Campsite(id = "pine-lake", name = "Pine Lake")
        val renamed = campsite.copy(name = "North Pine Lake", description = "Lakeside site")
        val other = campsite.copy(id = "cedar-lake")

        val originalCode = CampsiteQrCode.encode(campsite.id)
        assertEquals(originalCode, CampsiteQrCode.encode(renamed.id))
        assertNotEquals(originalCode, CampsiteQrCode.encode(other.id))
    }

    @Test
    fun unrelatedAndMalformedCodesAreRejected() {
        assertNull(CampsiteQrCode.decode("https://example.com"))
        assertNull(CampsiteQrCode.decode("charge-reservation:campsite:"))
        assertNull(CampsiteQrCode.decode("charge-reservation:campsite:../other"))
        assertNull(CampsiteQrCode.decode("charge-reservation:campsite:__reserved__"))
        assertNull(CampsiteQrCode.decode("charge-reservation:campsite:" + "é".repeat(751)))
    }
}
