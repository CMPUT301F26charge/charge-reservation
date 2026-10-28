package com.charge.reservation.model

/**
 * Represents an entrant's participation and invitation state in a lottery.
 *
 * - [WAITING]: The entrant has joined and is waiting the draw.
 * - [WITHDRAWN]: The entrant left before the draw.
 * - [NOT_SELECTED]: The entrant was not selected in the draw.
 * - [INVITED]: The entrant won and has an unused invitation.
 * - [DECLINED]: The entrant declined their invitation.
 * - [USED]: The entrant used their invitation to create a reservation.
 * - [EXPIRED]: The unused invitation passed its booking deadline.
 * - [CANCELLED]: Participation or an unused invitation was revoked.
 *
 * A used invitation remains [USED] even if its reservation is later cancelled.
 */
enum class ParticipationStatus {
    WAITING, WITHDRAWN, NOT_SELECTED, INVITED, DECLINED, USED, EXPIRED, CANCELLED
}