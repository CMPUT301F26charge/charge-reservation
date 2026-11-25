
package com.charge.reservation.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
data class LotteryEntrantRow(
    val entrantId: String,
    val status: String,
    val name: String = "Unknown entrant"
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

                db.collection("lotteryParticipations")
                    .whereEqualTo("lotteryId", lotteryId)
                    .get(Source.SERVER)
                    .addOnSuccessListener { result ->
                        val entrants = result.documents
                            .filter { doc ->
                                doc.getString("status") != "WITHDRAWN"
                            }
                            .map { doc ->
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

                        if (entrants.isEmpty()) {
                            onSuccess(entrants)
                        } else {
                            val profileTasks = entrants.map { entrant ->
                                db.collection("users")
                                    .document(entrant.entrantId)
                                    .get(Source.SERVER)
                            }

                            Tasks.whenAllComplete(profileTasks)
                                .addOnSuccessListener { completedTasks ->
                                    val entrantsWithNames = entrants.mapIndexed { index, entrant ->
                                        val task = completedTasks[index]
                                        val user = if (task.isSuccessful) {
                                            task.result as? DocumentSnapshot
                                        } else {
                                            null
                                        }

                                        entrant.copy(
                                            name = user?.getString("name")
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "Unknown entrant"
                                        )
                                    }

                                    onSuccess(entrantsWithNames)
                                }
                                .addOnFailureListener {
                                    onSuccess(entrants)
                                }
                        }

                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
}
