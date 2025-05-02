package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import com.ermek.parentwellness.ui.components.MetricType
import com.ermek.parentwellness.ui.components.SimpleLineChart
import com.ermek.parentwellness.ui.components.StatisticItem
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodSugarScreen(
    onBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    // State variables
    val bloodSugarData by viewModel.bloodSugarData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Date range selection
    var selectedDateRange by remember { mutableStateOf("Week") }

    // Load blood sugar data when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadHealthData(HealthData.TYPE_BLOOD_SUGAR)
    }

    // Update date range when changed
    LaunchedEffect(selectedDateRange) {
        when (selectedDateRange) {
            "Week" -> viewModel.setCurrentWeek()
            "Month" -> viewModel.setCurrentMonth()
            "3 Months" -> viewModel.setLast3Months()
            "Year" -> viewModel.setCurrentYear()
        }
        viewModel.loadHealthDataByDateRange(HealthData.TYPE_BLOOD_SUGAR)
    }

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
                                    viewModel.generateSimulatedData(HealthData.TYPE_BLOOD_SUGAR, 20)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                onClick = {
                                    viewModel.loadHealthData(HealthData.TYPE_BLOOD_SUGAR)
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
                    contentDescription = "Add Blood Sugar Reading",
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
                            onClick = { viewModel.loadHealthData(HealthData.TYPE_BLOOD_SUGAR) },
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
                    // Blood sugar statistics
                    BloodSugarStatistics(bloodSugarData)

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
                            text = { Text("History (${bloodSugarData.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date range selector (only for statistics tab)
                    if (selectedTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Blood Sugar (mg/dL)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DateRangeChip(
                                    text = "Week",
                                    selected = selectedDateRange == "Week",
                                    onClick = { selectedDateRange = "Week" }
                                )

                                DateRangeChip(
                                    text = "Month",
                                    selected = selectedDateRange == "Month",
                                    onClick = { selectedDateRange = "Month" }
                                )

                                DateRangeChip(
                                    text = "3M",
                                    selected = selectedDateRange == "3 Months",
                                    onClick = { selectedDateRange = "3 Months" }
                                )

                                DateRangeChip(
                                    text = "Year",
                                    selected = selectedDateRange == "Year",
                                    onClick = { selectedDateRange = "Year" }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Content based on selected tab
                    if (selectedTab == 0) {
                        // Statistics tab
                        BloodSugarChart(bloodSugarData)

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Blood Sugar Ranges",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BloodSugarRanges()

                        Spacer(modifier = Modifier.height(24.dp))

                        BloodSugarTips()
                    } else {
                        // History tab
                        if (bloodSugarData.isEmpty()) {
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
                                        text = "No blood sugar data available",
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
                            BloodSugarHistory(
                                bloodSugarData = bloodSugarData,
                                onDeleteEntry = { id ->
                                    viewModel.deleteHealthData(id, HealthData.TYPE_BLOOD_SUGAR)
                                }
                            )
                        }
                    }
                }
            }

            // Add blood sugar dialog
            if (showAddDialog) {
                HealthDataEntryDialog(
                    metricType = MetricType.BLOOD_SUGAR,
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
fun DateRangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PrimaryRed else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, Color.Gray)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun BloodSugarStatistics(bloodSugarData: List<HealthData>) {
    val fasting = bloodSugarData
        .filter { it.situation.contains("Fasting", ignoreCase = true) || it.situation.contains("Before Meal", ignoreCase = true) }
        .takeIf { it.isNotEmpty() }
        ?.map { it.primaryValue }
        ?.average()
        ?.toInt() ?: 0

    val afterMeal = bloodSugarData
        .filter { it.situation.contains("After Meal", ignoreCase = true) }
        .takeIf { it.isNotEmpty() }
        ?.map { it.primaryValue }
        ?.average()
        ?.toInt() ?: 0

    val beforeBed = bloodSugarData
        .filter { it.situation.contains("Before Sleep", ignoreCase = true) || it.situation.contains("Before Bed", ignoreCase = true) }
        .takeIf { it.isNotEmpty() }
        ?.map { it.primaryValue }
        ?.average()
        ?.toInt() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatisticItem(value = fasting.toString(), label = "Fasting")
        StatisticItem(value = afterMeal.toString(), label = "After Meal")
        StatisticItem(value = beforeBed.toString(), label = "Before Bed")
    }
}

@Composable
fun BloodSugarChart(bloodSugarData: List<HealthData>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Chart using our SimpleLineChart component
        SimpleLineChart(
            data = bloodSugarData,
            lineColor = Color.Green,
            showPoints = true
        )
    }
}

@Composable
fun BloodSugarRanges() {
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
            BSRangeRow(
                category = "Normal Fasting",
                range = "70-99 mg/dL",
                color = Color.Green
            )

            BSRangeRow(
                category = "Prediabetes Fasting",
                range = "100-125 mg/dL",
                color = Color(0xFFFFC107) // Amber
            )

            BSRangeRow(
                category = "Diabetes Fasting",
                range = "126+ mg/dL",
                color = Color.Red
            )

            BSRangeRow(
                category = "Normal After Meal",
                range = "Less than 140 mg/dL",
                color = Color.Green
            )

            BSRangeRow(
                category = "Prediabetes After Meal",
                range = "140-199 mg/dL",
                color = Color(0xFFFFC107) // Amber
            )

            BSRangeRow(
                category = "Diabetes After Meal",
                range = "200+ mg/dL",
                color = Color.Red
            )
        }
    }
}

