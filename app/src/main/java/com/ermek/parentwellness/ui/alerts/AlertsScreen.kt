package com.ermek.parentwellness.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AlertSeverity {
    HIGH, MEDIUM, LOW, INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* Open alert settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Today's alerts
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        tint = Color.White
                    )
                },
                title = "High Heart Rate",
                message = "Your heart rate was above 100 BPM for more than 10 minutes while resting.",
                time = "2 hours ago",
                severity = AlertSeverity.HIGH
            )

            Spacer(modifier = Modifier.height(8.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Blood Pressure",
                        tint = Color.White
                    )
                },
                title = "Elevated Blood Pressure",
                message = "Your blood pressure reading was 135/90 mmHg at 9:30 AM.",
                time = "5 hours ago",
                severity = AlertSeverity.MEDIUM
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Yesterday's alerts
            Text(
                text = "Yesterday",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = "Blood Sugar",
                        tint = Color.White
                    )
                },
                title = "Low Blood Sugar",
                message = "Your blood sugar level dropped to 65 mg/dL at 11:45 PM.",
                time = "1 day ago",
                severity = AlertSeverity.HIGH
            )

            Spacer(modifier = Modifier.height(8.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = Color.White
                    )
                },
                title = "Activity Goal Achieved",
                message = "Congratulations! You've reached your daily step goal of 10,000 steps.",
                time = "1 day ago",
                severity = AlertSeverity.INFO
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Last Week alerts
            Text(
                text = "Last Week",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        tint = Color.White
                    )
                },
                title = "Irregular Heart Rhythm",
                message = "Several instances of irregular heart rhythm were detected. Please consult your doctor.",
                time = "5 days ago",
                severity = AlertSeverity.HIGH
            )

            Spacer(modifier = Modifier.height(8.dp))

            AlertItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Blood Pressure",
                        tint = Color.White
                    )
                },
                title = "Blood Pressure Trend",
                message = "Your systolic blood pressure has been gradually increasing over the past week.",
                time = "6 days ago",
                severity = AlertSeverity.MEDIUM
            )
        }
    }
}

@Composable
fun AlertItem(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    time: String,
    severity: AlertSeverity
) {
    val backgroundColor = when (severity) {
        AlertSeverity.HIGH -> Color.Red.copy(alpha = 0.1f)
        AlertSeverity.MEDIUM -> Color(0xFFFFA500).copy(alpha = 0.1f) // Orange
        AlertSeverity.LOW -> Color.Yellow.copy(alpha = 0.1f)
        AlertSeverity.INFO -> Color.Green.copy(alpha = 0.1f)
    }

    val iconBackgroundColor = when (severity) {
        AlertSeverity.HIGH -> Color.Red
        AlertSeverity.MEDIUM -> Color(0xFFFFA500) // Orange
        AlertSeverity.LOW -> Color.Yellow
        AlertSeverity.INFO -> Color.Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}