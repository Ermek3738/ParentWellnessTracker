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
import com.ermek.parentwellness.ui.components.BloodSugarChart
import com.ermek.parentwellness.ui.components.TimeRangeSelector
import com.ermek.parentwellness.ui.theme.PrimaryRed
import com.ermek.parentwellness.ui.components.SimulatedDataControls
import com.ermek.parentwellness.data.model.HealthData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodSugarScreen(
    onBack: () -> Unit,
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    val bloodSugarData by healthDataViewModel.bloodSugarData.collectAsState()
    val isLoading by healthDataViewModel.isLoading.collectAsState()
    val error by healthDataViewModel.error.collectAsState()

    var showAddBloodSugarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Sugar") },
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
                onClick = { showAddBloodSugarDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Blood Sugar",
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
                    metricType = HealthData.TYPE_BLOOD_SUGAR
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val avgBloodSugar = bloodSugarData.takeIf { it.isNotEmpty() }?.let {
                        it.sumOf { data -> data.value } / it.size
                    } ?: 0

                    val maxBloodSugar = bloodSugarData.takeIf { it.isNotEmpty() }?.maxOfOrNull { it.value } ?: 0
                    val minBloodSugar = bloodSugarData.takeIf { it.isNotEmpty() }?.minOfOrNull { it.value } ?: 0

                    StatisticItem(value = avgBloodSugar.toString(), label = "Average")
                    StatisticItem(value = maxBloodSugar.toString(), label = "Maximum")
                    StatisticItem(value = minBloodSugar.toString(), label = "Minimum")
                }

                TimeRangeSelector(
                    onRangeSelected = { timeRange ->
                        healthDataViewModel.loadDataByTimeRange(timeRange)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                BloodSugarChart(
                    data = bloodSugarData,
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

                if (bloodSugarData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No blood sugar data available")
                    }
                } else {
                    bloodSugarData.forEach { data ->
                        BloodSugarHistoryItem(data)
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

    if (showAddBloodSugarDialog) {
        AddBloodSugarDialog(
            onDismiss = { showAddBloodSugarDialog = false },
            onSave = { value, situation ->
                healthDataViewModel.addBloodSugarReading(value, situation)
                showAddBloodSugarDialog = false
            }
        )
    }
}

@Composable
fun BloodSugarHistoryItem(data: com.ermek.parentwellness.data.repository.BloodSugarData) {
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
                text = data.value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryRed,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.situation ?: "Blood Sugar",
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
            text = "${data.value} mg/dL",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryRed
        )
    }
}

@Composable
fun AddBloodSugarDialog(
    onDismiss: () -> Unit,
    onSave: (value: Int, situation: String?) -> Unit
) {
    var bloodSugarText by remember { mutableStateOf("") }
    var situation by remember { mutableStateOf("") }

    var bloodSugarError by remember { mutableStateOf(false) }

    val situations = listOf("Fasting", "Before Meal", "After Meal", "Before Sleep", "Morning", "Evening")
    var situationMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Blood Sugar") },
        text = {
            Column {
                OutlinedTextField(
                    value = bloodSugarText,
                    onValueChange = {
                        bloodSugarText = it.filter { char -> char.isDigit() }
                        bloodSugarError = false
                    },
                    label = { Text("Blood Sugar (mg/dL)") },
                    isError = bloodSugarError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (bloodSugarError) {
                    Text(
                        text = "Please enter a valid blood sugar value",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp))

                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    val bloodSugar = bloodSugarText.toIntOrNull()

                    bloodSugarError = bloodSugar == null || bloodSugar < 20 || bloodSugar > 600

                    if (!bloodSugarError) {
                        onSave(
                            bloodSugar!!,
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