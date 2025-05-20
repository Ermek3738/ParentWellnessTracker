package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.data.generators.SimulatedDataGenerator
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.ui.health.HealthDataViewModel

@Composable
fun SimulatedDataControls(
    viewModel: HealthDataViewModel,
    metricType: String // Pass in the current metric type
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(0) }
    var selectedTimePeriod by remember { mutableStateOf(2) } // Default to MONTH
    var includeAnomalies by remember { mutableStateOf(true) }

    // Profiles for dropdown
    val profiles = listOf(
        "Healthy" to SimulatedDataGenerator.Companion.UserProfile.HEALTHY,
        "Active" to SimulatedDataGenerator.Companion.UserProfile.ACTIVE,
        "Sedentary" to SimulatedDataGenerator.Companion.UserProfile.SEDENTARY,
        "Pre-Hypertensive" to SimulatedDataGenerator.Companion.UserProfile.PRE_HYPERTENSIVE,
        "Hypertensive" to SimulatedDataGenerator.Companion.UserProfile.HYPERTENSIVE,
        "Pre-Diabetic" to SimulatedDataGenerator.Companion.UserProfile.PRE_DIABETIC,
        "Diabetic" to SimulatedDataGenerator.Companion.UserProfile.DIABETIC
    )

    // Time periods for dropdown
    val timePeriods = listOf(
        "Day" to SimulatedDataGenerator.Companion.TimePeriod.DAY,
        "Week" to SimulatedDataGenerator.Companion.TimePeriod.WEEK,
        "Month" to SimulatedDataGenerator.Companion.TimePeriod.MONTH,
        "3 Months" to SimulatedDataGenerator.Companion.TimePeriod.THREE_MONTHS,
        "6 Months" to SimulatedDataGenerator.Companion.TimePeriod.SIX_MONTHS,
        "Year" to SimulatedDataGenerator.Companion.TimePeriod.YEAR
    )

    Button(
        onClick = { showDialog = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = "Generate Test Data",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Generate Test Data")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Generate Simulated Data") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile selector
                    Text("Health Profile", style = MaterialTheme.typography.labelLarge)

                    Card {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profiles.forEachIndexed { index, (name, _) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedProfile = index }
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedProfile == index,
                                        onClick = { selectedProfile = index }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            }
                        }
                    }

                    // Time period selector
                    Text("Time Period", style = MaterialTheme.typography.labelLarge)

                    Card {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timePeriods.forEachIndexed { index, (name, _) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTimePeriod = index }
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedTimePeriod == index,
                                        onClick = { selectedTimePeriod = index }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            }
                        }
                    }

                    // Include anomalies checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = includeAnomalies,
                            onCheckedChange = { includeAnomalies = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Include anomalies (for testing alerts)")
                    }

                    // Information about the data generation
                    Text(
                        "This will generate realistic ${getMetricTypeLabel(metricType)} data with " +
                                "typical daily and weekly patterns. The data will replace any existing " +
                                "simulated data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateSimulatedData(
                            metricType = metricType,
                            profile = profiles[selectedProfile].second,
                            period = timePeriods[selectedTimePeriod].second,
                            includeAnomalies = includeAnomalies
                        )
                        showDialog = false
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function to get a human-readable label for metric types
private fun getMetricTypeLabel(metricType: String): String {
    return when(metricType) {
        HealthData.TYPE_HEART_RATE -> "heart rate"
        HealthData.TYPE_BLOOD_PRESSURE -> "blood pressure"
        HealthData.TYPE_BLOOD_SUGAR -> "blood sugar"
        HealthData.TYPE_STEPS -> "steps"
        else -> "health"
    }
}