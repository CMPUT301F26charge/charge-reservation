package com.charge.reservation.notifications

import com.charge.reservation.model.Notification
import com.charge.reservation.model.NotificationSendStatus
import com.charge.reservation.model.NotificationType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Streams the user's `notificationEnabled` setting from users/[userId].
     * A missing document or missing field counts as enabled, so existing users
     * who never set the field keep seeing notifications.
     * Only emits when the value actually changes.
     */
    fun notificationsEnabledFor(userId: String): Flow<Boolean> = callbackFlow {
        val listener = db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getBoolean("notificationEnabled") ?: true)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    /**
     * Streams the SENT notifications addressed to [recipientId], newest first.
     * Emits a new list whenever the matching documents change.
     */
    fun notificationsFor(recipientId: String): Flow<List<Notification>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("recipientId", recipientId)
            .whereEqualTo("sendStatus", NotificationSendStatus.SENT.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents?.mapNotNull { it.toNotification() } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    private fun DocumentSnapshot.toNotification(): Notification? {
        val recipientId = getString("recipientId") ?: return null
        return Notification(
            id = id,
            senderId = getString("senderId"),
            recipientId = recipientId,
            type = getString("type")?.let {
                runCatching { NotificationType.valueOf(it) }.getOrNull()
            },
            content = getString("content") ?: "",
            createdAt = getTimestamp("createdAt")?.toDate()?.toInstant(),
            lotteryId = getString("lotteryId"),
            reservationId = getString("reservationId"),
            campsiteId = getString("campsiteId"),
            sendStatus = getString("sendStatus")?.let {
                runCatching { NotificationSendStatus.valueOf(it) }.getOrNull()
            } ?: NotificationSendStatus.PENDING
        )
    }
}
