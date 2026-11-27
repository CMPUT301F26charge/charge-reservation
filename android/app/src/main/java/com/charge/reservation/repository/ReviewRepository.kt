package com.charge.reservation.repository

import java.time.LocalDate
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

import com.charge.reservation.model.Rating
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus

/** The longest written review that can be posted. */
const val MAX_REVIEW_LENGTH = 1000

/** An error whose message is safe to show directly to the user. */
class ReviewException(message: String) : Exception(message)

/**
 * Whether a user may post a review for a campsite.
 *
 * @property message the explanation shown to the user, or empty if allowed
 */
enum class ReviewEligibility(val message: String) {
    CAN_REVIEW(""),
    ALREADY_REVIEWED("You have already reviewed this campsite."),
    NO_COMPLETED_STAY("You can review this campsite after you complete a stay here."),
    OWN_CAMPSITE("You can't review your own campsite.")
}

/**
 * Checks whether [entrantId] has a completed stay at [campsiteId].
 *
 * A stay counts once its reservation is still confirmed and its
 * check-out date is today or earlier.
 */
fun hasCompletedStay(
    reservations: List<Reservation>,
    campsiteId: String,
    entrantId: String,
    today: LocalDate
): Boolean = reservations.any { reservation ->
    reservation.campsiteId == campsiteId &&
            reservation.entrantId == entrantId &&
            reservation.status == ReservationStatus.CONFIRMED &&
            reservation.checkOutDate?.let { !it.isAfter(today) } == true
}

/** Finds a [ReviewException] anywhere in the cause chain (transactions can wrap exceptions). */
fun Throwable.reviewMessage(fallback: String): String =
    generateSequence(this) { it.cause }
        .filterIsInstance<ReviewException>()
        .firstOrNull()
        ?.message
        ?: fallback

/**
 * Reads and posts campsite reviews.
 *
 * Each review is stored in "ratings/<campsiteId>_<raterId>". Posting a review
 * also updates the campsite's ratingCount, ratingTotal, and averageRating
 * in the same transaction.
 */
