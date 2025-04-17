package com.ermek.parentwellness.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToHeartRate: () -> Unit,
    onNavigateToBloodPressure: () -> Unit,
    onNavigateToBloodSugar: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
            // Time period selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time Period",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date range buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeRangeButton(text = "Week", selected = true)
                        TimeRangeButton(text = "Month", selected = false)
                        TimeRangeButton(text = "3 Months", selected = false)
                        TimeRangeButton(text = "Year", selected = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heart Rate Report
            ReportCard(
                title = "Heart Rate",
                value = "76",
                unit = "BPM",
                trend = "+5%",
                trendUp = true,
                chartContent = {
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Heart Rate Chart")
                    }
                },
                onClick = onNavigateToHeartRate
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Blood Pressure Report
            ReportCard(
                title = "Blood Pressure",
                value = "120/80",
                unit = "mmHg",
                trend = "-2%",
                trendUp = false,
                chartContent = {
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Blood Pressure Chart")
                    }
                },
                onClick = onNavigateToBloodPressure
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Blood Sugar Report
            ReportCard(
                title = "Blood Sugar",
                value = "95",
                unit = "mg/dL",
                trend = "Stable",
                trendUp = null,
                chartContent = {
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Blood Sugar Chart")
                    }
                },
                onClick = onNavigateToBloodSugar
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Activity Report
            ReportCard(
                title = "Activity",
                value = "8,456",
                unit = "steps",
                trend = "+12%",
                trendUp = true,
                chartContent = {
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Activity Chart")
                    }
                },
                onClick = { /* Navigate to activity details */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Monthly Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProgressItem(
                        label = "Steps Goal",
                        current = 8456,
                        target = 10000,
                        color = Color.Blue
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ProgressItem(
                        label = "Blood Pressure Goal",
                        current = 85,
                        target = 100,
                        color = Color.Green
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ProgressItem(
                        label = "Heart Rate Goal",
                        current = 70,
                        target = 100,
                        color = PrimaryRed
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRangeButton(
    text: String,
    selected: Boolean
) {
    OutlinedButton(
        onClick = { /* Select this time range */ },
        modifier = Modifier.padding(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PrimaryRed else Color.Transparent,
            contentColor = if (selected) Color.White else Color.Gray
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) PrimaryRed else Color.Gray
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun ReportCard(
    title: String,
    value: String,
    unit: String,
    trend: String,
    trendUp: Boolean?,
    chartContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trendUp != null) {
                        Icon(
                            imageVector = if (trendUp)
                                Icons.AutoMirrored.Filled.TrendingUp
                            else
                                Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = if (trendUp) "Trending Up" else "Trending Down",
                            tint = if (trendUp) Color.Green else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = trend,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (trendUp) {
                            true -> Color.Green
                            false -> Color.Red
                            null -> Color.Gray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            chartContent()
        }
    }
}

@Composable
fun ProgressItem(
    label: String,
    current: Int,
    target: Int,
    color: Color
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Text(
                text = "$current/$target",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = Color.LightGray,
            color = color
        )
    }
}