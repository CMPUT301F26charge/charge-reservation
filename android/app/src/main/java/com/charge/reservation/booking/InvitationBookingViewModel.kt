package com.charge.reservation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.charge.reservation.model.Campsite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BookingConfirmation(
    val reservationId: String,
    val campsiteName: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int
)

data class BookingUiState(
    val loading: Boolean = true,
    /** Set when the invitation itself can't be used (expired, not signed in, ...). */
    val fatalError: String? = null,
    val details: InvitationDetails? = null,

    val selectedCampsiteId: String? = null,
    val nights: List<NightOption> = emptyList(),
    val nightsLoading: Boolean = false,
    val guestCount: Int = 1,

    /** First selected night. */
    val rangeStart: LocalDate? = null,
    /** Last selected night, or null when only [rangeStart] is selected (a one-night stay). */
    val rangeEnd: LocalDate? = null,

    val submitting: Boolean = false,
    /** One-shot message for a snackbar. */
    val message: String? = null,
    val confirmation: BookingConfirmation? = null
) {
    val selectedCampsite: Campsite?
        get() = details?.campsites?.firstOrNull { it.id == selectedCampsiteId }

    val maxGuests: Int
        get() = (selectedCampsite?.maxOccupancy ?: DEFAULT_MAX_GUESTS).coerceAtLeast(1)

    val selectedNights: List<NightOption>
        get() {
            val start = rangeStart ?: return emptyList()
            val end = rangeEnd ?: start
            return nights.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }

    val checkIn: LocalDate? get() = selectedNights.firstOrNull()?.date
    val checkOut: LocalDate? get() = selectedNights.lastOrNull()?.date?.plusDays(1)

    val estimatedTotal: Double?
        get() = selectedCampsite?.pricePerNight?.let { it * selectedNights.size }

    val showForm: Boolean
        get() = !loading && fatalError == null && details != null && confirmation == null

    fun isBookable(night: NightOption) = night.remaining >= guestCount

    companion object {
        const val DEFAULT_MAX_GUESTS = 20
    }
}

class InvitationBookingViewModel(
    private val lotteryId: String,
    private val repository: InvitationBookingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = BookingUiState(loading = true)
            try {
                val details = repository.loadInvitation(lotteryId)
                _state.update { it.copy(loading = false, details = details) }
                selectCampsite(details.campsites.first().id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, fatalError = e.userMessage("Couldn't load your invitation. Check your connection and try again."))
                }
            }
        }
    }

    fun selectCampsite(campsiteId: String) {
        val details = _state.value.details ?: return
        val campsite = details.campsites.firstOrNull { it.id == campsiteId } ?: return
        _state.update {
            it.copy(
                selectedCampsiteId = campsiteId,
                guestCount = it.guestCount.coerceIn(1, (campsite.maxOccupancy ?: BookingUiState.DEFAULT_MAX_GUESTS).coerceAtLeast(1)),
                rangeStart = null,
                rangeEnd = null,
                nights = emptyList()
            )
        }
        refreshNights()
    }

    fun setGuestCount(count: Int) {
        _state.update {
            // Changing the party size can make some nights unbookable, so clear the selection.
            it.copy(guestCount = count.coerceIn(1, it.maxGuests), rangeStart = null, rangeEnd = null)
        }
    }

    /**
     * First tap picks the first night (a one-night stay). A later tap on a night after it
     * extends the stay up to that night, provided every night in between is bookable.
     * Tapping before the first night, or after a range is set, starts a new selection.
     */
    fun onNightTapped(date: LocalDate) {
        val s = _state.value
        val tapped = s.nights.firstOrNull { it.date == date } ?: return
        if (!s.isBookable(tapped)) return

        val start = s.rangeStart
        if (start == null || s.rangeEnd != null || date.isBefore(start)) {
            _state.update { it.copy(rangeStart = date, rangeEnd = null) }
            return
        }
        if (date == start) return

        val allBookable = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(date) }
            .all { d -> s.nights.any { it.date == d && s.isBookable(it) } }

        if (allBookable) {
            _state.update { it.copy(rangeEnd = date) }
        } else {
            _state.update {
                it.copy(
                    rangeStart = date,
                    rangeEnd = null,
                    message = "Some nights in that range are unavailable. Pick nights that run back to back."
                )
            }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(rangeStart = null, rangeEnd = null) }
    }

    fun book() {
        val s = _state.value
        val details = s.details ?: return
        val campsite = s.selectedCampsite ?: return
        val nights = s.selectedNights
        val checkIn = s.checkIn ?: return
        val checkOut = s.checkOut ?: return
        if (s.submitting || nights.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            try {
                val reservationId = repository.bookStay(details, campsite, nights, s.guestCount)
                _state.update {
                    it.copy(
                        submitting = false,
                        confirmation = BookingConfirmation(
                            reservationId, campsite.name, checkIn, checkOut, s.guestCount
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        submitting = false,
                        rangeStart = null,
                        rangeEnd = null,
                        message = e.userMessage("Couldn't complete your booking. Try again.")
                    )
                }
                // Availability may have changed under us; show the latest numbers.
                refreshNights()
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun refreshNights() {
        val s = _state.value
        val details = s.details ?: return
        val campsiteId = s.selectedCampsiteId ?: return
        viewModelScope.launch {
            _state.update { it.copy(nightsLoading = true) }
            try {
                val nights = repository.loadNights(campsiteId, details.lottery)
                _state.update {
                    // Ignore the result if the user switched campsites while loading.
                    if (it.selectedCampsiteId == campsiteId) it.copy(nights = nights, nightsLoading = false) else it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(nightsLoading = false, message = "Couldn't load available nights. Try again.")
                }
            }
        }
    }

    companion object {
        fun factory(lotteryId: String) = viewModelFactory {
            initializer { InvitationBookingViewModel(lotteryId, InvitationBookingRepository()) }
        }
    }
}

/** Finds a [BookingException] anywhere in the cause chain (transactions can wrap exceptions). */
private fun Throwable.userMessage(fallback: String): String =
    generateSequence(this) { it.cause }
        .filterIsInstance<BookingException>()
        .firstOrNull()
        ?.message
        ?: fallback