
package com.charge.reservation.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * US 02.04.07: Manages lottery waitlist membership.
 *
 * The lottery document holds waitlistCount and waitlistCapacity.
 * Each lottery also has a participants subcollection.
 *
 * Firestore transactions prevent simultaneous joins from
 * exceeding the waitlist capacity.
 */
class WaitlistRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    /**
     * Adds an entrant to a lottery if registration is open
     * and there is available waitlist capacity.
     *
     * @param lotteryId ID of the selected lottery.
     * @param entrantId Firebase UID of the entrant.
     * @param onSuccess Called after a successful join.
     * @param onFailure Called if the join is rejected.
     */
    fun joinWaitlist(
        lotteryId: String,
        entrantId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (lotteryId.isBlank() || entrantId.isBlank()) {
            onFailure(
                IllegalArgumentException(
                    "Lottery and entrant IDs are required."
                )
            )
            return
        }

        val lotteryRef = firestore
            .collection("lotteries")
            .document(lotteryId)

        val participantRef = lotteryRef
            .collection("participants")
            .document(entrantId)

        firestore.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
            val participant = transaction.get(participantRef)

            if (!lottery.exists()) {
                throw IllegalStateException(
                    "Lottery not found."
                )
            }

            val now = Timestamp.now()
            val opens = lottery.getTimestamp("registrationStartAt")
            val closes = lottery.getTimestamp("registrationEndAt")

            if (
                opens == null ||
                closes == null ||
                now.compareTo(opens) < 0 ||
                now.compareTo(closes) >= 0 ||
                lottery.getTimestamp("drawCompletedAt") != null
            ) {
                throw IllegalStateException(
                    "Lottery registration is closed."
                )
            }

            val existingStatus =
                if (participant.exists()) {
                    participant.getString("status")
                } else {
                    null
                }

            if (participant.exists() &&
                existingStatus != "WITHDRAWN"
            ) {
                throw IllegalStateException(
                    "You have already joined this lottery."
                )
            }

            val count = lottery.getLong("waitlistCount")
                ?: throw IllegalStateException(
                    "Lottery waitlist count is not initialized."
                )

            val capacity = lottery.getLong("waitlistCapacity")

            if (capacity != null && count >= capacity) {
                throw IllegalStateException(
                    "This lottery waitlist is full."
                )
            }

            transaction.set(
                participantRef,
                mapOf(
                    "lotteryId" to lotteryId,
                    "entrantId" to entrantId,
                    "joinedAt" to now,
                    "status" to "WAITING",
                    "reservationId" to null
                )
            )

            transaction.update(
                lotteryRef,
                "waitlistCount",
                count + 1L
            )

            Unit
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Withdraws an entrant who is currently waiting.
     *
     * The same transaction updates the participation status
     * and frees one waitlist place.
     */
    fun leaveWaitlist(
        lotteryId: String,
        entrantId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (lotteryId.isBlank() || entrantId.isBlank()) {
            onFailure(
                IllegalArgumentException(
                    "Lottery and entrant IDs are required."
                )
            )
            return
        }

        val lotteryRef = firestore
            .collection("lotteries")
            .document(lotteryId)

        val participantRef = lotteryRef
            .collection("participants")
            .document(entrantId)

        firestore.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
            val participant = transaction.get(participantRef)

            if (!lottery.exists()) {
                throw IllegalStateException(
                    "Lottery not found."
                )
            }

            if (!participant.exists() ||
                participant.getString("status") != "WAITING"
            ) {
                throw IllegalStateException(
                    "No active waiting entry was found."
                )
            }

            val count = lottery.getLong("waitlistCount")
                ?: throw IllegalStateException(
                    "Lottery waitlist count is not initialized."
                )

            if (count <= 0L) {
                throw IllegalStateException(
                    "Waitlist count is inconsistent."
                )
            }

            transaction.update(
                participantRef,
                "status",
                "WITHDRAWN"
            )

            transaction.update(
                lotteryRef,
                "waitlistCount",
                count - 1L
            )

            Unit
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Allows the owner to update the waitlist capacity.
     *
     * Null means unlimited.
     * The new limit cannot be smaller than current membership.
     */
    fun updateWaitlistCapacity(
        lotteryId: String,
        ownerId: String,
        newCapacity: Int?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (lotteryId.isBlank() || ownerId.isBlank()) {
            onFailure(
                IllegalArgumentException(
                    "Lottery and owner IDs are required."
                )
            )
            return
        }

        if (newCapacity != null && newCapacity <= 0) {
            onFailure(
                IllegalArgumentException(
                    "Capacity must be a positive whole number."
                )
            )
            return
        }

        val lotteryRef = firestore
            .collection("lotteries")
            .document(lotteryId)

        firestore.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)

            if (!lottery.exists()) {
                throw IllegalStateException(
                    "Lottery not found."
                )
            }

            if (lottery.getString("ownerId") != ownerId) {
                throw IllegalStateException(
                    "Only the lottery owner can change capacity."
                )
            }

            val count = lottery.getLong("waitlistCount")
                ?: throw IllegalStateException(
                    "Lottery waitlist count is not initialized."
                )

            if (newCapacity != null &&
                newCapacity.toLong() < count
            ) {
                throw IllegalArgumentException(
                    "Capacity cannot be smaller than " +
                            "the current waitlist count."
                )
            }

            transaction.update(
                lotteryRef,
                "waitlistCapacity",
                newCapacity
            )

            Unit
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
