package com.charge.reservation.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charge.reservation.model.Notification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(
    private val repo: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val userId = MutableStateFlow<String?>(null)

    /** Set the signed-in user's id (UserProfile.id, the Firebase Auth UID). */
    fun setUser(uid: String?) {
        userId.value = uid
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<Notification>> = userId
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else notificationsIfEnabled(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Watches the user's notificationEnabled setting. While it is false, shows
     * nothing and doesn't listen to the notifications collection at all; when it
     * flips back to true, the notification listener starts again.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun notificationsIfEnabled(uid: String): Flow<List<Notification>> =
        repo.notificationsEnabledFor(uid)
            .catch { e ->
                // If the user doc can't be read, fall back to showing notifications.
                Log.e("NotificationsVM", "Failed to read notificationEnabled", e)
                emit(true)
            }
            .flatMapLatest { enabled ->
                if (!enabled) {
                    flowOf(emptyList())
                } else {
                    repo.notificationsFor(uid).catch { e ->
                        // Missing index or permission problems land here instead of crashing.
                        Log.e("NotificationsVM", "Failed to load notifications", e)
                        emit(emptyList())
                    }
                }
            }
}
