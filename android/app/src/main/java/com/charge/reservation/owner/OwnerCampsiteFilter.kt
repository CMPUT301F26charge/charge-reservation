package com.charge.reservation.owner


import com.charge.reservation.model.Campsite

/**
 * Filters campsite listings for the owner-facing campsite list.
 *
 * Only campsites belonging to the specified owner that have
 * not been removed are returned.
 */
object OwnerCampsiteFilter {

    /**
     * Returns the active campsites belonging to [ownerId].
     *
     * @param campsites all available campsite records
     * @param ownerId the current owner's profile ID
     * @return campsites owned by the user that are not removed
     */
    fun filter(
        campsites: List<Campsite>,
        ownerId: String
    ): List<Campsite> {
        return campsites.filter {
            it.ownerId == ownerId && !it.removed
        }
    }
}
