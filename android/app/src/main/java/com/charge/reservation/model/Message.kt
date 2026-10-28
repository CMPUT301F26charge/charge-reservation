package com.charge.reservation.model

import java.time.Instant

/**
 * Represents a private message sent from one user to another.
 *
 * @property id the ID of the message
 * @property senderId the ID of the user who sent the message
 * @property recipientId the ID of the intended recipient
 * @property content the message text
 * @property createdAt the instant when the message was created
 */
data class Message (
    val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val content: String = "",
    val createdAt: Instant? = null
    )