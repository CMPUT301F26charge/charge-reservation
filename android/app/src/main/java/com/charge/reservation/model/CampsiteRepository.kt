
package com.charge.reservation.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CampsiteRepository(
    private val db: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    // ADDED: Listen for changes to an owner's campsites.
    fun listenToOwnerCampsites(
        ownerId: String,
        onResult: (List<Campsite>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return db.collection("campsites")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("removed", false)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val campsites = snapshot?.documents?.mapNotNull {
                        document ->
                    document.toObject(Campsite::class.java)
                        ?.copy(id = document.id)
                } ?: emptyList()

                onResult(campsites)
            }
    }
}
