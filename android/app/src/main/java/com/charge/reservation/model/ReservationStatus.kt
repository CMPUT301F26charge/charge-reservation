package com.charge.reservation.model

/**
 * Represents the current status of a campsite reservation.
 *
 * - [CONFIRMED]: The reservation was successfully booked and has not been
 * canceled or rejected.
 * - [CANCELLED]: The reservation was canceled by the entrant or system.
 * - [REJECTED]: The reservation was rejected by the campsite owner.
 */
enum class ReservationStatus {
    CONFIRMED, CANCELLED, REJECTED
}