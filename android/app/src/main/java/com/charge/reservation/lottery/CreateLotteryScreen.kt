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

@Composable
fun CreateLotteryScreen(
    campsite: Campsite
) {
    var winnerCountInput by rememberSaveable(campsite.id) {
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
            onDateSelected = { registrationEndDateMillis = it }
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

        OutlinedTextField(
            value = winnerCountInput,
            onValueChange = { winnerCountInput = it },
            label = { Text("Number of winners") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
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