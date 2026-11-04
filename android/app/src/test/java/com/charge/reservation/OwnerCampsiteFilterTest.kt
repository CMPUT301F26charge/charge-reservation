package com.charge.reservation


import com.charge.reservation.model.Campsite
import com.charge.reservation.owner.OwnerCampsiteFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the owner campsite filtering logic.
 */
class OwnerCampsiteFilterTest {

    @Test
    fun onlyCurrentOwnersCampsitesAreReturned() {
        val campsites = listOf(
            Campsite(
                id = "1",
                ownerId = "ownerA",
                name = "Pine Lake"
            ),
            Campsite(
                id = "2",
                ownerId = "ownerB",
                name = "Crystal Lake"
            )
        )

        val result = OwnerCampsiteFilter.filter(
            campsites,
            "ownerA"
        )

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun removedCampsitesAreExcluded() {
        val campsites = listOf(
            Campsite(
                id = "1",
                ownerId = "ownerA",
                removed = false
            ),
            Campsite(
                id = "2",
                ownerId = "ownerA",
                removed = true
            )
        )

        val result = OwnerCampsiteFilter.filter(
            campsites,
            "ownerA"
        )

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun ownerWithNoCampsitesGetsEmptyList() {
        val campsites = listOf(
            Campsite(
                id = "1",
                ownerId = "ownerA"
            )
        )

        val result = OwnerCampsiteFilter.filter(
            campsites,
            "ownerB"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun emptyInputReturnsEmptyList() {
        val result = OwnerCampsiteFilter.filter(
            emptyList(),
            "ownerA"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun multipleValidCampsitesAreReturned() {
        val campsites = listOf(
            Campsite(id = "1", ownerId = "ownerA"),
            Campsite(id = "2", ownerId = "ownerA"),
            Campsite(id = "3", ownerId = "ownerB")
        )

        val result = OwnerCampsiteFilter.filter(
            campsites,
            "ownerA"
        )

        assertEquals(2, result.size)
    }
}
