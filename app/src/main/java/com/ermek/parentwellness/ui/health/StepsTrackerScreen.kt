package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.components.TimeRangeSelector
import com.ermek.parentwellness.ui.theme.PrimaryRed
import com.ermek.parentwellness.ui.components.SimulatedDataControls
import com.ermek.parentwellness.data.model.HealthData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsTrackerScreen(
    onBack: () -> Unit,
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    val stepsData by healthDataViewModel.stepsData.collectAsState()
    val isLoading by healthDataViewModel.isLoading.collectAsState()
    val error by healthDataViewModel.error.collectAsState()

    var showAddStepsDialog by remember { mutableStateOf(false) }

    // Calculate daily goal and progress
    val dailyGoal = 10000
    val todaySteps = stepsData.filter {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.timeInMillis = it.timestamp

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }.maxByOrNull { it.timestamp }?.steps ?: 0

    val progress = (todaySteps.toFloat() / dailyGoal * 100).toInt().coerceIn(0, 100)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps Tracker") },
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
                onClick = { showAddStepsDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Steps",
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
                // Add SimulatedDataControls at the top
                SimulatedDataControls(
                    viewModel = healthDataViewModel,
                    metricType = HealthData.TYPE_STEPS
                )

                // Today's progress
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today's Steps",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "$todaySteps",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed
                        )

                        Text(
                            text = "of $dailyGoal goal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = PrimaryRed,
                            trackColor = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$progress% of daily goal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Average stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val avgSteps = stepsData.takeIf { it.isNotEmpty() }?.let {
                        it.sumOf { data -> data.steps } / it.size
                    } ?: 0

                    val maxSteps = stepsData.takeIf { it.isNotEmpty() }?.maxOfOrNull { it.steps } ?: 0
                    val totalSteps = stepsData.takeIf { it.isNotEmpty() }?.sumOf { it.steps } ?: 0

                    StatisticItem(value = avgSteps.toString(), label = "Daily Avg")
                    StatisticItem(value = maxSteps.toString(), label = "Best Day")
                    StatisticItem(value = totalSteps.toString(), label = "Total")
                }

                // Time range selector
                TimeRangeSelector(
                    onRangeSelected = { timeRange ->
                        healthDataViewModel.loadDataByTimeRange(timeRange)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chart placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PrimaryRed)
                    } else if (error != null) {
                        Text("Error: $error")
                    } else if (stepsData.isEmpty()) {
                        Text("No steps data available")
                    } else {
                        Text("Steps Chart")
                        // We'll implement the actual chart visualization later
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Steps history section
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (stepsData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No steps data available")
                    }
                } else {
                    // Display list of steps readings
                    stepsData.forEach { data ->
                        StepsHistoryItem(data)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            // Loading indicator
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

    // Add Steps Dialog
    if (showAddStepsDialog) {
        AddStepsDialog(
            onDismiss = { showAddStepsDialog = false },
            onSave = { steps ->
                healthDataViewModel.addStepsReading(steps)
                showAddStepsDialog = false
            }
        )
    }
}

@Composable
fun StepsHistoryItem(data: com.ermek.parentwellness.data.repository.StepsData) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember(data.timestamp) { dateFormat.format(Date(data.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Steps icon
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = "Steps",
            tint = PrimaryRed,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            // Calculate progress
            val progress = (data.steps.toFloat() / 10000 * 100).toInt().coerceIn(0, 100)

            Text(
                text = "$progress% of daily goal",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Text(
            text = "${data.steps}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryRed
        )
    }
}

@Composable
fun AddStepsDialog(
    onDismiss: () -> Unit,
    onSave: (steps: Int) -> Unit
) {
    var stepsText by remember { mutableStateOf("") }
    var stepsError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Steps") },
        text = {
            Column {
                // Steps input
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = {
                        stepsText = it.filter { char -> char.isDigit() }
                        stepsError = false
                    },
                    label = { Text("Steps Count") },
                    isError = stepsError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (stepsError) {
                    Text(
                        text = "Please enter a valid steps value",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = stepsText.toIntOrNull()

                    // Validate input
                    stepsError = steps == null || steps < 0 || steps > 100000

                    if (!stepsError) {
                        onSave(steps!!)
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