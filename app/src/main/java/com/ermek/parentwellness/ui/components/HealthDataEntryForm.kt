package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

enum class MetricType {
    HEART_RATE,
    BLOOD_PRESSURE,
    BLOOD_SUGAR,
    STEPS
}

data class HealthDataEntry(
    val metricType: MetricType,
    val primaryValue: String,
    val secondaryValue: String? = null, // For blood pressure (diastolic)
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val situation: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDataEntryForm(
    metricType: MetricType,
    onSubmit: (HealthDataEntry) -> Unit,
    onCancel: () -> Unit
) {
    var primaryValue by remember { mutableStateOf("") }
    var secondaryValue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedSituation by remember { mutableStateOf("") }

    val situations = listOf(
        "Resting",
        "After Exercise",
        "Before Meal",
        "After Meal",
        "Before Sleep",
        "Upon Waking",
        "Sitting",
        "Standing",
        "Walking",
        "At Work",
        "Relaxing"
    )

    // Generate current date and time in readable format
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val currentTime = dateFormat.format(Date())

    // Setup metric-specific properties
    val (title, icon, primaryValueLabel, secondaryValueLabel, primaryUnit, secondaryUnit) = when (metricType) {
        MetricType.HEART_RATE -> Quintuple(
            "Heart Rate",
            Icons.Default.Favorite,
            "BPM",
            null,
            "bpm",
            null
        )
        MetricType.BLOOD_PRESSURE -> Quintuple(
            "Blood Pressure",
            Icons.Default.Water,
            "Systolic",
            "Diastolic",
            "mmHg",
            "mmHg"
        )
        MetricType.BLOOD_SUGAR -> Quintuple(
            "Blood Sugar",
            Icons.Default.Opacity,
            "Value",
            null,
            "mg/dL",
            null
        )
        MetricType.STEPS -> Quintuple(
            "Steps",
            Icons.AutoMirrored.Filled.DirectionsWalk,
            "Count",
            null,
            "steps",
            null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add $title",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Current time display
        Text(
            text = "Time: $currentTime",
            style = MaterialTheme.typography.bodyMedium
        )

        // Primary value input
        OutlinedTextField(
            value = primaryValue,
            onValueChange = {
                // Only allow numeric input
                if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                    primaryValue = it
                }
            },
            label = { Text(primaryValueLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Text(
                    text = primaryUnit,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        )

        // Secondary value input (for blood pressure)
        if (secondaryValueLabel != null) {
            OutlinedTextField(
                value = secondaryValue,
                onValueChange = {
                    // Only allow numeric input
                    if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                        secondaryValue = it
                    }
                },
                label = { Text(secondaryValueLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Text(
                        text = secondaryUnit ?: "",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }

        // Situation dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedSituation,
                onValueChange = {},
                readOnly = true,
                label = { Text("Situation") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // Use the parameter-less version which is not deprecated
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                situations.forEach { situation ->
                    DropdownMenuItem(
                        text = { Text(situation) },
                        onClick = {
                            selectedSituation = situation
                            expanded = false
                        }
                    )
                }
            }
        }

        // Notes field
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val entry = HealthDataEntry(
                        metricType = metricType,
                        primaryValue = primaryValue,
                        secondaryValue = if (secondaryValueLabel != null) secondaryValue else null,
                        timestamp = System.currentTimeMillis(),
                        notes = notes,
                        situation = selectedSituation
                    )
                    onSubmit(entry)
                },
                enabled = primaryValue.isNotEmpty() &&
                        (secondaryValueLabel == null || secondaryValue.isNotEmpty())
            ) {
                Text("Save")
            }
        }
    }
}

/**
 * Helper class for multiple return values
 */
data class Quintuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)