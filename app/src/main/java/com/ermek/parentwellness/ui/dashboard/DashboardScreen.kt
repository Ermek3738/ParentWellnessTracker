package com.ermek.parentwellness.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.dashboard.components.HealthMetricCard
import com.ermek.parentwellness.ui.dashboard.components.MeasurementListItem
import com.ermek.parentwellness.ui.theme.*

// Import the correct User class
import com.ermek.parentwellness.data.model.User

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToBloodPressure: () -> Unit,
    onNavigateToBloodSugar: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStepsTracker: () -> Unit = {},  // Make optional
    onNavigateToReports: () -> Unit = {},       // Make optional
    onNavigateToAddMeasurement: () -> Unit = {}, // Make optional
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToWatch: () -> Unit = {}         // Add this parameter for Watch navigation
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val user by viewModel.user.collectAsState()
    val healthData by viewModel.healthData.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* already on home */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    selected = false,
                    onClick = onNavigateToReports,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    label = { Text("Add") },
                    selected = false,
                    onClick = onNavigateToAddMeasurement,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts") },
                    selected = false,
                    onClick = onNavigateToAlerts,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryRed,
                        selectedTextColor = PrimaryRed,
                        indicatorColor = White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (dashboardState) {
                is DashboardState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryRed)
                    }
                }

                is DashboardState.Success -> {
                    DashboardContent(
                        user = user,
                        onNavigateToHeartRate = onNavigateToHeartRate,
                        onNavigateToBloodPressure = onNavigateToBloodPressure,
                        onNavigateToBloodSugar = onNavigateToBloodSugar,
                        onNavigateToStepsTracker = onNavigateToStepsTracker,
                        onNavigateToWatch = onNavigateToWatch  // Pass the parameter
                    )
                }

                is DashboardState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (dashboardState as DashboardState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    user: User?,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToBloodPressure: () -> Unit,
    onNavigateToBloodSugar: () -> Unit,
    onNavigateToStepsTracker: () -> Unit,
    onNavigateToWatch: () -> Unit  // Add this parameter
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "App Logo",
                tint = PrimaryRed,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = "Parent Wellness",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { /* Open settings */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryRed)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Greeting
        Text(
            text = "Hello, ${if (user?.fullName.isNullOrEmpty()) "there" else user.fullName}! 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Heart Rate Card
        HealthMetricCard(
            title = "Heart Rate",
            value = "72",
            subtitle = "BPM",
            backgroundColor = PrimaryLightRed,
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart Rate",
                    tint = PrimaryRed,
                    modifier = Modifier.fillMaxSize()
                )
            },
            onClick = onNavigateToHeartRate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Health Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Blood Pressure Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(4.dp)
                    .clickable(onClick = onNavigateToBloodPressure),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE3F2FD)) // Light blue
                        .padding(16.dp)

                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blood Pressure",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Text(
                                text = "120/80",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "mmHg",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Box(modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.Water,
                                contentDescription = "Blood Pressure",
                                tint = Color.Blue,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Blood Sugar Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(4.dp)
                    .clickable(onClick = onNavigateToBloodSugar),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8F5E9)) // Light green
                        .padding(16.dp)

                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blood Sugar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Text(
                                text = "95",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "mg/dL",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Box(modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = "Blood Sugar",
                                tint = Color(0xFF4CAF50), // Green
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Steps Tracker Card (replacing Weight & BMI)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(4.dp)
                    .clickable(onClick = onNavigateToStepsTracker),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFF3E0)) // Light orange
                        .padding(16.dp)

                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Steps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Text(
                                text = "8,467",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "steps today",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Box(modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Steps",
                                tint = Color(0xFFFF9800), // Orange
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // AI Doctor Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3E5F5)) // Light purple
                        .padding(16.dp)

                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Doctor",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "with AI doctor",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Box(modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.Support,
                                contentDescription = "AI Doctor",
                                tint = Color(0xFF9C27B0), // Purple
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Samsung Galaxy Watch4 Integration Card
        WatchIntegrationCard(onNavigate = onNavigateToWatch)

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Measurements
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Check-up History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = { /* View all measurements */ }) {
                Text(
                    text = "View All →",
                    color = PrimaryRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sample measurement item - in a real app, loop through recent measurements
        MeasurementListItem(
            value = "76",
            label = "Sitting",
            status = "Normal",
            time = "Today, 9:41 AM",
            onClick = onNavigateToHeartRate
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        MeasurementListItem(
            value = "120/80",
            label = "Relaxing",
            status = "Normal",
            time = "Today, 8:30 AM",
            onClick = onNavigateToBloodPressure
        )

        Spacer(modifier = Modifier.height(80.dp))  // Space for bottom navigation
    }
}

// Add the WatchIntegrationCard composable function
@Composable
fun WatchIntegrationCard(onNavigate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onNavigate() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0F7FA)) // Light cyan
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung Galaxy Watch4",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Connect and sync health data from your watch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00BCD4).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Samsung Watch",
                        tint = Color(0xFF0097A7), // Darker cyan
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}