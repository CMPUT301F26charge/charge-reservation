package com.charge.reservation.repository

import com.charge.reservation.booking.toLottery
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.MAX_LOTTERY_CAMPSITES
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions

/**
 * Loads, publishes, and updates owner lotteries.
 */
class LotteryRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Generates an unused lottery document ID without saving a document.
     *
     * @return the ID to retain while the creation form is open.
     */
    fun generateLotteryId(): String {
        return firestore.collection("lotteries").document().id
    }

    /**
     * Publishes one lottery for the selected active campsites of the same owner.
     *
     * Validates the shared settings and checks each campsite in the transaction
     * that saves the lottery.
     * An existing lottery with the same ID is never overwritten.
     *
     * @param lottery the configuration with distinct campsite IDs and shared settings.
     * @param onSuccess called after the lottery is successfully saved.
     * @param onFailure receives validation, ownership, duplicate ID or
     * Firestore errors. A failed save does not report success.
     */
    fun createLottery(
        lottery: Lottery,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = saveLottery(lottery, isUpdate = false, onSuccess, onFailure)

    /**
     * Saves new settings for an undrawn lottery owned by [Lottery.ownerId].
     *
     * Uses the latest draw state and waitlist count in the transaction. Keeps
     * the same lottery ID, owner, time zone, contributions, and participation records.
     *
     * @param lottery the updated settings and the creator's selected campsite IDs.
     * @param onSuccess called after the update commits.
     * @param onFailure receives validation or Firestore errors.
     */
    fun updateLottery(
        lottery: Lottery,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = saveLottery(lottery, isUpdate = true, onSuccess, onFailure)

    private fun saveLottery(
        lottery: Lottery,
        isUpdate: Boolean,
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

            val campsiteIds = lottery.campsiteIds.toList()
            require(campsiteIds.size in 1..MAX_LOTTERY_CAMPSITES) {
                "Select between 1 and $MAX_LOTTERY_CAMPSITES campsites."
            }
            require(campsiteIds.none { it.isBlank() || '/' in it }) {
                "A selected campsite has an invalid ID."
            }
            require(campsiteIds.toSet().size == campsiteIds.size) {
                "Select each campsite only once."
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
            val lotteryDocument = firestore.collection("lotteries")
                .document(lottery.id)

            val settings = mapOf<String, Any?>(
                "campsiteIds" to campsiteIds,
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
                "winnerCount" to winnerCount,
                "waitlistCapacity" to capacity
            )

            firestore.runTransaction { transaction ->
                val existingLottery = transaction.get(lotteryDocument)

                if (isUpdate) {
                    require(existingLottery.exists() &&
                            existingLottery.getString("ownerId") == lottery.ownerId) {
                        "Only the owner can edit this lottery."
                    }
                    require(existingLottery.get("drawCompletedAt") == null) {
                        "This lottery has already been drawn and can no longer be edited."
                    }
                    require(existingLottery.getString("timeZone") == zone.id) {
                        "The lottery's time zone changed. Reopen the editor and try again."
                    }

                    val waitlistCount = existingLottery.getLong("waitlistCount")
                    require(waitlistCount != null && waitlistCount >= 0L) {
                        "This lottery's waitlist count is unavailable."
                    }
                    require(capacity == null || capacity.toLong() >= waitlistCount) {
                        "Waitlist limit must be at least $waitlistCount, or blank for no limit."
                    }
                } else {
                    require(lottery.drawCompletedAt == null) {
                        "A new lottery cannot already have a completed draw."
                    }
                    check(!existingLottery.exists()) {
                        "A lottery already exists with this ID."
                    }
                }

                for (campsiteId in campsiteIds) {
                    val campsite = transaction.get(
                        firestore.collection("campsites").document(campsiteId)
                    )
                    require(
                        campsite.exists() &&
                                campsite.getString("ownerId") == lottery.ownerId &&
                                campsite.getBoolean("removed") == false
                    ) {
                        "All selected campsites must be active and belong to this owner."
                    }
                }

                if (isUpdate) {
                    val contributions = existingLottery.toLottery().contributedCampsiteIds
                    val includedCampsites = (campsiteIds + contributions).distinct()
                    require(includedCampsites.size <= MAX_LOTTERY_CAMPSITES) {
                        "Select up to $MAX_LOTTERY_CAMPSITES campsites, including contributions."
                    }
                    transaction.update(lotteryDocument, settings + ("campsiteIds" to includedCampsites))
                } else {
                    transaction.set(
                        lotteryDocument,
                        settings + mapOf(
                            "id" to lottery.id,
                            "ownerId" to lottery.ownerId,
                            "timeZone" to zone.id,
                            "waitlistCount" to 0L,
                            "drawCompletedAt" to null
                        )
                    )
                }
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
     * Reads the latest settings before opening an owner's lottery editor.
     *
     * @param lotteryId document ID from the owner details page.
     * @param ownerId the signed-in owner's Firebase Authentication UID.
     * @param onSuccess receives the saved lottery, including its draw state.
     * @param onFailure receives a missing lottery, ownership, or Firestore error.
     */
    fun getOwnerLottery(
        lotteryId: String,
        ownerId: String,
        onSuccess: (Lottery) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("lotteries").document(lotteryId)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                val lottery = try {
                    require(document.exists() && document.getString("ownerId") == ownerId) {
                        "Lottery not found or you don't have permission to edit it."
                    }
                    document.toLottery()
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }
                onSuccess(lottery)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Loads the lotteries created by an owner or containing a campsite they contributed.
     *
     * @param ownerId the signed-in owner's Firebase Authentication UID.
     * @param onSuccess receives the lotteries, newest registration first.
     * @param onFailure receives Firestore or stored data conversion errors.
     */
    fun getOwnerLotteries(
        ownerId: String,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("lotteries")
            .where(
                Filter.or(
                    Filter.equalTo("ownerId", ownerId),
                    Filter.arrayContains("contributingOwnerIds", ownerId)
                )
            )
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val lotteries = try {
                    snapshot.documents.map { it.toLottery() }
                        .sortedByDescending { it.registrationStartAt }
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }
                onSuccess(lotteries)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Loads the shared and created lotteries that include the current campsite.
     *
     * @param ownerId the signed-in owner's Firebase Authentication UID.
     * @param campsiteId filters the results to lotteries containing this campsite.
     * @param onSuccess receives the matching lotteries.
     * @param onFailure receives load or conversion errors.
     */
    fun getOwnerCampsiteLotteries(
        ownerId: String,
        campsiteId: String,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getOwnerLotteries(
        ownerId,
        onSuccess = { lotteries -> onSuccess(lotteries.filter { campsiteId in it.campsiteIds }) },
        onFailure = onFailure
    )

    /**
     * Requests a single-use share code for a lottery created by the signed-in owner.
     *
     * @param lotteryId the lottery to share.
     * @param onSuccess receives the invitation code.
     * @param onFailure receives a validation or Firebase error.
     */
    fun createSharedLotteryInvitation(
        lotteryId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseFunctions.getInstance().getHttpsCallable("createSharedLotteryInvitation")
            .call(mapOf("lotteryId" to lotteryId))
            .addOnSuccessListener { result ->
                val code = (result.data as? Map<*, *>)?.get("code") as? String
                if (code == null) onFailure(IllegalStateException("The invite code could not be read."))
                else onSuccess(code)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Redeems a code by adding one of the signed-in owner's active campsites.
     *
     * @param code the invitation code shared by the creator.
     * @param campsiteId the campsite to contribute.
     * @param onSuccess runs after the campsite is added.
     * @param onFailure receives a validation or Firebase error.
     */
    fun acceptSharedLotteryInvitation(
        code: String,
        campsiteId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseFunctions.getInstance().getHttpsCallable("acceptSharedLotteryInvitation")
            .call(mapOf("code" to code, "campsiteId" to campsiteId))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
