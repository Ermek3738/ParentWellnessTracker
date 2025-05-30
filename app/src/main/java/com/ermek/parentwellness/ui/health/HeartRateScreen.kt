package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.components.HeartRateChart
import com.ermek.parentwellness.ui.theme.PrimaryRed
import com.ermek.parentwellness.ui.components.SimulatedDataControls
import com.ermek.parentwellness.ui.components.TimeRangeSelector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    onBack: () -> Unit,
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    val heartRateData by healthDataViewModel.heartRateData.collectAsState()
    val isLoading by healthDataViewModel.isLoading.collectAsState()
    val error by healthDataViewModel.error.collectAsState()
    var showAddHeartRateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heart Rate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHeartRateDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Heart Rate",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SimulatedDataControls(
                    viewModel = healthDataViewModel,
                    metricType = "HEART_RATE"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val avgHeartRate = heartRateData.takeIf { it.isNotEmpty() }?.let {
                        it.sumOf { data -> data.heartRate } / it.size
                    } ?: 0

                    val maxHeartRate = heartRateData.takeIf { it.isNotEmpty() }?.maxOfOrNull { it.heartRate } ?: 0
                    val minHeartRate = heartRateData.takeIf { it.isNotEmpty() }?.minOfOrNull { it.heartRate } ?: 0

                    StatisticItem(value = avgHeartRate.toString(), label = "Average")
                    StatisticItem(value = maxHeartRate.toString(), label = "Maximum")
                    StatisticItem(value = minHeartRate.toString(), label = "Minimum")
                }

                TimeRangeSelector(
                    onRangeSelected = { timeRange ->
                        healthDataViewModel.loadDataByTimeRange(TimeRange.fromString(timeRange.toString()))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HeartRateChart(
                    data = heartRateData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (heartRateData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No heart rate data available")
                    }
                } else {
                    heartRateData.forEach { data ->
                        HeartRateHistoryItem(data)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
        }
    }

    if (showAddHeartRateDialog) {
        AddHeartRateDialog(
            onDismiss = { showAddHeartRateDialog = false },
            onSave = { heartRate, isResting ->
                healthDataViewModel.addHeartRateReading(heartRate, isResting)
                showAddHeartRateDialog = false
            }
        )
    }
}

@Composable
fun StatisticItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun HeartRateHistoryItem(data: com.ermek.parentwellness.data.repository.HeartRateData) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(data.timestamp) { dateFormat.format(Date(data.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimaryRed.copy(alpha = 0.1f), shape = RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = data.heartRate.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryRed,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (data.isResting) "Resting Heart Rate" else "Heart Rate",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Text(
            text = "${data.heartRate} BPM",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryRed
        )
    }
}

@Composable
fun AddHeartRateDialog(
    onDismiss: () -> Unit,
    onSave: (heartRate: Int, isResting: Boolean) -> Unit
) {
    var heartRateText by remember { mutableStateOf("") }
    var isResting by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Heart Rate") },
        text = {
            Column {
                OutlinedTextField(
                    value = heartRateText,
                    onValueChange = {
                        heartRateText = it.filter { char -> char.isDigit() }
                        isError = false
                    },
                    label = { Text("Heart Rate (BPM)") },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Please enter a valid heart rate",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp))

                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isResting,
                        onCheckedChange = { isResting = it }
                    )
                    Text(
                        text = "Resting heart rate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val heartRate = heartRateText.toIntOrNull()
                    if (heartRate != null && heartRate > 0 && heartRate < 300) {
                        onSave(heartRate, isResting)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}