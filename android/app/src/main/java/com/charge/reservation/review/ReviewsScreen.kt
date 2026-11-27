package com.charge.reservation.review

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

import com.charge.reservation.model.Rating
import com.charge.reservation.repository.ReviewEligibility
import com.charge.reservation.repository.ReviewRepository

private const val TAG = "Reviews"

/** The colour used for stars, matching the campsite details page. */
internal val StarColor = Color(0xFF996A00)

/** Formats an average rating to two decimal places, such as "4.25". */
fun formatRating(average: Double): String =
    String.format(Locale.US, "%.2f", average)

/** Shows [filled] solid stars followed by hollow stars, five in total. */
internal fun starText(filled: Int): String {
    val stars = filled.coerceIn(0, 5)
    return "★".repeat(stars) + "☆".repeat(5 - stars)
}

/** The average of all valid scores, or null if there are none. */
internal fun averageScore(reviews: List<Rating>): Double? =
    reviews.mapNotNull { it.score }
        .takeIf { it.isNotEmpty() }
        ?.average()

/**
 * Shows a campsite's average rating and its written reviews.
 *
 * Every review counts toward the average, but only reviews with text are
 * listed. The button at the bottom opens the review form when the user
 * has a completed stay and has not yet reviewed the campsite.
 *
 * @param campsiteId the campsite whose reviews are shown
 * @param userId the signed-in user's Firebase UID
 * @param onWriteReviewClick opens the review form
 */
@Composable
fun ReviewsScreen(
    campsiteId: String,
    userId: String,
    onWriteReviewClick: () -> Unit,
    repository: ReviewRepository = remember { ReviewRepository() }
) {
    var reviews by remember(campsiteId) { mutableStateOf<List<Rating>>(emptyList()) }
    var isLoadingReviews by remember(campsiteId) { mutableStateOf(true) }
    var reviewsError by remember(campsiteId) { mutableStateOf("") }

    var eligibility by remember(campsiteId, userId) {
        mutableStateOf<ReviewEligibility?>(null)
    }
    var eligibilityError by remember(campsiteId, userId) { mutableStateOf("") }

    DisposableEffect(campsiteId) {
        isLoadingReviews = true
        reviewsError = ""

        val listener = repository.listenToCampsiteReviews(
            campsiteId = campsiteId,
            onSuccess = { loaded ->
                reviews = loaded
                reviewsError = ""
                isLoadingReviews = false
            },
            onFailure = { exception ->
                Log.e(TAG, "Could not load reviews", exception)
                reviewsError = "Could not load reviews. Please try again."
                isLoadingReviews = false
            }
        )

        onDispose { listener.remove() }
    }

    // Runs again each time this screen opens, such as after posting a review.
    DisposableEffect(campsiteId, userId) {
        var active = true

        eligibility = null
        eligibilityError = ""

        repository.getReviewEligibility(
            campsiteId = campsiteId,
            userId = userId,
            onSuccess = { result ->
                if (active) eligibility = result
            },
            onFailure = { exception ->
                if (active) {
                    Log.e(TAG, "Could not check review eligibility", exception)
                    eligibilityError =
                        "Could not check whether you can review this campsite."
                }
            }
        )

        onDispose { active = false }
    }

    // The user's own review appears in the live list before eligibility reloads.
    val currentEligibility =
        if (reviews.any { it.raterId == userId }) {
            ReviewEligibility.ALREADY_REVIEWED
        } else {
            eligibility
        }

    val writtenReviews = reviews.filter { !it.text.isNullOrBlank() }
    val average = averageScore(reviews)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Reviews",
            style = MaterialTheme.typography.headlineSmall
        )

        RatingSummary(
            average = average,
            ratingCount = reviews.size,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoadingReviews -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                reviewsError.isNotEmpty() -> Text(
                    text = reviewsError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                writtenReviews.isEmpty() -> Text(
                    text = "No written reviews yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(writtenReviews, key = { it.raterId }) { review ->
                        ReviewCard(review)
                    }
                }
            }
        }

        Button(
            onClick = onWriteReviewClick,
            enabled = currentEligibility == ReviewEligibility.CAN_REVIEW,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (currentEligibility == ReviewEligibility.ALREADY_REVIEWED) {
                    "You've reviewed this campsite"
                } else {
                    "Write a review"
                }
            )
        }

        val helperText = when {
            eligibilityError.isNotEmpty() -> eligibilityError
            currentEligibility == null -> "Checking whether you can review this campsite…"
            else -> currentEligibility.message
        }

        if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Shows the average rating with two decimal places and the number of ratings.
 */
@Composable
private fun RatingSummary(
    average: Double?,
    ratingCount: Int,
    modifier: Modifier = Modifier
) {
    if (average == null) {
        Text(
            text = "No ratings yet",
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatRating(average),
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = starText(average.roundToInt()),
                color = StarColor,
                fontSize = 22.sp,
                modifier = Modifier.semantics {
                    contentDescription = "${formatRating(average)} out of 5 stars"
                }
            )
        }

        Text(
            text = if (ratingCount == 1) "Based on 1 rating" else "Based on $ratingCount ratings",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Shows one written review.
 */
@Composable
private fun ReviewCard(review: Rating) {
    val score = review.score ?: 0
    val dateFormat = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDDDDDD))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = starText(score),
                    color = StarColor,
                    modifier = Modifier.semantics {
                        contentDescription = "$score out of 5 stars"
                    }
                )

                review.createdAt?.let { postedAt ->
                    Text(
                        text = dateFormat.format(postedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }

            Text(
                text = review.raterName?.takeIf { it.isNotBlank() } ?: "Anonymous camper",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black
            )

            Text(
                text = review.text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF333333)
            )
        }
    }
}
