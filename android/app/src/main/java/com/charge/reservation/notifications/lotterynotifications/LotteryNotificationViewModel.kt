package com.charge.reservation.notifications.lotterynotifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.charge.reservation.model.ParticipationStatus
import kotlinx.coroutines.CancellationException
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

    private val _declineError = MutableStateFlow(false)
    /** True if the last decline attempt failed. */
    val declineError: StateFlow<Boolean> = _declineError.asStateFlow()

    fun load(lotteryId: String, entrantId: String) {
        viewModelScope.launch {
            _state.value = LotteryNotificationState.Loading
            _declineError.value = false
            _state.value = try {
                repo.load(lotteryId, entrantId)
                    ?.let { LotteryNotificationState.Loaded(it) }
                    ?: LotteryNotificationState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LotteryNotificationVM", "Failed to load lottery notification", e)
                LotteryNotificationState.Error
            }
        }
    }

    /**
     * Declines the invitation. On success the screen switches to the
     * "Invitation declined" message because the local status becomes DECLINED.
     */
    fun declineInvitation(lotteryId: String, entrantId: String, navController: NavController) {
        if (_declining.value) return
        viewModelScope.launch {
            _declining.value = true
            _declineError.value = false
            try {
                repo.declineInvitation(lotteryId, entrantId)
                _state.update { current ->
                    if (current is LotteryNotificationState.Loaded) {
                        LotteryNotificationState.Loaded(
                            current.data.copy(status = ParticipationStatus.DECLINED)
                        )
                    } else {
                        current
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LotteryNotificationVM", "Failed to decline invitation", e)
                _declineError.value = true
            } finally {
                _declining.value = false
            }
        }
    }
}