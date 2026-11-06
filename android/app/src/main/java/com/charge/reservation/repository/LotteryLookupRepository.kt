package com.charge.reservation.repository

import com.charge.reservation.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.Instant

data class FoundLottery(
    val lottery: Lottery,
    val waitlistCount: Int
)

class LotteryLookupRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getForCampsite(
        campsiteId: String,
        onSuccess: (FoundLottery?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("lotteries")
            .whereArrayContains("campsiteIds", campsiteId)
            .get(Source.SERVER)
            .addOnSuccessListener { result ->
                try {
                    val lotteries = result.documents.map { doc ->
                        val lottery = Lottery(
                            id = doc.id,
                            ownerId = doc.getString("ownerId") ?: "",
                            campsiteIds = (
                                    doc.get("campsiteIds") as? List<*>
                                    )?.filterIsInstance<String>().orEmpty(),
                            registrationStartAt = doc
                                .getTimestamp("registrationStartAt")
                                ?.toDate()?.toInstant(),
                            registrationEndAt = doc
                                .getTimestamp("registrationEndAt")
                                ?.toDate()?.toInstant(),
                            waitlistCapacity = doc
                                .getLong("waitlistCapacity")?.toInt(),
                            drawCompletedAt = doc
                                .getTimestamp("drawCompletedAt")
                                ?.toDate()?.toInstant()
                        )

                        FoundLottery(
                            lottery = lottery,
                            waitlistCount = doc
                                .getLong("waitlistCount")?.toInt() ?: 0
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
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}