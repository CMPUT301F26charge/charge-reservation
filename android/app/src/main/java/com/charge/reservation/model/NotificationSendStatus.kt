package com.charge.reservation.model

/**
 * Represents the sending state of a notification for one recipient.
 *
 * - [PENDING]: The notification is waiting to be processed.
 * - [SENT]: The send attempt succeeded.
 * - [FAILED]: The send attempt failed.
 * - [SKIPPED]: Sending was skipped because the recipient
 * disabled notifications.
 */
enum class NotificationSendStatus {
    PENDING, SENT, FAILED, SKIPPED
}