package com.ermek.parentwellness.ui.caregiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.ui.profile.ProfileViewModel
import com.ermek.parentwellness.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit,
    onManageParents: () -> Unit,
    onSwitchRole: () -> Unit = {},
    showRoleSwitcher: Boolean,
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
                title = { Text("Caregiver Profile") },
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

                // Role badge
                Surface(
                    modifier = Modifier.padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Blue.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Blue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Caregiver",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Blue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Role switcher (only shown if user has both roles)
                if (showRoleSwitcher) {
                    Button(
                        onClick = onSwitchRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Switch Role",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch to Parent Mode",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

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
                            icon = Icons.Default.People,
                            title = "Manage Parents",
                            subtitle = "View and add parents you're caring for",
                            onClick = onManageParents
                        )

                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Notification Settings",
                            subtitle = "Configure how you receive alerts",
                            onClick = { /* Open notification settings */ }
                        )

                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = "Privacy & Security",
                            subtitle = "Manage data sharing and security settings",
                            onClick = { /* Open privacy settings */ }
                        )

                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            title = "Help & Support",
                            subtitle = "Get assistance with the app",
                            onClick = { /* Open help center */ }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            subtitle = "Learn about Parent Wellness",
                            onClick = { /* Open about page */ }
                        )
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
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            tint = Color.Blue,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Blue,
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