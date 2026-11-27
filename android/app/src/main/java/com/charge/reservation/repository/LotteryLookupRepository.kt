package com.charge.reservation.repository

import java.time.Instant
import kotlinx.coroutines.tasks.await

import com.charge.reservation.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.charge.reservation.booking.FirestorePaths
import com.charge.reservation.booking.toLottery

/**
 * A campsite lottery listing and its current waitlist count.
 *
 * @property lottery the configuration shared by the included campsites.
 * @property waitlistCount the number of current waitlist entries.
 */
data class FoundLottery(
    val lottery: Lottery,
    val waitlistCount: Int
)

/** Resolves lottery titles and observes the lotteries listed on a campsite. */
class LotteryLookupRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Returns a title based on an included campsite, or null if none has a name.
     * Firestore read or conversion errors propagate to the caller.
     */
    suspend fun getLotteryTitle(lotteryId: String): String? {
        val lottery = db.collection(FirestorePaths.LOTTERIES)
            .document(lotteryId)
            .get()
            .await()
            .toLottery()

        for (campsiteId in lottery.campsiteIds) {
            val campsite = db.collection(FirestorePaths.CAMPSITES)
                .document(campsiteId)
                .get()
                .await()

            val name = campsite.getString("name")
                ?.takeIf { it.isNotBlank() } ?: continue
            val otherCampsites = lottery.campsiteIds.size - 1

            return if (otherCampsites > 0) {
                "$name + $otherCampsites more"
            } else {
                name
            }
        }

        return null
    }

    /**
     * Watches all listings containing the campsite, with open registration first.
     *
     * @param campsiteId the campsite whose lotteries to load.
     * @param onSuccess receives the matching lotteries, or an empty list.
     * @param onFailure receives a Firestore or stored-data conversion error.
     * @return the listener registration, which the caller removes when leaving.
     */
    fun listenToCampsiteLotteries(
        campsiteId: String,
        onSuccess: (List<FoundLottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("lotteries")
            .whereArrayContains("campsiteIds", campsiteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: return@addSnapshotListener
                try {
                    val lotteries = documents.map { doc ->
                        FoundLottery(
                            lottery = doc.toLottery(),
                            waitlistCount = doc.getLong("waitlistCount")?.toInt() ?: 0
                        )
                    }

                    val now = Instant.now()

                    val orderedLotteries = lotteries.sortedByDescending { found ->
                        val start = found.lottery.registrationStartAt
                        val end = found.lottery.registrationEndAt

                        start != null &&
                                end != null &&
                                !now.isBefore(start) &&
                                now.isBefore(end) &&
                                found.lottery.drawCompletedAt == null
                    }

                    onSuccess(orderedLotteries)

                } catch (e: Exception) {
                    onFailure(e)
                }
            }
    }
}
