package com.charge.reservation.entrant

import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.model.Campsite
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Returns campsites matching the search, location, guest count, and dates.
 * The date range uses check-in inclusive and check-out exclusive.
 *
 * @param campsites campsites to search
 * @param searchText name entered by the user
 * @param locationText location entered by the user
 * @param requestedGuests number of guests, or null if not set
 * @param checkIn first requested night, or null if not set
 * @param checkOut checkout date, or null if not set
 * @param availabilityById available nights for each campsite ID
 */
fun filterCampsites(
    campsites: List<Campsite>,
    searchText: String,
    locationText: String,
    requestedGuests: Int?,
    checkIn: LocalDate?,
    checkOut: LocalDate?,
    availabilityById: Map<String, List<AvailabilityDay>>
): List<Campsite> {
    val normalizedSearch = searchText.trim().replace(Regex("\\s+"), " ")
    val normalizedLocation = locationText.trim().replace(Regex("\\s+"), " ")

    return campsites.filter { campsite ->
        val matchesName = campsite.name.trim().replace(Regex("\\s+"), " ")
            .contains(normalizedSearch, ignoreCase = true)
        val matchesLocation = campsite.location.trim().replace(Regex("\\s+"), " ")
            .contains(normalizedLocation, ignoreCase = true)
        val matchesGuests = requestedGuests == null ||
                (campsite.maxOccupancy ?: 0) >= requestedGuests

        val matchesDates = if (checkIn == null || checkOut == null) {
            true
        } else {
            val days = availabilityById[campsite.id].orEmpty()
            val neededGuests = requestedGuests ?: 1
            val nights = ChronoUnit.DAYS.between(checkIn, checkOut)

            (0L until nights).all { offset ->
                val requestedDate = checkIn.plusDays(offset)
                days.any { day ->
                    day.date == requestedDate &&
                            (day.guestCapacity ?: 0) - day.bookedGuests >= neededGuests
                }
            }
        }

        matchesName && matchesLocation && matchesGuests && matchesDates
    }
}
