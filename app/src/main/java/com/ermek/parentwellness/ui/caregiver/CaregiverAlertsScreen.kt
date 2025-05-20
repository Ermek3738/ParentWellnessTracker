package com.ermek.parentwellness.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.notifications.Alert
import com.ermek.parentwellness.ui.alerts.AlertsViewModel
import com.ermek.parentwellness.ui.alerts.AlertsState
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverAlertsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlertsViewModel = viewModel()
) {
    val alertsState by viewModel.alertsState.collectAsState()

    // Load alerts when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadAlerts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Alerts") },
                actions = {
                    IconButton(onClick = { /* Open notification settings */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = alertsState) {
            is AlertsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
            is AlertsState.Success -> {
                if (state.alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No Alerts",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Parent health alerts will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Group alerts by date
                        val groupedAlerts = state.alerts.groupBy { alert ->
                            formatDateHeader(alert.timestamp)
                        }

                        groupedAlerts.forEach { (date, alertsForDate) ->
                            item {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            items(alertsForDate) { alert ->
                                CaregiverAlertItem(
                                    alert = alert,
                                    onAlertClick = { viewModel.markAlertAsRead(alert.id) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
            is AlertsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Error loading alerts",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.loadAlerts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRed
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverAlertItem(
    alert: Alert,
    onAlertClick: () -> Unit
) {
    val backgroundColor = if (!alert.read)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onAlertClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alert icon based on type
            val icon = when (alert.type) {
                "high_heart_rate", "low_heart_rate" -> Icons.Default.Favorite
                "high_blood_pressure", "low_blood_pressure" -> Icons.Default.Water
                "high_blood_sugar", "low_blood_sugar" -> Icons.Default.Opacity
                "emergency" -> Icons.Default.Warning
                else -> Icons.Default.Notifications
            }

            val iconColor = when (alert.type) {
                "high_heart_rate", "high_blood_pressure", "high_blood_sugar" -> MaterialTheme.colorScheme.error
                "low_heart_rate", "low_blood_pressure", "low_blood_sugar" -> MaterialTheme.colorScheme.primary
                "emergency" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${alert.userId}: ${getAlertTitle(alert)}",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = getAlertDescription(alert),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = formatTime(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!alert.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// Helper functions for formatting
private fun formatDateHeader(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    return when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR) < 7 -> {
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            sdf.format(Date(timestamp))
        }

        else -> {
            val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getAlertTitle(alert: Alert): String {
    return when (alert.type) {
        "high_heart_rate" -> "High Heart Rate Alert"
        "low_heart_rate" -> "Low Heart Rate Alert"
        "high_blood_pressure" -> "High Blood Pressure Alert"
        "low_blood_pressure" -> "Low Blood Pressure Alert"
        "high_blood_sugar" -> "High Blood Sugar Alert"
        "low_blood_sugar" -> "Low Blood Sugar Alert"
        "emergency" -> "Emergency Alert"
        else -> "Health Alert"
    }
}

private fun getAlertDescription(alert: Alert): String {
    return when (alert.type) {
        "high_heart_rate" -> "Heart rate was ${alert.value} BPM, which is above normal range."
        "low_heart_rate" -> "Heart rate was ${alert.value} BPM, which is below normal range."
        "high_blood_pressure" -> "Blood pressure was ${alert.value} mmHg, which is elevated."
        "low_blood_pressure" -> "Blood pressure was ${alert.value} mmHg, which is low."
        "high_blood_sugar" -> "Blood sugar was ${alert.value} mg/dL, which is above normal range."
        "low_blood_sugar" -> "Blood sugar was ${alert.value} mg/dL, which is below normal range."
        "emergency" -> "Emergency alert: ${alert.value}"
        else -> "${alert.metricName}: ${alert.value}"
    }
}