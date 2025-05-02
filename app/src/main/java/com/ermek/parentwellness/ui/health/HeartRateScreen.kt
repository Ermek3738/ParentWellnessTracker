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
import com.ermek.parentwellness.ui.components.MetricType
import com.ermek.parentwellness.ui.components.SimpleLineChart
import com.ermek.parentwellness.ui.components.StatisticItem
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    onBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    // State variables
    val heartRateData by viewModel.heartRateData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Load heart rate data when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadHealthData(HealthData.TYPE_HEART_RATE)
    }

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
                                    viewModel.generateSimulatedData(HealthData.TYPE_HEART_RATE, 20)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                onClick = {
                                    viewModel.loadHealthData(HealthData.TYPE_HEART_RATE)
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
                            onClick = { viewModel.loadHealthData(HealthData.TYPE_HEART_RATE) },
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
                    // Heart rate statistics
                    HeartRateStatistics(heartRateData)

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
                            text = { Text("History (${heartRateData.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content based on selected tab
                    if (selectedTab == 0) {
                        // Statistics tab
                        HeartRateChart(heartRateData)

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Recommended to Read",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ArticleCard(
                                title = "Understanding Your Heart Rate",
                                modifier = Modifier.weight(1f)
                            )

                            ArticleCard(
                                title = "Heart Rate for Exercise",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // History tab
                        if (heartRateData.isEmpty()) {
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
                                        text = "No heart rate data available",
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
                            HeartRateHistory(
                                heartRateData = heartRateData,
                                onDeleteEntry = { id ->
                                    viewModel.deleteHealthData(id, HealthData.TYPE_HEART_RATE)
                                }
                            )
                        }
                    }
                }
            }

            // Add heart rate dialog
            if (showAddDialog) {
                HealthDataEntryDialog(
                    metricType = MetricType.HEART_RATE,
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
fun HeartRateStatistics(heartRateData: List<HealthData>) {
    val averageHeartRate = if (heartRateData.isNotEmpty()) {
        heartRateData.map { it.primaryValue }.average().toInt()
    } else {
        0
    }

    val maxHeartRate = if (heartRateData.isNotEmpty()) {
        heartRateData.maxOf { it.primaryValue }.toInt()
    } else {
        0
    }

    val minHeartRate = if (heartRateData.isNotEmpty()) {
        heartRateData.minOfOrNull { it.primaryValue }?.toInt() ?: 0
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatisticItem(value = averageHeartRate.toString(), label = "Average")
        StatisticItem(value = maxHeartRate.toString(), label = "Maximum")
        StatisticItem(value = minHeartRate.toString(), label = "Minimum")
    }
}

@Composable
fun HeartRateChart(heartRateData: List<HealthData>) {
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
                text = "Heart Rate (bpm)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart using our SimpleLineChart component
        SimpleLineChart(
            data = heartRateData,
            lineColor = PrimaryRed,
            showPoints = true
        )
    }
}

@Composable
fun HeartRateHistory(
    heartRateData: List<HealthData>,
    onDeleteEntry: (String) -> Unit
) {
    Column {
        heartRateData.forEach { data ->
            HeartRateHistoryItem(
                heartRateData = data,
                onDelete = { onDeleteEntry(data.id) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun HeartRateHistoryItem(
    heartRateData: HealthData,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(heartRateData.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Heart rate value
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PrimaryRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = heartRateData.primaryValue.toInt().toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryRed,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = heartRateData.situation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status tag
                val heartRate = heartRateData.primaryValue.toInt()
                val (statusText, statusColor) = when {
                    heartRate < 60 -> "Low" to Color.Blue
                    heartRate > 100 -> "High" to Color.Red
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

            if (heartRateData.notes.isNotEmpty()) {
                Text(
                    text = "Note: ${heartRateData.notes}",
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

@Composable
fun ArticleCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(120.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}