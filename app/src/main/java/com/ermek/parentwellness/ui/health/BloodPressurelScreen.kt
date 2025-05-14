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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.components.TimeRangeSelector
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureScreen(
    onBack: () -> Unit,
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    val bloodPressureData by healthDataViewModel.bloodPressureData.collectAsState()
    val isLoading by healthDataViewModel.isLoading.collectAsState()
    val error by healthDataViewModel.error.collectAsState()

    var showAddBloodPressureDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Pressure") },
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
                onClick = { showAddBloodPressureDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Blood Pressure",
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
                // Average stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val avgSystolic = bloodPressureData.takeIf { it.isNotEmpty() }?.let {
                        it.sumOf { data -> data.systolic } / it.size
                    } ?: 0

                    val avgDiastolic = bloodPressureData.takeIf { it.isNotEmpty() }?.let {
                        it.sumOf { data -> data.diastolic } / it.size
                    } ?: 0

                    val avgPulse = bloodPressureData.takeIf { it.isNotEmpty() }?.mapNotNull { it.pulse }?.let {
                        if (it.isNotEmpty()) it.sum() / it.size else null
                    }

                    StatisticItem(value = avgSystolic.toString(), label = "Systolic")
                    StatisticItem(value = avgDiastolic.toString(), label = "Diastolic")
                    avgPulse?.let { StatisticItem(value = it.toString(), label = "Pulse") }
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
                    } else if (bloodPressureData.isEmpty()) {
                        Text("No blood pressure data available")
                    } else {
                        Text("Blood Pressure Chart")
                        // We'll implement the actual chart visualization later
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Blood pressure history section
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (bloodPressureData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No blood pressure data available")
                    }
                } else {
                    // Display list of blood pressure readings
                    bloodPressureData.forEach { data ->
                        BloodPressureHistoryItem(data)
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

    // Add Blood Pressure Dialog
    if (showAddBloodPressureDialog) {
        AddBloodPressureDialog(
            onDismiss = { showAddBloodPressureDialog = false },
            onSave = { systolic, diastolic, pulse, situation ->
                healthDataViewModel.addBloodPressureReading(systolic, diastolic, pulse, situation)
                showAddBloodPressureDialog = false
            }
        )
    }
}

@Composable
fun BloodPressureHistoryItem(data: com.ermek.parentwellness.data.repository.BloodPressureData) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(data.timestamp) { dateFormat.format(Date(data.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blood pressure values stacked
        Column(
            modifier = Modifier
                .width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.systolic.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(40.dp)
                    .padding(vertical = 2.dp),
                color = Color.Gray
            )

            Text(
                text = data.diastolic.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Blue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.situation ?: "Blood Pressure",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            data.pulse?.let {
                Text(
                    text = "Pulse: $it BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }

        Text(
            text = "${data.systolic}/${data.diastolic}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}

@Composable
fun AddBloodPressureDialog(
    onDismiss: () -> Unit,
    onSave: (systolic: Int, diastolic: Int, pulse: Int?, situation: String?) -> Unit
) {
    var systolicText by remember { mutableStateOf("") }
    var diastolicText by remember { mutableStateOf("") }
    var pulseText by remember { mutableStateOf("") }
    var situation by remember { mutableStateOf("") }

    var systolicError by remember { mutableStateOf(false) }
    var diastolicError by remember { mutableStateOf(false) }
    var pulseError by remember { mutableStateOf(false) }

    val situations = listOf("Resting", "After Exercise", "Morning", "Evening", "Before Medication", "After Medication")
    var situationMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Blood Pressure") },
        text = {
            Column {
                // Systolic input
                OutlinedTextField(
                    value = systolicText,
                    onValueChange = {
                        systolicText = it.filter { char -> char.isDigit() }
                        systolicError = false
                    },
                    label = { Text("Systolic (mm Hg)") },
                    isError = systolicError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (systolicError) {
                    Text(
                        text = "Please enter a valid systolic value",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Diastolic input
                OutlinedTextField(
                    value = diastolicText,
                    onValueChange = {
                        diastolicText = it.filter { char -> char.isDigit() }
                        diastolicError = false
                    },
                    label = { Text("Diastolic (mm Hg)") },
                    isError = diastolicError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (diastolicError) {
                    Text(
                        text = "Please enter a valid diastolic value",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pulse input (optional)
                OutlinedTextField(
                    value = pulseText,
                    onValueChange = {
                        pulseText = it.filter { char -> char.isDigit() }
                        pulseError = false
                    },
                    label = { Text("Pulse (BPM, optional)") },
                    isError = pulseError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (pulseError) {
                    Text(
                        text = "Please enter a valid pulse value",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Situation dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = situation,
                        onValueChange = { situation = it },
                        label = { Text("Situation (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { situationMenuExpanded = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Situation"
                                )
                            }
                        },
                        readOnly = true
                    )

                    DropdownMenu(
                        expanded = situationMenuExpanded,
                        onDismissRequest = { situationMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        situations.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    situation = option
                                    situationMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val systolic = systolicText.toIntOrNull()
                    val diastolic = diastolicText.toIntOrNull()
                    val pulse = pulseText.takeIf { it.isNotEmpty() }?.toIntOrNull()

                    // Validate inputs
                    systolicError = systolic == null || systolic < 50 || systolic > 250
                    diastolicError = diastolic == null || diastolic < 30 || diastolic > 150
                    pulseError = pulse != null && (pulse < 30 || pulse > 220)

                    if (!systolicError && !diastolicError && !pulseError) {
                        onSave(
                            systolic!!,
                            diastolic!!,
                            pulse,
                            situation.takeIf { it.isNotEmpty() }
                        )
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