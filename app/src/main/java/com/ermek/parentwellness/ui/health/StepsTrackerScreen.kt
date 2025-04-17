package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsTrackerScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with average stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem(value = "76", label = "Average")
                StatisticItem(value = "104", label = "Maximum")
                StatisticItem(value = "60", label = "Minimum")
            }

            // Tab layout for Statistics/History
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Statistics", "History (296)")

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
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) PrimaryRed else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date range selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Heart Rate (bpm)")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Previous date range */ }) {
                        Text("<", fontWeight = FontWeight.Bold)
                    }

                    Text("Dec 16 - Dec 22, 2024")

                    IconButton(onClick = { /* Next date range */ }) {
                        Text(">", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == 0) {
                    Text("Heart Rate Chart")
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        ActivityHistoryItem(
                            heartRate = 76,
                            status = "Normal",
                            activity = "Sitting",
                            time = "Dec 23, 2024 • 09:41 AM"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ActivityHistoryItem(
                            heartRate = 85,
                            status = "Normal",
                            activity = "Exercise",
                            time = "Dec 22, 2024 • 05:18 PM"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ActivityHistoryItem(
                            heartRate = 95,
                            status = "Normal",
                            activity = "Jogging",
                            time = "Dec 21, 2024 • 06:30 PM"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ActivityHistoryItem(
                            heartRate = 83,
                            status = "Normal",
                            activity = "Walking",
                            time = "Dec 20, 2024 • 07:43 AM"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Record details
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { /* Delete record */ }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Heart Rate summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(PrimaryLightRed, RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "76",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryRed
                            )
                        }
                    }

                    Text(
                        text = "Heart Rate",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Results summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Activity details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailItem(label = "Activity", value = "Sitting")
                        DetailItem(label = "Time", value = "9:41 AM")
                        DetailItem(label = "Age", value = "28")
                        DetailItem(label = "Gender", value = "Male")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Normal range indicator
                    Text(
                        text = "Normal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Range slider
                    LinearProgressIndicator(
                        progress = { 0.4f }, // Progress as a lambda expression
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = Color.LightGray,
                        color = Color.Green
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Range labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slow", style = MaterialTheme.typography.bodySmall, color = Color.Blue)
                        Text("Normal", style = MaterialTheme.typography.bodySmall, color = Color.Green)
                        Text("Fast", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Health metrics
                    HealthMetricItem(value = "43", unit = "%", label = "Stress", color = Color.Green)

                    Spacer(modifier = Modifier.height(8.dp))

                    HealthMetricItem(value = "112", unit = "/min", label = "HRV", color = Color.Green)

                    Spacer(modifier = Modifier.height(8.dp))

                    HealthMetricItem(value = "80", unit = "%", label = "Energy", color = Color.Green)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recommended reading
            if (selectedTab == 0) {
                Text(
                    text = "Recommended to Read",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Article cards
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = { /* Add new measurement */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityHistoryItem(
    heartRate: Int,
    status: String,
    activity: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Heart rate in circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PrimaryLightRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = heartRate.toString(),
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
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Green.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
            }

            Text(
                text = "$activity • $time",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        IconButton(onClick = { /* Navigate to record details */ }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "See details",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun HealthMetricItem(
    value: String,
    unit: String,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Normal",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}
