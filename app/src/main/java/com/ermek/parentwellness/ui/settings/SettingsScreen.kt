package com.ermek.parentwellness.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.dashboard.DashboardViewModel
import com.ermek.parentwellness.ui.health.HealthDataViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    notificationSettingsViewModel: NotificationSettingsViewModel = viewModel()
) {
    val preferencesState by notificationSettingsViewModel.preferencesState.collectAsState()

    // Load notification preferences when the screen is first shown
    LaunchedEffect(key1 = Unit) {
        notificationSettingsViewModel.loadNotificationPreferences()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Notification Settings Section
            NotificationSettingsSection(
                preferencesState = preferencesState,
                onUpdateHeartRateAlerts = { notificationSettingsViewModel.updateHeartRateAlerts(it) },
                onUpdateBloodPressureAlerts = { notificationSettingsViewModel.updateBloodPressureAlerts(it) },
                onUpdateBloodSugarAlerts = { notificationSettingsViewModel.updateBloodSugarAlerts(it) },
                onUpdateWeeklyReports = { notificationSettingsViewModel.updateWeeklyReports(it) },
                onUpdateCaregiverAlerts = { notificationSettingsViewModel.updateCaregiverAlerts(it) },
                onSavePreferences = { notificationSettingsViewModel.savePreferences() }
            )

            // Demo Mode Section
            DemoModeSection()
        }
    }
}

@Composable
fun NotificationSettingsSection(
    preferencesState: NotificationSettingsState,
    onUpdateHeartRateAlerts: (Boolean) -> Unit,
    onUpdateBloodPressureAlerts: (Boolean) -> Unit,
    onUpdateBloodSugarAlerts: (Boolean) -> Unit,
    onUpdateWeeklyReports: (Boolean) -> Unit,
    onUpdateCaregiverAlerts: (Boolean) -> Unit,
    onSavePreferences: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notification Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when (preferencesState) {
                is NotificationSettingsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }
                is NotificationSettingsState.Error -> {
                    Text(
                        text = "Error: ${preferencesState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is NotificationSettingsState.Success -> {
                    val preferences = preferencesState.preferences

                    NotificationToggleItem(
                        title = "Heart Rate Alerts",
                        description = "Receive alerts for abnormal heart rate readings",
                        checked = preferences.heartRateAlerts,
                        onCheckedChange = onUpdateHeartRateAlerts
                    )

                    NotificationToggleItem(
                        title = "Blood Pressure Alerts",
                        description = "Receive alerts for abnormal blood pressure readings",
                        checked = preferences.bloodPressureAlerts,
                        onCheckedChange = onUpdateBloodPressureAlerts
                    )

                    NotificationToggleItem(
                        title = "Blood Sugar Alerts",
                        description = "Receive alerts for abnormal blood sugar readings",
                        checked = preferences.bloodSugarAlerts,
                        onCheckedChange = onUpdateBloodSugarAlerts
                    )

                    NotificationToggleItem(
                        title = "Weekly Health Reports",
                        description = "Receive weekly summaries of your health data",
                        checked = preferences.weeklyReports,
                        onCheckedChange = onUpdateWeeklyReports
                    )

                    NotificationToggleItem(
                        title = "Caregiver Alerts",
                        description = "Send alerts to connected caregivers for abnormal readings",
                        checked = preferences.caregiverAlerts,
                        onCheckedChange = onUpdateCaregiverAlerts
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSavePreferences,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Notification Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
@Composable
fun DemoModeSection() {
    var demoModeEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val healthDataViewModel: HealthDataViewModel = viewModel()
    var showConfirmDialog by remember { mutableStateOf(false) }

    // To fix the dashboard view model issues:
    val dashboardViewModel: DashboardViewModel = viewModel()

    // Initialize demoModeEnabled from shared preferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        demoModeEnabled = prefs.getBoolean("demo_mode_enabled", false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Demo Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Demo Mode",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Generate realistic health data to demonstrate app features",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = demoModeEnabled,
                    onCheckedChange = {
                        if (it) {
                            showConfirmDialog = true
                        } else {
                            demoModeEnabled = false
                            // Save to preferences
                            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                .edit { putBoolean("demo_mode_enabled", false) }
                            // Update dashboard
                            if (true) {
                                dashboardViewModel.setDemoModeEnabled(false)
                            }
                        }
                    }
                )
            }

            if (demoModeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Generate data for all metrics
                        val coroutineScope = CoroutineScope(Dispatchers.Main)
                        coroutineScope.launch {
                            Toast.makeText(context, "Generating demo data...", Toast.LENGTH_SHORT).show()

                            healthDataViewModel.generateHeartRateData()
                            healthDataViewModel.generateBloodPressureData()
                            healthDataViewModel.generateBloodSugarData()
                            healthDataViewModel.generateStepsData()

                            // Update dashboard
                            if (true) {
                                dashboardViewModel.updateDashboardWithSimulatedData()
                            }

                            Toast.makeText(context, "Demo data generated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Regenerate All Demo Data")
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Enable Demo Mode") },
            text = {
                Text("This will generate simulated health data with realistic patterns and some abnormal readings to demonstrate the app's features. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        demoModeEnabled = true
                        // Save to preferences
                        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            .edit { putBoolean("demo_mode_enabled", true) }
                        // Update dashboard
                        if (true) {
                            dashboardViewModel.setDemoModeEnabled(true)
                        }
                        showConfirmDialog = false

                        // Generate initial data for all metrics
                        val coroutineScope = CoroutineScope(Dispatchers.Main)
                        coroutineScope.launch {
                            Toast.makeText(context, "Generating demo data...", Toast.LENGTH_SHORT).show()

                            healthDataViewModel.generateHeartRateData()
                            healthDataViewModel.generateBloodPressureData()
                            healthDataViewModel.generateBloodSugarData()
                            healthDataViewModel.generateStepsData()

                            // Update dashboard
                            if (true) {
                                dashboardViewModel.updateDashboardWithSimulatedData()
                            }

                            Toast.makeText(context, "Demo data generated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}