class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()

    private fun ratingReference(campsiteId: String, raterId: String) =
        firestore.collection(RATINGS).document("${campsiteId}_${raterId}")

    /**
     * Listens for every review of a campsite, including those without text.
     *
     * @param onSuccess receives the reviews, newest first, whenever they change.
     * @param onFailure receives Firestore or data conversion errors.
     * @return the listener registration, remove it when no longer needed.
     */
    fun listenToCampsiteReviews(
        campsiteId: String,
        onSuccess: (List<Rating>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(RATINGS)
            .whereEqualTo("campsiteId", campsiteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val reviews = try {
                    snapshot?.documents
                        ?.mapNotNull { it.toRating() }
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addSnapshotListener
                }

                onSuccess(reviews)
            }
    }

    /**
     * Checks whether the user may post a review for the campsite.
     *
     * @param today the date used to decide whether a stay has finished.
     * @param onSuccess receives the eligibility result.
     * @param onFailure receives validation or Firestore errors.
     */
    fun getReviewEligibility(
        campsiteId: String,
        userId: String,
        today: LocalDate = LocalDate.now(),
        onSuccess: (ReviewEligibility) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (campsiteId.isBlank() || userId.isBlank()) {
            onFailure(IllegalArgumentException("Campsite ID and user ID must not be blank."))
            return
        }

        val campsiteTask = firestore.collection("campsites")
            .document(campsiteId)
            .get(Source.SERVER)

        val reviewTask = ratingReference(campsiteId, userId).get(Source.SERVER)

        val reservationsTask = firestore.collection("reservations")
            .whereEqualTo("entrantId", userId)
            .whereEqualTo("campsiteId", campsiteId)
            .get(Source.SERVER)

        Tasks.whenAll(campsiteTask, reviewTask, reservationsTask)
            .addOnSuccessListener {
                val eligibility = try {
                    val reservations = reservationsTask.result.documents.mapNotNull { document ->
                        val status = document.getString("status")?.let { value ->
                            ReservationStatus.values().firstOrNull { it.name == value }
                        } ?: return@mapNotNull null

                        Reservation(
                            id = document.id,
                            campsiteId = document.getString("campsiteId").orEmpty(),
                            entrantId = document.getString("entrantId").orEmpty(),
                            checkOutDate = ReservationValueParser.parseDate(
                                document.get("checkOutDate")
                            ),
                            status = status
                        )
                    }

                    when {
                        reviewTask.result.exists() -> ReviewEligibility.ALREADY_REVIEWED

                        campsiteTask.result.getString("ownerId") == userId ->
                            ReviewEligibility.OWN_CAMPSITE

                        !hasCompletedStay(reservations, campsiteId, userId, today) ->
                            ReviewEligibility.NO_COMPLETED_STAY

                        else -> ReviewEligibility.CAN_REVIEW
                    }
                } catch (exception: Exception) {
                    onFailure(exception)
                    return@addOnSuccessListener
                }

                onSuccess(eligibility)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Posts the user's review and updates the campsite's average rating.
     *
     * Checks eligibility first, then saves inside a transaction that refuses
     * to overwrite an existing review.
     *
     * @param score the star rating from 1 to 5.
     * @param text the optional written review. Blank text is saved as null.
     * @param onSuccess called after the review and new average are saved.
     * @param onFailure receives a [ReviewException] for user-facing problems,
     * or a Firestore error.
     */
    fun createReview(
        campsiteId: String,
        userId: String,
        score: Int,
        text: String,
        today: LocalDate = LocalDate.now(),
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reviewText = text.trim().ifBlank { null }

        val validationError = when {
            campsiteId.isBlank() || userId.isBlank() -> "This campsite can't be reviewed."
            score !in 1..5 -> "Please choose a rating from 1 to 5 stars."
            (reviewText?.length ?: 0) > MAX_REVIEW_LENGTH ->
                "Reviews can be at most $MAX_REVIEW_LENGTH characters."
            else -> null
        }

        if (validationError != null) {
            onFailure(ReviewException(validationError))
            return
        }

        getReviewEligibility(
            campsiteId = campsiteId,
            userId = userId,
            today = today,
            onSuccess = { eligibility ->
                if (eligibility == ReviewEligibility.CAN_REVIEW) {
                    saveReview(campsiteId, userId, score, reviewText, onSuccess, onFailure)
                } else {
                    onFailure(ReviewException(eligibility.message))
                }
            },
            onFailure = onFailure
        )
    }

    /**
     * Saves the review and the campsite's new rating totals together.
     */
    private fun saveReview(
        campsiteId: String,
        userId: String,
        score: Int,
        reviewText: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reviewDocument = ratingReference(campsiteId, userId)
        val campsiteDocument = firestore.collection("campsites").document(campsiteId)
        val userDocument = firestore.collection("users").document(userId)

        firestore.runTransaction { transaction ->
            val existingReview = transaction.get(reviewDocument)
            val campsite = transaction.get(campsiteDocument)
            val user = transaction.get(userDocument)

            if (existingReview.exists()) {
                throw ReviewException(ReviewEligibility.ALREADY_REVIEWED.message)
            }
            if (!campsite.exists()) {
                throw ReviewException("This campsite no longer exists.")
            }
            if (campsite.getString("ownerId") == userId) {
                throw ReviewException(ReviewEligibility.OWN_CAMPSITE.message)
            }

            val ratingCount = (campsite.getLong("ratingCount") ?: 0L) + 1L
            val ratingTotal = (campsite.getLong("ratingTotal") ?: 0L) + score

            transaction.set(
                reviewDocument,
                mapOf(
                    "campsiteId" to campsiteId,
                    "raterId" to userId,
                    "raterName" to user.getString("name"),
                    "score" to score,
                    "text" to reviewText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            transaction.update(
                campsiteDocument,
                mapOf(
                    "ratingCount" to ratingCount,
                    "ratingTotal" to ratingTotal,
                    "averageRating" to ratingTotal.toDouble() / ratingCount
                )
            )

            Unit
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    /**
     * Converts a review document, or returns null if its score is invalid.
     */
    private fun DocumentSnapshot.toRating(): Rating? {
        val score = getLong("score")?.toInt()?.takeIf { it in 1..5 } ?: return null

        return Rating(
            campsiteId = getString("campsiteId").orEmpty(),
            raterId = getString("raterId").orEmpty(),
            score = score,
            text = getString("text")?.takeIf { it.isNotBlank() },
            raterName = getString("raterName"),
            // Estimate the time of a review this device has just posted.
            createdAt = getTimestamp(
                "createdAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDate()?.toInstant()
        )
    }

    private companion object {
        const val RATINGS = "ratings"
    }
}
