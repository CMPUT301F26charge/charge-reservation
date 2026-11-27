package com.charge.reservation.review

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.charge.reservation.repository.MAX_REVIEW_LENGTH
import com.charge.reservation.repository.ReviewRepository
import com.charge.reservation.repository.reviewMessage

private const val TAG = "CreateReview"

/**
 * Lets the user rate a campsite from 1 to 5 stars and optionally write a review.
 *
 * The repository checks that the user has a completed stay and has not
 * already reviewed the campsite before saving.
 *
 * @param campsiteId the campsite being reviewed
 * @param userId the signed-in user's Firebase UID
 * @param onReviewPosted called after the review is saved
 */
@Composable
fun CreateReviewScreen(
    campsiteId: String,
    userId: String,
    onReviewPosted: () -> Unit,
    repository: ReviewRepository = remember { ReviewRepository() }
) {
    var score by rememberSaveable(campsiteId) { mutableStateOf(0) }
    var text by rememberSaveable(campsiteId) { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Write a review",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "How was your stay?",
            style = MaterialTheme.typography.bodyMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..5) {
                    IconButton(
                        onClick = {
                            score = star
                            errorMessage = ""
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .size(52.dp)
                            .semantics {
                                contentDescription =
                                    if (star == 1) "1 star" else "$star stars"
                                selected = star == score
                            }
                    ) {
                        Text(
                            text = if (star <= score) "★" else "☆",
                            fontSize = 36.sp,
                            color = if (star <= score) {
                                StarColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                }
            }

            Text(
                text = if (score == 0) "Tap a star to rate" else "$score out of 5 stars",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                if (newText.length <= MAX_REVIEW_LENGTH) {
                    text = newText
                    errorMessage = ""
                }
            },
            label = { Text("Review (optional)") },
            supportingText = { Text("${text.length} / $MAX_REVIEW_LENGTH") },
            minLines = 4,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Only reviews with written text appear in the review list. " +
                "Your star rating always counts toward the campsite's average.",
            style = MaterialTheme.typography.bodySmall
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = score in 1..5 && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                isSaving = true
                errorMessage = ""

                repository.createReview(
                    campsiteId = campsiteId,
                    userId = userId,
                    score = score,
                    text = text,
                    onSuccess = {
                        isSaving = false
                        onReviewPosted()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Could not post review", exception)
                        isSaving = false
                        errorMessage = exception.reviewMessage(
                            "Could not post your review. Please try again."
                        )
                    }
                )
            }
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Post review")
            }
        }
    }
}
