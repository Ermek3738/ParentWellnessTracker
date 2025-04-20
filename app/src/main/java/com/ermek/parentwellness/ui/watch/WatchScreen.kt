package com.ermek.parentwellness.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ermek.parentwellness.data.samsung.SensorConnectionManager.ConnectionState
import com.ermek.parentwellness.ui.watch.WatchViewModel.HeartRateState
import com.ermek.parentwellness.ui.watch.WatchViewModel.StepsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    viewModel: WatchViewModel,
    onNavigateBack: () -> Unit
) {
    val heartRateState by viewModel.heartRateState.collectAsState()
    val stepsState by viewModel.stepsState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()

    // Activity context for resolving connection issues
    val context = LocalContext.current

    // Track if we need to show connection issue resolution
    var showConnectionResolution by remember { mutableStateOf(false) }

    // Handle resolution required state
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.ResolutionRequired) {
            showConnectionResolution = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch Health Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshWatchData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Connection Status Card
            ConnectionStatusCard(
                connectionState = connectionState,
                isInitialized = isInitialized,
                onResolve = {
                    val activity = context as? android.app.Activity
                    activity?.let { viewModel.resolveConnectionIssue(it) }
                },
                showResolution = showConnectionResolution
            )

            // Heart Rate Card
            HeartRateCard(heartRateState)

            // Steps Card
            StepsCard(stepsState)
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    isInitialized: Boolean,
    onResolve: () -> Unit,
    showResolution: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (connectionState) {
                is ConnectionState.Connected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = "Connected to Samsung Health",
                            color = Color.Green
                        )
                    }
                }
                is ConnectionState.Connecting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(text = "Connecting to Samsung Health...")
                    }
                }
                is ConnectionState.Disconnected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Gray, CircleShape)
                        )
                        Text(text = "Disconnected from Samsung Health")
                    }
                }
                is ConnectionState.Error -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Text(
                                text = "Connection Error",
                                color = Color.Red
                            )
                        }
                        Text(
                            text = connectionState.exception.message ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
                is ConnectionState.ResolutionRequired -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFFFA500) // Use Color(0xFFFFA500) for Orange
                            )
                            Text(
                                text = "Action Required",
                                color = Color(0xFFFFA500) // Use Color(0xFFFFA500) for Orange
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (connectionState.exception.errorCode) {
                                0 -> "Samsung Health service is not installed"
                                1 -> "Samsung Health service needs to be updated"
                                else -> "Samsung Health connection issue"
                            },
                            textAlign = TextAlign.Center
                        )

                        if (showResolution) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = onResolve) {
                                Text("Resolve Issue")
                            }
                        }
                    }
                }
            }

            if (!isInitialized) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initializing Samsung Health integration...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun HeartRateCard(heartRateState: HeartRateState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = "Heart Rate",
                    tint = Color.Red
                )
                Text(
                    text = "Heart Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (heartRateState) {
                is HeartRateState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading heart rate data...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is HeartRateState.Success -> {
                    val textColor = if (heartRateState.isAbnormal) Color.Red else Color.Unspecified

                    Text(
                        text = "${heartRateState.heartRate}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Last updated: ${heartRateState.timestamp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (heartRateState.isAbnormal) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Abnormal heart rate detected",
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                is HeartRateState.Error -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = heartRateState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StepsCard(stepsState: StepsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.DirectionsRun,
                    contentDescription = "Steps",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Daily Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (stepsState) {
                is StepsState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading steps data...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is StepsState.Success -> {
                    Text(
                        text = "${stepsState.steps}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "steps",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Goal: ${stepsState.dailyGoal}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${stepsState.progress}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { stepsState.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Last updated: ${stepsState.timestamp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                is StepsState.Error -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stepsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}