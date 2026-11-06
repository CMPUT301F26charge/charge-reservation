package com.charge.reservation.lottery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Campsite
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import java.time.LocalTime
import java.time.ZoneId
import java.time.Duration
import androidx.compose.material3.Button
import com.charge.reservation.model.Lottery

/**
 * Collects lottery settings for one registered campsite.
 *
 * Publishing requires all required fields, valid date/time ordering,
 * and a positive whole-number winner count. Date/time selections use
 * the displayed time zone. Form inputs remain when a save fails.
 *
 * @param campsite the registered campsite associated with this lottery.
 * @param lotteryId stable document ID retained for this creation form.
 * @param isSaving disables publishing while a save is pending.
 * @param errorMessage save failure message, or empty when none exists.
 * @param onCreateLottery receives the validated configuration when
 * Publish is pressed; the caller performs saving and handles its result.
 */
@Composable
fun CreateLotteryScreen(
    campsite: Campsite,
    lotteryId: String,
    isSaving: Boolean,
    errorMessage: String,
    onCreateLottery: (Lottery) -> Unit
) {
    var winnerCountInput by rememberSaveable(campsite.id) {
        mutableStateOf("")
    }

    var waitlistCapacityInput by rememberSaveable(campsite.id) {
        mutableStateOf("")
    }

    var registrationStartDateMillis by rememberSaveable(campsite.id) {
        mutableStateOf<Long?>(null)
    }

    var registrationEndDateMillis by rememberSaveable(campsite.id) {
        mutableStateOf<Long?>(null)
    }

    var registrationStartMinutes by rememberSaveable(campsite.id) {
        mutableStateOf<Int?>(null)
    }

    var registrationEndMinutes by rememberSaveable(campsite.id) {
        mutableStateOf<Int?>(null)
    }

    val timeZoneId = rememberSaveable(campsite.id) {
        ZoneId.systemDefault().id
    }

    var bookingStartDateMillis by rememberSaveable(campsite.id) {
        mutableStateOf<Long?>(null)
    }

    var bookingEndDateMillis by rememberSaveable(campsite.id) {
        mutableStateOf<Long?>(null)
    }

    var bookingDeadlineDateMillis by rememberSaveable(campsite.id) {
        mutableStateOf<Long?>(null)
    }

    var bookingDeadlineMinutes by rememberSaveable(campsite.id) {
        mutableStateOf<Int?>(null)
    }

    val oneDayMillis = Duration.ofDays(1).toMillis()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Create lottery",
            fontSize = 28.sp
        )

        Text("Campsite: ${campsite.name}")

        Text("Time zone: $timeZoneId")

        LotteryDateField(
            label = "Registration opens",
            selectedDateMillis = registrationStartDateMillis,
            onDateSelected = { newStart ->
                registrationStartDateMillis = newStart

                val endDate = registrationEndDateMillis
                if (endDate != null && endDate < newStart) {
                    registrationEndDateMillis = null
                }
            }
        )

        LotteryTimeField(
            label = "Opening time",
            selectedMinutes = registrationStartMinutes,
            onTimeSelected = { registrationStartMinutes = it }
        )

        LotteryDateField(
            label = "Registration closes",
            selectedDateMillis = registrationEndDateMillis,
            minDateMillis = registrationStartDateMillis,
            onDateSelected = { newEnd ->
                registrationEndDateMillis = newEnd

                val bookingStart = bookingStartDateMillis
                if (bookingStart != null && bookingStart <= newEnd) {
                    bookingStartDateMillis = null
                    bookingEndDateMillis = null
                }
            }
        )

        LotteryTimeField(
            label = "Closing time",
            selectedMinutes = registrationEndMinutes,
            onTimeSelected = { registrationEndMinutes = it }
        )

        val openingMinutes = registrationStartMinutes
        val closingMinutes = registrationEndMinutes

        if (
            registrationStartDateMillis != null &&
            registrationStartDateMillis == registrationEndDateMillis &&
            openingMinutes != null &&
            closingMinutes != null &&
            closingMinutes <= openingMinutes
        ) {
            Text(
                text = "Closing time must be after opening time.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Booking deadline",
            fontSize = 20.sp
        )

        Text("Winners must confirm their reservation by this deadline.")

        LotteryDateField(
            label = "Deadline date",
            selectedDateMillis = bookingDeadlineDateMillis,
            minDateMillis = registrationEndDateMillis,
            onDateSelected = { bookingDeadlineDateMillis = it }
        )

        LotteryTimeField(
            label = "Deadline time",
            selectedMinutes = bookingDeadlineMinutes,
            onTimeSelected = { bookingDeadlineMinutes = it }
        )

        val registrationClosesAt = lotteryDateTimeInstant(
            registrationEndDateMillis,
            registrationEndMinutes,
            timeZoneId
        )

        val deadlineAt = lotteryDateTimeInstant(
            bookingDeadlineDateMillis,
            bookingDeadlineMinutes,
            timeZoneId
        )

        val bookingBeginsAt = lotteryDateTimeInstant(
            bookingStartDateMillis,
            0,
            timeZoneId
        )

        val deadlineError = when {
            deadlineAt != null && registrationClosesAt != null &&
                    !deadlineAt.isAfter(registrationClosesAt) ->
                "Deadline must be after registration closes."

            deadlineAt != null && bookingBeginsAt != null &&
                    !deadlineAt.isBefore(bookingBeginsAt) ->
                "Deadline must be before the booking period begins."

            else -> null
        }

        if (deadlineError != null) {
            Text(
                text = deadlineError,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Booking period",
            fontSize = 20.sp
        )

        LotteryDateField(
            label = "Earliest check-in",
            selectedDateMillis = bookingStartDateMillis,
            minDateMillis = registrationEndDateMillis?.let {
                it + oneDayMillis
            },
            onDateSelected = { newStart ->
                bookingStartDateMillis = newStart

                val endDate = bookingEndDateMillis
                if (endDate != null && endDate <= newStart) {
                    bookingEndDateMillis = null
                }
            }
        )

        LotteryDateField(
            label = "Latest checkout",
            selectedDateMillis = bookingEndDateMillis,
            minDateMillis = bookingStartDateMillis?.let {
                it + oneDayMillis
            },
            onDateSelected = { bookingEndDateMillis = it }
        )

        val winnerCount = winnerCountInput.toIntOrNull()
        val invalidWinnerCount = winnerCountInput.isNotEmpty() &&
                (winnerCount == null || winnerCount <= 0)

        OutlinedTextField(
            value = winnerCountInput,
            onValueChange = { winnerCountInput = it },
            label = { Text("Number of winners") },
            isError = invalidWinnerCount,
            supportingText = {
                if (invalidWinnerCount) {
                    Text("Enter a whole number greater than zero.")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // US 02.04.07: Optional waitlist capacity.
        val capacity = if (waitlistCapacityInput.isBlank()) {
            null
        } else {
            waitlistCapacityInput.toIntOrNull()
        }

        val invalidCapacity =
            waitlistCapacityInput.isNotBlank() &&
                    (capacity == null || capacity <= 0)

        OutlinedTextField(
            value = waitlistCapacityInput,
            onValueChange = { waitlistCapacityInput = it },
            label = { Text("Waitlist capacity (optional)") },
            placeholder = { Text("Leave blank for unlimited") },
            supportingText = {
                Text(
                    if (invalidCapacity) {
                        "Enter a positive whole number."
                    } else {
                        "Blank means unlimited entrants."
                    }
                )
            },
            isError = invalidCapacity,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val registrationOpensAt = lotteryDateTimeInstant(
            registrationStartDateMillis,
            registrationStartMinutes,
            timeZoneId

        )

        val bookingStart = bookingStartDateMillis
        val bookingEnd = bookingEndDateMillis

        val formError = when {
            registrationOpensAt == null ->
                "Select the registration opening date and time."

            registrationClosesAt == null ->
                "Select the registration closing date and time."

            !registrationClosesAt.isAfter(registrationOpensAt) ->
                "Registration must close after it opens."

            bookingStart == null ->
                "Select the earliest check-in date."

            bookingEnd == null ->
                "Select the latest checkout date."

            bookingEnd <= bookingStart ->
                "Checkout must be after check-in."

            deadlineAt == null ->
                "Select the booking deadline date and time."

            deadlineError != null ->
                deadlineError

            winnerCount == null || winnerCount <= 0 ->
                "Enter a whole number of winners greater than zero."

            invalidCapacity ->
                "Waitlist capacity must be a positive whole number."

            else -> null
        }

        Text(
            text = formError ?: "All required details are complete."
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = formError == null && !isSaving,
            onClick = {
                if (formError == null && !isSaving) {
                    onCreateLottery(
                        Lottery(
                            id = lotteryId,
                            ownerId = campsite.ownerId,
                            campsiteIds = listOf(campsite.id),
                            registrationStartAt = registrationOpensAt,
                            registrationEndAt = registrationClosesAt,
                            bookingDeadline = deadlineAt,
                            bookingStartDate = Instant.ofEpochMilli(
                                requireNotNull(bookingStart)
                            ).atZone(ZoneOffset.UTC).toLocalDate(),
                            bookingEndDate = Instant.ofEpochMilli(
                                requireNotNull(bookingEnd)
                            ).atZone(ZoneOffset.UTC).toLocalDate(),
                            timeZone = ZoneId.of(timeZoneId),
                            winnerCount = winnerCount,
                            waitlistCapacity = capacity
                        )
                    )
                }
            }
        ) {
            Text(
                if (isSaving) "Publishing..." else "Publish lottery"
            )
        }
    }
}

private fun lotteryDateTimeInstant(
    dateMillis: Long?,
    minutes: Int?,
    timeZoneId: String
): Instant? {
    if (dateMillis == null || minutes == null) {
        return null
    }

    val date = Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()

    return date.atTime(minutes / 60, minutes % 60)
        .atZone(ZoneId.of(timeZoneId))
        .toInstant()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LotteryDateField(
    label: String,
    selectedDateMillis: Long?,
    minDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit
) {
    var showPicker by rememberSaveable {
        mutableStateOf(false)
    }

    val dateText = selectedDateMillis?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()
    } ?: "Select date"

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: $dateText")
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis?.takeIf {
                minDateMillis == null || it >= minDateMillis
            },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return minDateMillis == null ||
                            utcTimeMillis >= minDateMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        val selected = pickerState.selectedDateMillis

                        if (selected != null) {
                            onDateSelected(selected)
                            showPicker = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LotteryTimeField(
    label: String,
    selectedMinutes: Int?,
    onTimeSelected: (Int) -> Unit
) {
    var showPicker by rememberSaveable {
        mutableStateOf(false)
    }

    val timeText = selectedMinutes?.let {
        LocalTime.of(it / 60, it % 60).toString()
    } ?: "Select time"

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: $timeText")
    }

    if (showPicker) {
        val initialMinutes = selectedMinutes ?: 9 * 60
        val pickerState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(
                            pickerState.hour * 60 + pickerState.minute
                        )
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}