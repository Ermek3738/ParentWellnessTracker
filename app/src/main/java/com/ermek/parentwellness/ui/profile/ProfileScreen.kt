package com.ermek.parentwellness.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToManageCaregivers: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onSwitchRole: () -> Unit = {},
    showRoleSwitcher: Boolean = false,
    viewModel: ProfileViewModel = viewModel()
) {
    // State to hold user data
    val userState by viewModel.currentUser.collectAsState()

    // Effect to load user when screen is displayed
    LaunchedEffect(key1 = Unit) {
        viewModel.loadCurrentUser()
    }

    // Loading state
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else if (userState != null) {
            val user = userState!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )

                    // Edit button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Using if empty instead of Elvis operator
                Text(
                    text = if (user.fullName.isEmpty()) "User" else user.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Role badge instead of simple text
                RoleBadge(isParent = user.isParent)

                Spacer(modifier = Modifier.height(24.dp))

                // Personal Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Personal Info",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = onEditProfile) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Personal Info",
                                    tint = PrimaryRed
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ProfileInfoItem(
                            icon = Icons.Default.Person,
                            label = "Full Name",
                            value = if (user.fullName.isEmpty()) "Not set" else user.fullName
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = if (user.email.isEmpty()) "Not set" else user.email
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Phone,
                            label = "Phone Number",
                            value = if (user.phoneNumber.isEmpty()) "Not set" else user.phoneNumber
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Wc,
                            label = "Gender",
                            value = if (user.gender.isEmpty()) "Not set" else user.gender
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Cake,
                            label = "Date of Birth",
                            value = if (user.birthDate.isEmpty()) "Not set" else user.birthDate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role switcher (only shown if user has both roles)
                if (showRoleSwitcher) {
                    Button(
                        onClick = onSwitchRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isParent) Color.Blue else PrimaryRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Switch Role",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch to ${if (user.isParent) "Caregiver" else "Parent"} Mode",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Health Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Health Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsItem(
                            icon = Icons.Default.Favorite,
                            title = "Heart Rate Alerts",
                            subtitle = "Set your heart rate alert thresholds"
                        )

                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = "Blood Pressure Targets",
                            subtitle = "Customize your blood pressure goals"
                        )

                        SettingsItem(
                            icon = Icons.Default.Opacity,
                            title = "Blood Sugar Range",
                            subtitle = "Configure your ideal blood sugar range"
                        )

                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            title = "Activity Goals",
                            subtitle = "Set your daily step and activity targets"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Notification Settings",
                            subtitle = "Configure how you receive alerts"
                        )

                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = "Privacy & Security",
                            subtitle = "Manage data sharing and security settings"
                        )

                        SettingsItem(
                            icon = Icons.Default.Watch,
                            title = "Connected Devices",
                            subtitle = "Manage your Samsung Galaxy Watch4"
                        )
                        SettingsItem(
                            icon = Icons.Default.Call,
                            title = "Emergency Contacts",
                            subtitle = "Manage your emergency contacts",
                            onClick = onNavigateToEmergencyContacts
                        )

                        if (user.isParent) {
                            SettingsItem(
                                icon = Icons.Default.People,
                                title = "Manage Caregivers",
                                subtitle = "Add or remove people who can monitor your health",
                                onClick = onNavigateToManageCaregivers
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.Red
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color.Red
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App info
                Text(
                    text = "Parent Wellness v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            // Error or no user state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Unable to load profile data")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadCurrentUser() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun RoleBadge(isParent: Boolean) {
    val (backgroundColor, contentColor, text) = if (isParent) {
        Triple(PrimaryRed.copy(alpha = 0.1f), PrimaryRed, "Parent")
    } else {
        Triple(Color.Blue.copy(alpha = 0.1f), Color.Blue, "Caregiver")
    }

    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isParent) Icons.Default.Person else Icons.Default.Favorite,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryRed,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryRed,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Only show chevron if there's an onClick handler
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }
    }
}