package com.charge.reservation.notifications.lotterynotifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charge.reservation.booking.BookingException
import com.charge.reservation.model.ParticipationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LotteryNotificationState {
    data object Loading : LotteryNotificationState
    data object NotFound : LotteryNotificationState
    data object Error : LotteryNotificationState
    data class Loaded(val data: LotteryNotificationData) : LotteryNotificationState
}

class LotteryNotificationViewModel(
    private val repo: LotteryNotificationRepository = LotteryNotificationRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<LotteryNotificationState>(LotteryNotificationState.Loading)
    val state: StateFlow<LotteryNotificationState> = _state.asStateFlow()

    private val _declining = MutableStateFlow(false)
    /** True while a decline request is in flight. */
    val declining: StateFlow<Boolean> = _declining.asStateFlow()

    private val _declineError = MutableStateFlow<String?>(null)
    /** Message for the last failed decline, or null. */
    val declineError: StateFlow<String?> = _declineError.asStateFlow()

    private var lotteryId: String? = null
    private var entrantId: String? = null
    private var loadJob: Job? = null

    /**
     * Called every time the screen comes to the front, including when the user
     * returns from the booking screen, so a new booking shows up straight away.
     * The first call shows a spinner; later calls refresh quietly.
     */
    fun onScreenResumed(lotteryId: String, entrantId: String) {
        val alreadyShowing = lotteryId == this.lotteryId && entrantId == this.entrantId &&
            _state.value is LotteryNotificationState.Loaded
        fetch(lotteryId, entrantId, showLoading = !alreadyShowing)
    }

    /** Reload after an error. */
    fun retry() {
        val l = lotteryId ?: return
        val e = entrantId ?: return
        fetch(l, e, showLoading = true)
    }

    private fun fetch(lotteryId: String, entrantId: String, showLoading: Boolean) {
        this.lotteryId = lotteryId
        this.entrantId = entrantId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) {
                _state.value = LotteryNotificationState.Loading
                _declineError.value = null
            }
            val result = try {
                repo.load(lotteryId, entrantId)
                    ?.let { LotteryNotificationState.Loaded(it) }
                    ?: LotteryNotificationState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load lottery notification", e)
                // A quiet refresh that fails keeps showing what we already have.
                if (!showLoading) return@launch
                LotteryNotificationState.Error
            }
            _state.value = result
        }
    }

    /**
     * Declines the invitation. On success the local status becomes DECLINED, so the
     * screen switches to "Invitation declined" and booking is no longer offered.
     */
    fun declineInvitation() {
        val l = lotteryId ?: return
        val e = entrantId ?: return
        if (_declining.value) return
        viewModelScope.launch {
            _declining.value = true
            _declineError.value = null
            try {
                repo.declineInvitation(l, e)
                _state.update { current ->
                    if (current is LotteryNotificationState.Loaded) {
                        LotteryNotificationState.Loaded(
                            current.data.copy(status = ParticipationStatus.DECLINED)
                        )
                    } else {
                        current
                    }
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to decline invitation", ex)
                _declineError.value = ex.userMessage()
                    ?: "Couldn't decline the invitation. Please try again."
                // The status may have changed elsewhere (e.g. already used); refresh it.
                fetch(l, e, showLoading = false)
            } finally {
                _declining.value = false
            }
        }
    }

    private companion object {
        const val TAG = "LotteryNotificationVM"
    }
}

/** Firestore may wrap exceptions thrown in a transaction; find ours in the cause chain. */
private fun Throwable.userMessage(): String? =
    generateSequence(this) { it.cause }
        .filterIsInstance<BookingException>()
        .firstOrNull()
        ?.message
