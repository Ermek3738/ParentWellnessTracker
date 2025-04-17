package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.theme.PrimaryRed


val PrimaryLightRed = Color(0xFFFFE6E6)


@Composable
fun BarChartIcon(tint: Color) {
    Icon(
        imageVector = Icons.Filled.Add, // Placeholder, replace with proper icon
        contentDescription = "Bar Chart",
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun LineChartIcon(tint: Color) {
    Icon(
        imageVector = Icons.Filled.Add, // Placeholder, replace with proper icon
        contentDescription = "Line Chart",
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodSugarScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Sugar") },
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
                StatisticItem(value = "90.6", label = "Fasting")
                StatisticItem(value = "132.5", label = "After Meal")
                StatisticItem(value = "74.9", label = "Before Bed")
            }

            // Tab layout for Statistics/History
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Statistics", "History (220)")

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
                Text(text = "Blood Sugar (mg/dL)")

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

            // Chart type toggle
            var chartType by remember { mutableIntStateOf(0) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconToggleButton(
                    checked = chartType == 0,
                    onCheckedChange = { chartType = 0 }
                ) {
                    BarChartIcon(tint = if (chartType == 0) PrimaryRed else Color.Gray)
                }

                IconToggleButton(
                    checked = chartType == 1,
                    onCheckedChange = { chartType = 1 }
                ) {
                    LineChartIcon(tint = if (chartType == 1) PrimaryRed else Color.Gray)
                }
            }

            // Chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (chartType == 0) "Blood Sugar Bar Chart" else "Blood Sugar Line Chart"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Blood Sugar levels - History tab
            if (selectedTab == 1) {
                Column {
                    BloodSugarHistoryItem(
                        value = 75.9,
                        status = "Normal",
                        situation = "Asleep",
                        time = "Dec 22, 2024 • 11:30 PM"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    BloodSugarHistoryItem(
                        value = 120.4,
                        status = "Elevated",
                        situation = "After Eating",
                        time = "Dec 22, 2024 • 07:15 PM"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    BloodSugarHistoryItem(
                        value = 84.5,
                        status = "Normal",
                        situation = "Default",
                        time = "Dec 22, 2024 • 01:40 PM"
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
fun BloodSugarHistoryItem(
    value: Double,
    status: String,
    situation: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Value display
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PrimaryLightRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryRed
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = situation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status tag
                val statusColor = when (status) {
                    "Normal" -> Color.Green
                    "Elevated" -> Color(0xFFFFA500) // Custom orange color
                    else -> Color.Red
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        IconButton(onClick = { /* View details */ }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View details",
                tint = Color.Gray
            )
        }
    }
}