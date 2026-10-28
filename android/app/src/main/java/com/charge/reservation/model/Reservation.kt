package com.charge.reservation.model

import java.time.LocalDate
import java.time.Instant

/**
 * Represents a campsite reservation made using a lottery invitation.
 *
 * Stores the entrant's stay dates, guest count, reservation status,
 * and cancellation or rejection details.
 *
 * @property id the ID of the reservation
 * @property campsiteId the ID of the reserved campsite
 * @property lotteryId the ID of the lottery whose invitation was used
 * @property entrantId the ID of the entrant who made the reservation
 * @property createdAt the instant when the reservation was confirmed
 * @property checkInDate the first night of the stay
 * @property checkOutDate the check-out date
 * @property guestCount the number of guests included in the reservation
 * @property status the current reservation status
 * @property statusChangedAt the instant when the current status was set
 * @property statusChangeReason the reason for cancellation or rejection,
 * or null if no reason was recorded
 */
data class Reservation (
    val id: String = "",
    val campsiteId: String = "",
    val lotteryId: String = "",
    val entrantId: String = "",
    val createdAt: Instant? = null,

    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val guestCount: Int? = null,

    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val statusChangedAt: Instant? = null,
    val statusChangeReason: String? = null
)