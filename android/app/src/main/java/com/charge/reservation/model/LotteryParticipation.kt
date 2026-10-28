package com.charge.reservation.model

import java.time.Instant

/**
 * Represents one entrant's participation in a lottery.
 *
 * Tracks when the entrant joined, their participation and invitation state,
 * and the reservation created using their invitation.
 *
 * @property lotteryId the ID of the lottery the entrant joined
 * @property entrantId the ID of the participating entrant
 * @property joinedAt the instant when the entrant joined the lottery
 * @property status the current participation or invitation state
 * @property reservationId the ID of the reservation created using the invitation,
 * or null if the invitation has not been used
 */
data class LotteryParticipation (
    val lotteryId: String = "",
    val entrantId: String = "",
    val joinedAt: Instant? = null,
    val status: ParticipationStatus = ParticipationStatus.WAITING,
    val reservationId: String? = null
)