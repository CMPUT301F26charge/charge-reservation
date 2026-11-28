package com.charge.reservation.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Maximum sites an owner can publish in one lottery using Firestore rules. */
const val MAX_LOTTERY_CAMPSITES = 9

/**
 * Represents a lottery through which selected entrants can reserve a campsite.
 *
 * Stores the shared registration period, permitted booking dates, deadline,
 * and selection settings.
 *
 * @property id the ID of the lottery
 * @property ownerId the ID of the owner managing the lottery
 * @property campsiteIds the distinct IDs of campsites included in the lottery
 * @property registrationStartAt the instant when registration opens
 * @property registrationEndAt the instant when registration closes
 * @property bookingDeadline the instant by which winners must use their invitation
 * @property bookingStartDate the earliest permitted check-in date
 * @property bookingEndDate the latest permitted checkout date
 * @property timeZone the time zone used to display lottery times and interpret
 * the booking dates
 * @property waitlistCapacity the maximum number of entrants allowed to join,
 * or null if unlimited
 * @property winnerCount the number of entrants to select as winners
 * @property drawCompletedAt the instant when the draw completed, or null
 * if it has not completed
 * @property contributedCampsiteIds the campsite IDs added by other owners. The
 * creator's editor preserves these when saving shared settings.
 */
data class Lottery (
    val id: String = "",
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),

    val registrationStartAt: Instant? = null,
    val registrationEndAt: Instant? = null,
    val bookingDeadline: Instant? = null,
    val bookingStartDate: LocalDate? = null,
    val bookingEndDate: LocalDate? = null,
    val timeZone: ZoneId? = null,

    val waitlistCapacity: Int? = null,
    var waitlistSize: Int? = null,
    val winnerCount: Int? = null,
    val drawCompletedAt: Instant? = null,
    val contributedCampsiteIds: List<String> = emptyList()
    )
