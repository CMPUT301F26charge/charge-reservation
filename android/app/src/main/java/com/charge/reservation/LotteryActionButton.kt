package com.charge.reservation

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charge.reservation.model.Lottery
import com.charge.reservation.model.LotteryParticipation
import com.charge.reservation.model.ParticipationStatus
import com.charge.reservation.LotteryActionException
import com.charge.reservation.LotteryParticipationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

private const val TAG = "LotteryAction"

/** What the single lottery button on the campsite page should say and whether it is tappable. */
enum class LotteryButtonState(val label: String, val enabled: Boolean) {
    UNAVAILABLE("No available lottery", false),
    JOIN("Join lottery", true),
    LEAVE("Leave lottery", true)
}

/**
 * Decides the button state. Pure function (no Firebase, no Compose), so it is easy to unit test.
 *
 *  - LEAVE: the entrant is WAITING in the lottery and the draw hasn't happened.
 *  - JOIN: registration is open, the draw hasn't happened, the waitlist has room,
 *    and the entrant never joined or previously withdrew.
 *  - UNAVAILABLE: everything else (no lottery, closed, full, already drawn, or the entrant
 *    already has an invitation / result).
 */
fun lotteryButtonState(
    lottery: Lottery?,
    participation: LotteryParticipation?,
    waitlistCount: Int,
    now: Instant = Instant.now()
): LotteryButtonState {
    if (lottery == null) return LotteryButtonState.UNAVAILABLE

    val drawDone = lottery.drawCompletedAt != null

    if (participation?.status == ParticipationStatus.WAITING) {
        return if (drawDone) LotteryButtonState.UNAVAILABLE else LotteryButtonState.LEAVE
    }

    val start = lottery.registrationStartAt
    val end = lottery.registrationEndAt
    val registrationOpen = start != null && end != null && !now.isBefore(start) && now.isBefore(end)
    val hasRoom = lottery.waitlistCapacity?.let { waitlistCount < it } ?: true
    val canJoin = participation == null || participation.status == ParticipationStatus.WITHDRAWN

    return if (!drawDone && registrationOpen && hasRoom && canJoin) {
        LotteryButtonState.JOIN
    } else {
        LotteryButtonState.UNAVAILABLE
    }
}

/** The one button. A disabled Material button is greyed out automatically. */
@Composable
fun LotteryActionButton(
    state: LotteryButtonState,
    isWorking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = state.enabled && !isWorking,
        modifier = modifier
    ) {
        if (isWorking) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(state.label)
        }
    }
}

/** Everything the campsite page needs to render and drive the lottery button. */
@Stable
class LotteryActionState(
    val buttonState: LotteryButtonState,
    val isWorking: Boolean,
    /** Empty when there is no error. */
    val error: String,
    /** Waitlist count including this user's join/leave from this session. */
    val waitlistCount: Int,
    val onClick: () -> Unit
)

/**
 * Loads the signed-in entrant's participation for [lottery] and handles join/leave taps.
 *
 * @param waitlistCount the waitlist size loaded with the lottery
 * @param entrantId the signed-in user's profile ID
 */
@Composable
fun rememberLotteryAction(
    lottery: Lottery?,
    waitlistCount: Int,
    entrantId: String,
    repository: LotteryParticipationRepository = remember { LotteryParticipationRepository() }
): LotteryActionState {
    val lotteryId = lottery?.id
    val scope = rememberCoroutineScope()

    var participation by remember(lotteryId, entrantId) { mutableStateOf<LotteryParticipation?>(null) }
    var isLoading by remember(lotteryId, entrantId) { mutableStateOf(lotteryId != null) }
    var isUpdating by remember(lotteryId, entrantId) { mutableStateOf(false) }
    var error by remember(lotteryId, entrantId) { mutableStateOf("") }
    var countAdjust by remember(lotteryId, entrantId) { mutableStateOf(0) }

    LaunchedEffect(lotteryId, entrantId) {
        if (lotteryId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        try {
            participation = repository.getMine(lotteryId, entrantId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Can not load participation", e)
            error = "Couldn't check your lottery status."
        }
        isLoading = false
    }

    val shownCount = waitlistCount + countAdjust
    val buttonState = lotteryButtonState(lottery, participation, shownCount)

    val onClick: () -> Unit = click@{
        if (lottery == null || isLoading || isUpdating) return@click
        val action = buttonState
        if (action == LotteryButtonState.UNAVAILABLE) return@click

        isUpdating = true
        error = ""
        scope.launch {
            try {
                if (action == LotteryButtonState.JOIN) {
                    repository.join(lottery.id, entrantId)
                    countAdjust += 1
                } else {
                    repository.leave(lottery.id, entrantId)
                    countAdjust -= 1
                }
                participation = repository.getMine(lottery.id, entrantId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lottery action failed", e)
                error = e.lotteryMessage("Something went wrong. Please try again.")
                // Re-sync in case the failure was caused by a change on another device.
                participation = try {
                    repository.getMine(lottery.id, entrantId)
                } catch (_: Exception) {
                    participation
                }
            } finally {
                isUpdating = false
            }
        }
    }

    return LotteryActionState(
        buttonState = buttonState,
        isWorking = isLoading || isUpdating,
        error = error,
        waitlistCount = shownCount,
        onClick = onClick
    )
}

/** Finds a [LotteryActionException] anywhere in the cause chain (transactions can wrap exceptions). */
private fun Throwable.lotteryMessage(fallback: String): String =
    generateSequence(this) { it.cause }
        .filterIsInstance<LotteryActionException>()
        .firstOrNull()
        ?.message
        ?: fallback