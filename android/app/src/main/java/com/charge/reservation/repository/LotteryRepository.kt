package com.charge.reservation.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

import com.charge.reservation.model.Lottery
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source


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

    /**
     * Loads saved lotteries for the selected owner's campsite.
     *
     * @param ownerId the current owner's Firebase Authentication UID.
     * @param campsiteId the campsite whose lotteries should be returned.
     * @param onSuccess receives matching lotteries, newest registration first.
     * @param onFailure receives Firestore or stored data conversion errors.
     */
    fun getOwnerCampsiteLotteries(
        ownerId: String,
        campsiteId: String,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("lotteries")
            .whereEqualTo("ownerId", ownerId)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val lotteries = try {
                    val result = mutableListOf<Lottery>()

                    for (document in snapshot.documents) {
                        val campsiteIds =
                            (document.get("campsiteIds") as List<*>)
                                .map { it as String }

                        if (campsiteId !in campsiteIds) {
                            continue
                        }

                        result.add(
                            Lottery(
                                id = document.id,
                                ownerId = requireNotNull(
                                    document.getString("ownerId")
                                ),
                                campsiteIds = campsiteIds,
                                registrationStartAt = timestampToInstant(
                                    requireNotNull(
                                        document.getTimestamp("registrationStartAt")
                                    )
                                ),
                                registrationEndAt = timestampToInstant(
                                    requireNotNull(
                                        document.getTimestamp("registrationEndAt")
                                    )
                                ),
                                bookingDeadline = timestampToInstant(
                                    requireNotNull(
                                        document.getTimestamp("bookingDeadline")
                                    )
                                ),
                                bookingStartDate = LocalDate.parse(
                                    requireNotNull(
                                        document.getString("bookingStartDate")
                                    )
                                ),
                                bookingEndDate = LocalDate.parse(
                                    requireNotNull(
                                        document.getString("bookingEndDate")
                                    )
                                ),
                                timeZone = ZoneId.of(
                                    requireNotNull(
                                        document.getString("timeZone")
                                    )
                                ),
                                winnerCount = Math.toIntExact(
                                    requireNotNull(
                                        document.getLong("winnerCount")
                                    )
                                ),
                                waitlistCapacity =
                                    document.getLong("waitlistCapacity")?.let {
                                        Math.toIntExact(it)
                                    },
                                drawCompletedAt =
                                    document.getTimestamp("drawCompletedAt")?.let {
                                        timestampToInstant(it)
                                    }
                            )
                        )
                    }

                    result.sortedByDescending { it.registrationStartAt }
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }

                onSuccess(lotteries)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Converts a saved Firebase timestamp into an Instant.
     */
    private fun timestampToInstant(timestamp: Timestamp): Instant {
        return Instant.ofEpochSecond(
            timestamp.seconds,
            timestamp.nanoseconds.toLong()
        )
    }
}