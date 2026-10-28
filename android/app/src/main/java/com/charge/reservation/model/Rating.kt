package com.charge.reservation.model

/**
 * Represents an entrant's rating of a campsite.
 *
 * Each entrant may have one rating per campsite and can update that rating.
 * Submitted scores must be between 1 and 5.
 *
 * @property campsiteId the ID of the campsite being rated
 * @property raterId the profile ID of the entrant submitting the rating
 * @property score the whole number score from 1 to 5, or null if not provided
 */
data class Rating (
    val campsiteId: String = "",
    val raterId: String = "",
    val score: Int? = null
)