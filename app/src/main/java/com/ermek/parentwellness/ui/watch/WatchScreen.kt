@file:OptIn(ExperimentalMaterial3Api::class)

package com.ermek.parentwellness.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.data.model.WatchData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for displaying Samsung Galaxy Watch4 health data
 */
@Composable
fun WatchScreen(
    viewModel: WatchViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Toggle to show/hide diagnostic information
    var showDiagnostics by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galaxy Watch Health") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshWatchData(true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    // Add debug toggle
                    IconButton(onClick = { showDiagnostics = !showDiagnostics }) {
                        Icon(
                            if (showDiagnostics) Icons.Default.BugReport else Icons.Default.Build,
                            contentDescription = "Toggle Diagnostics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        WatchScreenContent(
            uiState = uiState,
            showDiagnostics = showDiagnostics,
            onRequestPermissions = { viewModel.launchSamsungHealth() },
            onRefresh = { viewModel.refreshWatchData() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun WatchScreenContent(
    uiState: WatchUiState,
    showDiagnostics: Boolean,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ConnectionStatusCard(
                connectionStatus = uiState.connectionStatus,
                permissionsGranted = uiState.permissionsGranted,
                onRequestPermissions = onRequestPermissions
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message - move to top for better visibility
            if (!uiState.isLoading && uiState.error != null) {
                ErrorMessage(error = uiState.error)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Display health data if connected
            if (uiState.permissionsGranted && !uiState.watchData.isEmpty()) {
                HealthDataSection(watchData = uiState.watchData)
            } else if (!uiState.isLoading && uiState.error == null && !uiState.permissionsGranted) {
                // Permissions required message
                NoPermissionsMessage(onRequestPermissions = onRequestPermissions)
            } else if (!uiState.isLoading && uiState.watchData.isEmpty() && uiState.error == null) {
                // No data available
                NoDataMessage(onRefresh = onRefresh)
            }

            // Diagnostic information (toggle with button)
            if (showDiagnostics && uiState.diagnosticInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                DiagnosticInfoCard(diagnosticInfo = uiState.diagnosticInfo)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionStatus: String,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Samsung Health Connection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusColor = when (connectionStatus) {
                    "Connected" -> Color.Green
                    "Connecting..." -> Color.Yellow
                    "Permissions Required" -> Color.Yellow
                    else -> Color.Red
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission status
            Text(
                text = if (permissionsGranted) {
                    "Health permissions: Granted"
                } else {
                    "Health permissions: Required"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Request permissions button if needed
            if (!permissionsGranted) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
private fun HealthDataSection(watchData: WatchData) {
    Column {
        Text(
            text = "Health Data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Last updated timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        Text(
            text = "Last updated: ${dateFormat.format(Date(watchData.timestamp))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Steps data
        HealthMetricCard(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            title = "Steps",
            value = "${watchData.steps.count}",
            subtitle = "steps today"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Heart rate data
        HealthMetricCard(
            icon = Icons.Default.Favorite,
            title = "Heart Rate",
            value = "${watchData.heartRate.average}",
            subtitle = "BPM (${watchData.heartRate.readings.size} readings)"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Blood pressure data
        if (watchData.bloodPressure.systolic > 0) {
            HealthMetricCard(
                icon = Icons.Default.HealthAndSafety,
                title = "Blood Pressure",
                value = "${watchData.bloodPressure.systolic.toInt()}/${watchData.bloodPressure.diastolic.toInt()}",
                subtitle = "mmHg (${watchData.bloodPressure.readings.size} readings)"
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Blood glucose data
        if (watchData.bloodGlucose.average > 0) {
            HealthMetricCard(
                icon = Icons.Default.Science,
                title = "Blood Glucose",
                value = "${watchData.bloodGlucose.average.toInt()}",
                subtitle = "mg/dL (${watchData.bloodGlucose.readings.size} readings)"
            )
        }
    }
}

@Composable
private fun HealthMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NoPermissionsMessage(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Permissions Required",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Health Data Permissions Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This app needs permission to access your Samsung Health data to display your health metrics from the Galaxy Watch4.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun NoDataMessage(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Watch,
            contentDescription = "No Data",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Health Data Available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure your Samsung Galaxy Watch4 is connected to your phone and synced with Samsung Health.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Refresh Data")
        }
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DiagnosticInfoCard(diagnosticInfo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Diagnostic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = diagnosticInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}