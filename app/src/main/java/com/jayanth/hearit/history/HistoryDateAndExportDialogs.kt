package com.jayanth.hearit.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

fun historyDateChipLabel(filter: HistoryFilter): String = when (filter.datePreset) {
    HistoryDatePreset.ALL -> "Date"
    HistoryDatePreset.TODAY -> "Today"
    HistoryDatePreset.YESTERDAY -> "Yesterday"
    HistoryDatePreset.LAST_7_DAYS -> "Last 7d"
    HistoryDatePreset.CUSTOM -> {
        val from = filter.dateFromMillis
        val to = filter.dateToMillis
        if (from != null && to != null) {
            val fmt = DateFormat.getDateInstance(DateFormat.SHORT)
            "${fmt.format(Date(from))}–${fmt.format(Date(to))}"
        } else {
            "Custom Date"
        }
    }
}

@Composable
fun HistoryDatePresetDialog(
    selected: HistoryDatePreset,
    onSelectPreset: (HistoryDatePreset) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        HistoryDatePreset.ALL to "All Time",
        HistoryDatePreset.TODAY to "Today",
        HistoryDatePreset.YESTERDAY to "Yesterday",
        HistoryDatePreset.LAST_7_DAYS to "Last 7 Days",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Date Range", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (preset, label) ->
                    Surface(
                        onClick = { onSelectPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected == preset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == preset, onClick = { onSelectPreset(preset) })
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontWeight = if (selected == preset) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onOpenCustomPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (selected == HistoryDatePreset.CUSTOM) "Change Custom Date Range…" else "Pick Custom Date Range…")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCustomDateRangePickerDialog(
    initialStartMillis: Long?,
    initialEndMillis: Long?,
    onConfirm: (startUtcMillis: Long, endUtcMillis: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = pickerState.selectedStartDateMillis
            val end = pickerState.selectedEndDateMillis ?: start
            TextButton(
                onClick = { if (start != null && end != null) onConfirm(start, end) },
                enabled = start != null,
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .padding(top = 8.dp),
        )
    }
}

@Composable
fun HistoryExportFormatDialog(
    entryCountShown: Int,
    onExportCsv: () -> Unit,
    onExportTxt: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export History", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Exports all records matching your current search and date/app/status filters ($entryCountShown+ entries loaded).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Export as CSV (.csv)")
                }
                OutlinedButton(
                    onClick = onExportTxt,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Export as Plain Text (.txt)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
