package com.charge.reservation.model

/**
 * Represents the source of a notification.
 *
 * - [SYSTEM]: An automatically generated alert, such as lottery results
 * or a reservation cancellation.
 * - [OWNER_CUSTOM]: A custom announcement written by a campsite owner.
 */
enum class NotificationType {
    SYSTEM, OWNER_CUSTOM
}