@Composable
fun BloodSugarTips() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Healthy Blood Sugar Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            BSRangeRow(
                category = "Regular Testing",
                range = "Test at consistent times to track trends",
                color = Color(0xFF2196F3) // Blue
            )

            Spacer(modifier = Modifier.height(8.dp))

            BSRangeRow(
                category = "Balanced Diet",
                range = "Limit refined carbs and added sugars",
                color = Color(0xFF4CAF50) // Green
            )

            Spacer(modifier = Modifier.height(8.dp))

            BSRangeRow(
                category = "Physical Activity",
                range = "Regular exercise helps regulate blood sugar",
                color = Color(0xFFFF9800) // Orange
            )

            Spacer(modifier = Modifier.height(8.dp))

            BSRangeRow(
                category = "Sleep Well",
                range = "Poor sleep can affect blood sugar levels",
                color = Color(0xFF9C27B0) // Purple
            )

            Spacer(modifier = Modifier.height(8.dp))

            BSRangeRow(
                category = "Stay Hydrated",
                range = "Drink plenty of water throughout the day",
                color = Color(0xFF03A9F4) // Light Blue
            )
        }
    }
}

@Composable
fun BSRangeRow(
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
fun BloodSugarHistory(
    bloodSugarData: List<HealthData>,
    onDeleteEntry: (String) -> Unit
) {
    Column {
        bloodSugarData.forEach { data ->
            BloodSugarHistoryItem(
                bloodSugarData = data,
                onDelete = { onDeleteEntry(data.id) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun BloodSugarHistoryItem(
    bloodSugarData: HealthData,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(bloodSugarData.timestamp))

    val value = bloodSugarData.primaryValue.toInt()
    val isFasting = bloodSugarData.situation.contains("Fasting", ignoreCase = true) ||
            bloodSugarData.situation.contains("Before Meal", ignoreCase = true)
    val isAfterMeal = bloodSugarData.situation.contains("After Meal", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blood sugar value
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Green.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Green,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bloodSugarData.situation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status tag
                val (statusText, statusColor) = when {
                    isFasting && value < 70 -> "Low" to Color.Blue
                    isFasting && value in 70..99 -> "Normal" to Color.Green
                    isFasting && value in 100..125 -> "Prediabetes" to Color(0xFFFFC107) // Amber
                    isFasting && value >= 126 -> "High" to Color.Red

                    isAfterMeal && value < 140 -> "Normal" to Color.Green
                    isAfterMeal && value in 140..199 -> "Elevated" to Color(0xFFFFC107) // Amber
                    isAfterMeal && value >= 200 -> "High" to Color.Red

                    else -> "Normal" to Color.Green
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

            if (bloodSugarData.notes.isNotEmpty()) {
                Text(
                    text = "Note: ${bloodSugarData.notes}",
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