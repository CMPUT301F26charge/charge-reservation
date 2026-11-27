package com.charge.reservation.model

import java.time.Instant

/**
 * Represents an entrant's review of a campsite.
 *
 * Each entrant may leave one review per campsite, and only after a completed
 * stay there. Every review's score counts toward the campsite's average
 * rating, but only reviews with text are shown in the review list.
 *
 * Stored in the "ratings" collection with the document ID
 * "<campsiteId>_<raterId>", which enforces one review per entrant per campsite.
 *
 * @property campsiteId the ID of the campsite being rated
 * @property raterId the profile ID of the entrant submitting the rating
 * @property score the whole number score from 1 to 5, or null if not provided
 * @property text the written review, or null if the entrant left only stars
 * @property raterName the entrant's display name when the review was posted,
 * or null if unavailable
 * @property createdAt the instant when the review was posted, or null if unset
 */
data class Rating (
    val campsiteId: String = "",
    val raterId: String = "",
    val score: Int? = null,
    val text: String? = null,
    val raterName: String? = null,
    val createdAt: Instant? = null
)