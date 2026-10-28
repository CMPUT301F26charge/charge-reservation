package com.charge.reservation.model

import java.time.LocalDate

/**
 * Represents a campsite's available guest capacity for one night.
 *
 * Each record corresponds to one campsite and one calendar date.
 *
 * @property campsiteId the ID of the campsite offering this night
 * @property date the calendar date on which the night begins, or null if unset
 * @property guestCapacity the total guest limit for this night, or null if unset
 * @property bookedGuests the number of guest spaces reserved for this night
 */
data class AvailabilityDay (
    val campsiteId: String = "",
    val date: LocalDate? = null,
    val guestCapacity: Int? = null,
    val bookedGuests: Int = 0
)