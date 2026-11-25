package com.charge.reservation.repository

import java.time.Instant
import kotlinx.coroutines.tasks.await

import com.charge.reservation.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.charge.reservation.booking.FirestorePaths
import com.charge.reservation.booking.toLottery

data class FoundLottery(
    val lottery: Lottery,
    val waitlistCount: Int
)

class LotteryLookupRepository {

    private val db = FirebaseFirestore.getInstance()

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

    fun listenToCampsiteLottery(
        campsiteId: String,
        onSuccess: (FoundLottery?) -> Unit,
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

                    val activeLottery = lotteries.firstOrNull { found ->
                        val start = found.lottery.registrationStartAt
                        val end = found.lottery.registrationEndAt

                        start != null &&
                                end != null &&
                                !now.isBefore(start) &&
                                now.isBefore(end) &&
                                found.lottery.drawCompletedAt == null
                    }

                    onSuccess(activeLottery ?: lotteries.firstOrNull())

                } catch (e: Exception) {
                    onFailure(e)
                }
            }
    }
}