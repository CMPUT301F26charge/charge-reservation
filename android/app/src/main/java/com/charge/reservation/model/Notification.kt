package com.charge.reservation.model

import java.time.Instant

/**
 * Represents a notification for one recipient and tracks its sending state.
 **
 * @property id the ID of the notification
 * @property senderId the ID of the owner who wrote the announcement,
 * or null for system notifications
 * @property recipientId the ID of the intended recipient
 * @property type the source of the notification, or null if not yet assigned
 * @property content the notification text
 * @property createdAt the instant when the notification was created
 * @property lotteryId the ID of the related lottery, or null if not applicable
 * @property reservationId the ID of the related reservation, or null if not applicable
 * @property campsiteId the ID of the related campsite, or null if not applicable
 * @property sendStatus the current sending state for this recipient
 */
data class Notification (
    val id: String = "",
    val senderId: String? = null,
    val recipientId: String = "",
    val type: NotificationType? = null,
    val content: String = "",
    val createdAt: Instant? = null,
    val lotteryId: String? = null,
    val reservationId: String? = null,
    val campsiteId: String? = null,
    val sendStatus: NotificationSendStatus = NotificationSendStatus.PENDING
)