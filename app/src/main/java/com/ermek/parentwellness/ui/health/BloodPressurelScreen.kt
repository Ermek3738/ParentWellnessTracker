package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.ui.components.DualLineChart
import com.ermek.parentwellness.ui.components.MetricType
import com.ermek.parentwellness.ui.components.StatisticItem
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureScreen(
    onBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    // State variables
    val bloodPressureData by viewModel.bloodPressureData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Load blood pressure data when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadHealthData(HealthData.TYPE_BLOOD_PRESSURE)
    }

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
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Generate Test Data") },
                                onClick = {
                                    viewModel.generateSimulatedData(HealthData.TYPE_BLOOD_PRESSURE, 20)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                onClick = {
                                    viewModel.loadHealthData(HealthData.TYPE_BLOOD_PRESSURE)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View All Data") },
                                onClick = {
                                    selectedTab = 1 // Switch to history tab
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Blood Pressure Reading",
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
            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            } else if (error != null) {
                // Error message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: $error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.loadHealthData(HealthData.TYPE_BLOOD_PRESSURE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRed
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Blood pressure statistics
                    BloodPressureStatistics(bloodPressureData)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab layout
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = PrimaryRed,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier,
                                color = PrimaryRed,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Statistics") }
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("History (${bloodPressureData.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content based on selected tab
                    if (selectedTab == 0) {
                        // Statistics tab
                        BloodPressureChart(bloodPressureData)

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Blood Pressure Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BloodPressureCategories()
                    } else {
                        // History tab
                        if (bloodPressureData.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No blood pressure data available",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showAddDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryRed
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Measurement")
                                    }
                                }
                            }
                        } else {
                            BloodPressureHistory(
                                bloodPressureData = bloodPressureData,
                                onDeleteEntry = { id ->
                                    viewModel.deleteHealthData(id, HealthData.TYPE_BLOOD_PRESSURE)
                                }
                            )
                        }
                    }
                }
            }

            // Add blood pressure dialog
            if (showAddDialog) {
                HealthDataEntryDialog(
                    metricType = MetricType.BLOOD_PRESSURE,
                    onDismiss = { showAddDialog = false },
                    onSubmit = { entry ->
                        viewModel.saveHealthData(entry)
                    }
                )
            }
        }
    }
}

@Composable
fun BloodPressureStatistics(bloodPressureData: List<HealthData>) {
    val avgSystolic = if (bloodPressureData.isNotEmpty()) {
        bloodPressureData.map { it.primaryValue }.average().toInt()
    } else {
        0
    }

    val avgDiastolic = if (bloodPressureData.isNotEmpty()) {
        bloodPressureData.mapNotNull { it.secondaryValue }.average().toInt()
    } else {
        0
    }

    val avgPulse = if (bloodPressureData.isNotEmpty()) {
        // Pulse is typically calculated as heart rate which we don't have
        // This is just a placeholder
        70
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatisticItem(value = avgSystolic.toString(), label = "Systolic")
        StatisticItem(value = avgDiastolic.toString(), label = "Diastolic")
        StatisticItem(value = avgPulse.toString(), label = "Pulse")
    }
}

@Composable
fun BloodPressureChart(bloodPressureData: List<HealthData>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Date range selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blood Pressure (mmHg)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart using our DualLineChart component
        DualLineChart(
            data = bloodPressureData,
            primaryLineColor = Color.Red,  // Systolic
            secondaryLineColor = Color.Blue // Diastolic
        )
    }
}

@Composable
fun BloodPressureCategories() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BPCategoryRow(
                category = "Normal",
                range = "Less than 120/80 mmHg",
                color = Color.Green
            )

            BPCategoryRow(
                category = "Elevated",
                range = "120-129/<80 mmHg",
                color = Color(0xFFFFC107) // Amber
            )

            BPCategoryRow(
                category = "High (Stage 1)",
                range = "130-139/80-89 mmHg",
                color = Color(0xFFFF9800) // Orange
            )

            BPCategoryRow(
                category = "High (Stage 2)",
                range = "140+/90+ mmHg",
                color = Color.Red
            )

            BPCategoryRow(
                category = "Crisis",
                range = "180+/120+ mmHg",
                color = Color(0xFF8B0000) // Dark red
            )
        }
    }
}

@Composable
fun BPCategoryRow(
    category: String,
    range: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = range,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BloodPressureHistory(
    bloodPressureData: List<HealthData>,
    onDeleteEntry: (String) -> Unit
) {
    Column {
        bloodPressureData.forEach { data ->
            BloodPressureHistoryItem(
                bloodPressureData = data,
                onDelete = { onDeleteEntry(data.id) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun BloodPressureHistoryItem(
    bloodPressureData: HealthData,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(bloodPressureData.timestamp))

    val systolic = bloodPressureData.primaryValue.toInt()
    val diastolic = bloodPressureData.secondaryValue?.toInt() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blood pressure values
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                text = "$systolic",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryRed
            )
            Text(
                text = "$diastolic",
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryRed
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bloodPressureData.situation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status tag
                val (statusText, statusColor) = when {
                    systolic < 120 && diastolic < 80 -> "Normal" to Color.Green
                    systolic in 120..129 && diastolic < 80 -> "Elevated" to Color(0xFFFFC107) // Amber
                    (systolic in 130..139 || diastolic in 80..89) -> "Stage 1" to Color(0xFFFF9800) // Orange
                    (systolic >= 140 || diastolic >= 90) -> "Stage 2" to Color.Red
                    (systolic >= 180 || diastolic >= 120) -> "Crisis" to Color(0xFF8B0000) // Dark red
                    else -> "Unknown" to Color.Gray
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }

            Text(
                text = "$formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (bloodPressureData.notes.isNotEmpty()) {
                Text(
                    text = "Note: ${bloodPressureData.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Gray
            )
        }
    }
}