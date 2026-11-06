
package com.charge.reservation.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

data class LotteryEntrantRow(
    val entrantId: String,
    val status: String
)

class LotteryEntrantsRepository(
    private val db: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {
    /**
     * Loads entrants for a lottery owned by the current owner.
     */
    fun getEntrants(
        lotteryId: String,
        ownerId: String,
        onSuccess: (List<LotteryEntrantRow>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (lotteryId.isBlank() || ownerId.isBlank()) {
            onFailure(
                IllegalArgumentException("Missing lottery or owner ID.")
            )
            return
        }

        val lotteryRef = db.collection("lotteries")
            .document(lotteryId)

        lotteryRef.get(Source.SERVER)
            .addOnSuccessListener { lottery ->
                if (!lottery.exists() ||
                    lottery.getString("ownerId") != ownerId
                ) {
                    onFailure(
                        IllegalStateException(
                            "Lottery not found or access denied."
                        )
                    )
                    return@addOnSuccessListener
                }

                lotteryRef.collection("participants")
                    .get(Source.SERVER)
                    .addOnSuccessListener { result ->
                        val entrants = result.documents.map { doc ->
                            LotteryEntrantRow(
                                entrantId = doc.getString("entrantId")
                                    ?: doc.id,
                                status = doc.getString("status")
                                    ?: "UNKNOWN"
                            )
                        }.sortedWith(
                            compareBy<LotteryEntrantRow>(
                                { it.status == "WITHDRAWN" },
                                { it.entrantId }
                            )
                        )

                        onSuccess(entrants)
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
}
