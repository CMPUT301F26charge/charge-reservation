package com.charge.reservation.notifications

import java.time.Instant
import java.time.LocalDate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.charge.reservation.booking.FirestorePaths
import com.charge.reservation.booking.toLottery
import com.charge.reservation.booking.toParticipation
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.NotificationSendStatus
import com.charge.reservation.model.NotificationType
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.repository.ReservationValueParser

/** The longest message an owner can send. */
const val MAX_NOTIFICATION_LENGTH = 500

/** An error whose message is safe to show directly to the owner. */
class OwnerNotificationException(message: String) : Exception(message)

/** Finds an [OwnerNotificationException] anywhere in the cause chain. */
fun Throwable.ownerNotificationMessage(fallback: String): String =
    generateSequence(this) { it.cause }
        .filterIsInstance<OwnerNotificationException>()
        .firstOrNull()
        ?.message
        ?: fallback

/**
 * Which entrants of a lottery receive a notification.
 *
 * @property label the name shown to the owner
 * @property statuses the participation states included in the group
 */
enum class EntrantGroup(val label: String, val statuses: Set<ParticipationStatus>) {
    ALL(
        "All entrants",
        setOf(
            ParticipationStatus.WAITING,
            ParticipationStatus.NOT_SELECTED,
            ParticipationStatus.INVITED,
            ParticipationStatus.DECLINED,
            ParticipationStatus.USED,
            ParticipationStatus.EXPIRED
        )
    ),
    WAITING("Waiting for the draw", setOf(ParticipationStatus.WAITING)),
    WINNERS("Winners", setOf(ParticipationStatus.INVITED, ParticipationStatus.USED)),
    NOT_SELECTED("Not selected", setOf(ParticipationStatus.NOT_SELECTED))
}

/** Who an owner's notification is sent to. */
sealed interface NotificationAudience {
    /** Entrants of one of the campsite's lotteries, filtered by [group]. */
    data class LotteryEntrants(
        val lotteryId: String,
        val group: EntrantGroup
    ) : NotificationAudience

    /** Entrants with a confirmed reservation that has not yet ended. */
    object UpcomingGuests : NotificationAudience
}

/**
 * One person who will receive the notification.
 *
 * @property reservationId the recipient's reservation, for reservation guests
 * @property notificationsEnabled false if the recipient turned notifications
 * off, in which case their copy is saved as SKIPPED
 */
data class NotificationRecipient(
    val userId: String,
    val reservationId: String?,
    val notificationsEnabled: Boolean
)

/** How many recipients received the notification and how many were skipped. */
data class SendResult(val sentCount: Int, val skippedCount: Int)

/**
 * One notification as the owner wrote it, combining every recipient's copy.
 *
 * @property toReservationGuests true if sent to reservation guests,
 * false if sent to lottery entrants
 * @property sentCount copies delivered to recipients
 * @property skippedCount copies skipped because notifications were off
 */
data class SentAnnouncement(
    val id: String,
    val content: String,
    val campsiteId: String?,
    val lotteryId: String?,
    val toReservationGuests: Boolean,
    val createdAt: Instant?,
    val sentCount: Int,
    val skippedCount: Int
)

/**
 * Sends owner announcements and lists the ones an owner has sent.
 *
 * Each recipient gets their own notification document. All copies of one
 * announcement share an "announcementId" so they can be shown together.
 */
class OwnerNotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Streams the owner's sent announcements, newest first.
     */
    fun sentAnnouncements(ownerId: String): Flow<List<SentAnnouncement>> = callbackFlow {
        val listener = db.collection(NOTIFICATIONS)
            .whereEqualTo("senderId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: return@addSnapshotListener
                trySend(documents.toAnnouncements())
            }

        awaitClose { listener.remove() }
    }

    /**
     * Loads the lotteries the owner has created for one campsite, newest first.
     */
    suspend fun getCampsiteLotteries(ownerId: String, campsiteId: String): List<Lottery> =
        db.collection(FirestorePaths.LOTTERIES)
            .whereEqualTo("ownerId", ownerId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                runCatching { document.toLottery().copy(id = document.id) }.getOrNull()
            }
            .filter { campsiteId in it.campsiteIds }
            .sortedByDescending { it.registrationStartAt }

    /**
     * Finds everyone who would receive a notification for [audience].
     *
     * Each person appears once. The owner, missing profiles, and removed
     * profiles are left out.
     *
     * @throws OwnerNotificationException if the campsite or lottery
     * does not belong to the owner.
     */
    suspend fun findRecipients(
        ownerId: String,
        campsiteId: String,
        audience: NotificationAudience,
        today: LocalDate = LocalDate.now()
    ): List<NotificationRecipient> {
        val campsite = db.collection(FirestorePaths.CAMPSITES)
            .document(campsiteId)
            .get()
            .await()

        if (!campsite.exists() || campsite.getString("ownerId") != ownerId) {
            throw OwnerNotificationException("Select one of your campsites.")
        }

        // Pairs of (recipient ID, reservation ID or null).
        val candidates: List<Pair<String, String?>> = when (audience) {
            is NotificationAudience.LotteryEntrants -> {
                val lottery = db.collection(FirestorePaths.LOTTERIES)
                    .document(audience.lotteryId)
                    .get()
                    .await()

                val lotteryCampsites = lottery.get("campsiteIds") as? List<*>
                if (!lottery.exists() ||
                    lottery.getString("ownerId") != ownerId ||
                    lotteryCampsites?.contains(campsiteId) != true
                ) {
                    throw OwnerNotificationException("Select one of this campsite's lotteries.")
                }

                db.collection(FirestorePaths.PARTICIPATIONS)
                    .whereEqualTo("lotteryId", audience.lotteryId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { runCatching { it.toParticipation() }.getOrNull() }
                    .filter { it.status in audience.group.statuses }
                    .map { it.entrantId to null }
            }

            NotificationAudience.UpcomingGuests -> {
                db.collection("reservations")
                    .whereEqualTo("campsiteId", campsiteId)
                    .whereEqualTo("status", ReservationStatus.CONFIRMED.name)
                    .get()
                    .await()
                    .documents
                    .filter { document ->
                        val checkOut = runCatching {
                            ReservationValueParser.parseDate(document.get("checkOutDate"))
                        }.getOrNull()

                        checkOut != null && !checkOut.isBefore(today)
                    }
                    .mapNotNull { document ->
                        document.getString("entrantId")?.let { it to document.id }
                    }
            }
        }

        val uniqueCandidates = candidates
            .filter { (userId, _) -> userId.isNotBlank() && userId != ownerId }
            .distinctBy { (userId, _) -> userId }

        // Read each profile to respect the recipient's notification setting.
        return coroutineScope {
            uniqueCandidates.map { (userId, reservationId) ->
                async {
                    val user = db.collection("users").document(userId).get().await()

                    if (!user.exists() || user.getBoolean("removed") == true) {
                        null
                    } else {
                        NotificationRecipient(
                            userId = userId,
                            reservationId = reservationId,
                            notificationsEnabled =
                                user.getBoolean("notificationEnabled") ?: true
                        )
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Sends the owner's message to everyone in [audience].
     *
     * Recipients who turned notifications off get a copy marked SKIPPED,
     * which they will not see.
     *
     * @return how many copies were sent and skipped.
     * @throws OwnerNotificationException if the message is invalid or
     * there is no one to send it to.
     */
    suspend fun send(
        ownerId: String,
        campsiteId: String,
        audience: NotificationAudience,
        content: String
    ): SendResult {
        val message = content.trim()

        if (message.isBlank()) {
            throw OwnerNotificationException("Please write a message.")
        }
        if (message.length > MAX_NOTIFICATION_LENGTH) {
            throw OwnerNotificationException(
                "Messages can be at most $MAX_NOTIFICATION_LENGTH characters."
            )
        }

        val recipients = findRecipients(ownerId, campsiteId, audience)

        if (recipients.isEmpty()) {
            throw OwnerNotificationException("No one matches these recipients yet.")
        }

        val notifications = db.collection(NOTIFICATIONS)
        val announcementId = notifications.document().id
        val lotteryId = (audience as? NotificationAudience.LotteryEntrants)?.lotteryId

        // A Firestore batch holds at most 500 writes.
        recipients.chunked(400).forEach { chunk ->
            val batch = db.batch()

            chunk.forEach { recipient ->
                val document = notifications.document()
                val status = if (recipient.notificationsEnabled) {
                    NotificationSendStatus.SENT
                } else {
                    NotificationSendStatus.SKIPPED
                }

                batch.set(
                    document,
                    mapOf(
                        "id" to document.id,
                        "senderId" to ownerId,
                        "recipientId" to recipient.userId,
                        "type" to NotificationType.OWNER_CUSTOM.name,
                        "content" to message,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lotteryId" to lotteryId,
                        "reservationId" to recipient.reservationId,
                        "campsiteId" to campsiteId,
                        "sendStatus" to status.name,
                        "announcementId" to announcementId
                    )
                )
            }

            batch.commit().await()
        }

        return SendResult(
            sentCount = recipients.count { it.notificationsEnabled },
            skippedCount = recipients.count { !it.notificationsEnabled }
        )
    }

    /**
     * Combines recipient copies into one entry per announcement.
     */
    private fun List<DocumentSnapshot>.toAnnouncements(): List<SentAnnouncement> =
        filter { it.getString("type") == NotificationType.OWNER_CUSTOM.name }
            .groupBy { it.getString("announcementId") ?: it.id }
            .map { (announcementId, copies) ->
                val first = copies.first()

                SentAnnouncement(
                    id = announcementId,
                    content = first.getString("content").orEmpty(),
                    campsiteId = first.getString("campsiteId"),
                    lotteryId = first.getString("lotteryId"),
                    toReservationGuests = first.getString("reservationId") != null,
                    createdAt = first.getTimestamp(
                        "createdAt",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toDate()?.toInstant(),
                    sentCount = copies.count {
                        it.getString("sendStatus") == NotificationSendStatus.SENT.name
                    },
                    skippedCount = copies.count {
                        it.getString("sendStatus") == NotificationSendStatus.SKIPPED.name
                    }
                )
            }
            .sortedByDescending { it.createdAt }

    private companion object {
        const val NOTIFICATIONS = "notifications"
    }
}
