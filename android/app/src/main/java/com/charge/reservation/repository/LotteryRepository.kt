package com.charge.reservation.repository

import com.charge.reservation.model.Lottery
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class LotteryRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun generateLotteryId(): String {
        return firestore.collection("lotteries").document().id
    }

    /**
     * Creates a lottery for one registered campsite.
     */
    fun createLottery(
        lottery: Lottery,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            require(lottery.id.isNotBlank()) {
                "Lottery ID is missing."
            }
            require(lottery.ownerId.isNotBlank()) {
                "Owner ID is missing."
            }

            val campsiteId = requireNotNull(
                lottery.campsiteIds.singleOrNull()
            ) {
                "Select exactly one campsite."
            }
            require(campsiteId.isNotBlank()) {
                "Campsite ID is missing."
            }

            val opens = requireNotNull(lottery.registrationStartAt) {
                "Select the registration opening date and time."
            }
            val closes = requireNotNull(lottery.registrationEndAt) {
                "Select the registration closing date and time."
            }
            val deadline = requireNotNull(lottery.bookingDeadline) {
                "Select the booking deadline."
            }
            val checkIn = requireNotNull(lottery.bookingStartDate) {
                "Select the earliest check-in date."
            }
            val checkout = requireNotNull(lottery.bookingEndDate) {
                "Select the latest checkout date."
            }
            val zone = requireNotNull(lottery.timeZone) {
                "A time zone is required."
            }
            val winnerCount = requireNotNull(lottery.winnerCount) {
                "Enter the number of winners."
            }

            require(closes.isAfter(opens)) {
                "Registration must close after it opens."
            }
            require(checkout.isAfter(checkIn)) {
                "Checkout must be after check-in."
            }
            require(deadline.isAfter(closes)) {
                "Deadline must be after registration closes."
            }
            require(
                deadline.isBefore(checkIn.atStartOfDay(zone).toInstant())
            ) {
                "Deadline must be before the booking period begins."
            }
            require(winnerCount > 0) {
                "Winner count must be greater than zero."
            }

            val capacity = lottery.waitlistCapacity
            require(capacity == null || capacity > 0) {
                "Waiting-list capacity must be greater than zero."
            }
            require(lottery.drawCompletedAt == null) {
                "A new lottery cannot already have a completed draw."
            }

            val lotteryDocument = firestore.collection("lotteries")
                .document(lottery.id)

            val campsiteDocument = firestore.collection("campsites")
                .document(campsiteId)

            val lotteryData = mapOf<String, Any?>(
                "id" to lottery.id,
                "ownerId" to lottery.ownerId,
                "campsiteIds" to listOf(campsiteId),
                "registrationStartAt" to Timestamp(
                    opens.epochSecond, opens.nano
                ),
                "registrationEndAt" to Timestamp(
                    closes.epochSecond, closes.nano
                ),
                "bookingDeadline" to Timestamp(
                    deadline.epochSecond, deadline.nano
                ),
                "bookingStartDate" to checkIn.toString(),
                "bookingEndDate" to checkout.toString(),
                "timeZone" to zone.id,
                "winnerCount" to winnerCount,
                "waitlistCapacity" to capacity,
                "drawCompletedAt" to null
            )

            firestore.runTransaction { transaction ->
                val existingLottery = transaction.get(lotteryDocument)
                val campsite = transaction.get(campsiteDocument)

                check(!existingLottery.exists()) {
                    "A lottery already exists with this ID."
                }

                require(
                    campsite.exists() &&
                            campsite.getString("ownerId") == lottery.ownerId &&
                            campsite.getBoolean("removed") == false
                ) {
                    "Select an active campsite belonging to this owner."
                }

                transaction.set(lotteryDocument, lotteryData)
                Unit
            }
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }
}