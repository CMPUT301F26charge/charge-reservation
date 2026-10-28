package com.charge.reservation.model

/**
 * Represents a campsite listing, its ownership, and descriptive information.
 *
 * Availability and individual rating records are stored separately
 * and linked to this campsite through its ID.
 *
 * @property id the ID of the campsite
 * @property ownerId the profile ID of the campsite's owner
 * @property name the displayed campsite name
 * @property description the description of the campsite and its amenities
 * @property promoImageUrls the promotional image URLs, or an empty list if absent
 * @property tags the descriptive labels associated with the campsite
 * @property maxOccupancy the maximum number of guests, or null if not provided
 * @property pricePerNight the price per night, or null if not provided
 * @property averageRating the calculated average score, or null if no ratings exist
 * @property removed whether the campsite has been removed from active listings
 * @property rules the rules guests must follow
 * @property cancellationPolicy the conditions for cancelling a reservation
 * @property location the readable location or address of the campsite
 * @property latitude the map latitude in degrees, or null if not provided
 * @property longitude the map longitude in degrees, or null if not provided
 */
data class Campsite (
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val promoImageUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    val maxOccupancy: Int? = null,
    val pricePerNight: Double? = null,
    val averageRating: Double? = null,
    val removed: Boolean = false,

    val rules: String = "",
    val cancellationPolicy: String = "",

    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)