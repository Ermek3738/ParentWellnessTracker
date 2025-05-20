package com.ermek.parentwellness

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.samsung.SamsungHealthSensorManager
import com.ermek.parentwellness.data.samsung.workers.SensorSyncWorker
import com.ermek.parentwellness.ui.alerts.AlertsScreen
import com.ermek.parentwellness.ui.auth.AuthState
import com.ermek.parentwellness.ui.auth.AuthViewModel
import com.ermek.parentwellness.ui.auth.LoginScreen
import com.ermek.parentwellness.ui.auth.RegisterScreen
import com.ermek.parentwellness.ui.caregiver.*
import com.ermek.parentwellness.ui.dashboard.DashboardScreen
import com.ermek.parentwellness.ui.dashboard.DashboardViewModel
import com.ermek.parentwellness.ui.emergency.EmergencyContactsScreen
import com.ermek.parentwellness.ui.health.BloodPressureScreen
import com.ermek.parentwellness.ui.health.BloodSugarScreen
import com.ermek.parentwellness.ui.health.HealthDataViewModel
import com.ermek.parentwellness.ui.health.HeartRateScreen
import com.ermek.parentwellness.ui.health.StepsTrackerScreen
import com.ermek.parentwellness.ui.onboarding.OnboardingScreen
import com.ermek.parentwellness.ui.profile.EditProfileScreen
import com.ermek.parentwellness.ui.profile.ProfileScreen
import com.ermek.parentwellness.ui.profile.ProfileViewModel
import com.ermek.parentwellness.ui.reports.ReportsScreen
import com.ermek.parentwellness.ui.role.RoleSelectionScreen
import com.ermek.parentwellness.ui.theme.ParentWellnessTheme
import com.ermek.parentwellness.ui.watch.WatchScreen
import com.ermek.parentwellness.ui.watch.WatchViewModel
import com.ermek.parentwellness.ui.settings.SettingsScreen
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var watchViewModel: WatchViewModel
    private lateinit var samsungHealthSensorManager: SamsungHealthSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Store current activity reference (for Samsung Health SDK resolution)
        ActivityProvider.setCurrentActivity(this)

        // Initialize Samsung Health Sensor Manager
        samsungHealthSensorManager = SamsungHealthSensorManager(applicationContext)

        // Initialize WatchViewModel with proper factory
        watchViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[WatchViewModel::class.java]

        // Create ViewModel factory for HealthDataViewModel
        val factory = ViewModelProvider.AndroidViewModelFactory(application)

        // Set up WorkManager for periodic sync
        setupSensorSyncWorker()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Current FCM token: $token")
            } else {
                Log.w("FCM_TOKEN", "Fetching FCM token failed", task.exception)
            }
        }

        setContent {
            ParentWellnessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel by viewModels()
                    val navController = rememberNavController()
                    val authState by authViewModel.authState.collectAsState()

                    // State to track user mode (parent or caregiver)
                    var currentUser by remember { mutableStateOf<User?>(null) }

                    // Update user data when authState changes
                    LaunchedEffect(authState) {
                        if (authState is AuthState.Authenticated) {
                            currentUser = (authState as AuthState.Authenticated).user
                        }
                    }

                    // Determine starting destination based on authentication state
                    val startDestination = when (authState) {
                        is AuthState.Authenticated -> {
                            val user = (authState as AuthState.Authenticated).user
                            if (user.isParent && user.parentIds.isNotEmpty()) {
                                "role_selection"
                            } else if (user.isParent) {
                                "dashboard"
                            } else {
                                "caregiver_dashboard"
                            }
                        }
                        else -> "onboarding"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // Role selection screen
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onSelectParentRole = {
                                    navController.navigate("dashboard") {
                                        popUpTo("role_selection") { inclusive = true }
                                    }
                                },
                                onSelectCaregiverRole = {
                                    navController.navigate("caregiver_dashboard") {
                                        popUpTo("role_selection") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Onboarding Flow
                        composable("onboarding") {
                            OnboardingScreen(
                                onNavigateToLogin = { navController.navigate("login") },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }

                        // Authentication Screens
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToForgotPassword = { /* TODO */ },
                                onLoginSuccess = {
                                    when (authState) {
                                        is AuthState.Authenticated -> {
                                            val user = (authState as AuthState.Authenticated).user
                                            navigateBasedOnRole(navController, user)
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("onboarding")
                                    }
                                },
                                onRegistrationSuccess = {
                                    when (authState) {
                                        is AuthState.Authenticated -> {
                                            val user = (authState as AuthState.Authenticated).user
                                            navigateBasedOnRole(navController, user)
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }

                        // Parent Dashboard Screen
                        composable("dashboard") {
                            val context = LocalContext.current
                            val dashboardViewModel: DashboardViewModel = viewModel(
                                factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
                            )

                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToHeartRate = { navController.navigate("heart_rate") },
                                onNavigateToBloodPressure = { navController.navigate("blood_pressure") },
                                onNavigateToBloodSugar = { navController.navigate("blood_sugar") },
                                onNavigateToStepsTracker = { navController.navigate("steps_tracker") },
                                onNavigateToReports = { navController.navigate("reports") },
                                onNavigateToAlerts = { navController.navigate("alerts") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToWatch = { navController.navigate("watch") },
                                onSwitchToCaregiverMode = {
                                    if (currentUser?.parentIds?.isNotEmpty() == true) {
                                        navController.navigate("caregiver_dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // Caregiver Screens
                        composable("caregiver_dashboard") {
                            val viewModel: CaregiverViewModel = viewModel()
                            CaregiverDashboardScreen(
                                viewModel = viewModel,
                                onNavigateToManageParents = { navController.navigate("manage_parents") },
                                onNavigateToParentDetail = { /* handle later */ },
                                onNavigateToHeartRate = { navController.navigate("heart_rate") },
                                onNavigateToBloodPressure = { navController.navigate("blood_pressure") },
                                onNavigateToBloodSugar = { navController.navigate("blood_sugar") },
                                onNavigateToStepsTracker = { navController.navigate("steps_tracker") },
                                onNavigateToEmergencyContact = { navController.navigate("emergency_contacts") },
                                onNavigateToReports = {
                                    navController.navigate("caregiver_reports") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToAlerts = {
                                    navController.navigate("caregiver_alerts") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToProfile = {
                                    navController.navigate("caregiver_profile") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onSwitchToParentMode = {
                                    if (currentUser?.isParent == true) {
                                        navController.navigate("dashboard") {
                                            popUpTo("caregiver_dashboard") { inclusive = true }
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("caregiver_reports") {
                            ReportsScreen(
                                onNavigateToHeartRate = { navController.navigate("heart_rate") },
                                onNavigateToBloodPressure = { navController.navigate("blood_pressure") },
                                onNavigateToBloodSugar = { navController.navigate("blood_sugar") }
                            )
                        }

                        composable("caregiver_alerts") {
                            CaregiverAlertsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("caregiver_profile") {
                            CaregiverProfileScreen(
                                onSignOut = {
                                    authViewModel.signOut()
                                    navController.navigate("onboarding") {
                                        popUpTo("caregiver_dashboard") { inclusive = true }
                                    }
                                },
                                onEditProfile = { navController.navigate("edit_profile") },
                                onManageParents = { navController.navigate("manage_parents") },
                                onSwitchRole = {
                                    if (currentUser?.isParent == true) {
                                        navController.navigate("dashboard") {
                                            popUpTo("caregiver_dashboard") { inclusive = true }
                                        }
                                    }
                                },
                                showRoleSwitcher = currentUser?.isParent == true,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("manage_parents") {
                            val viewModel: CaregiverViewModel = viewModel()

                            // Load the necessary data in this ViewModel when it's created
                            LaunchedEffect(Unit) {
                                viewModel.loadParentsForCurrentUser()
                            }
                            ManageParentsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSelectParent = { parent ->
                                    viewModel.selectParent(parent)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("manage_caregivers") {
                            val viewModel: CaregiverViewModel = viewModel()

                            LaunchedEffect(Unit) {
                                viewModel.loadCaregiversForCurrentUser()
                            }

                            ManageCaregiversScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel  // Pass the shared viewModel
                            )
                        }

                        // Health Tracking Screens
                        composable("heart_rate") {
                            val viewModel: HealthDataViewModel = viewModel(factory = factory)
                            HeartRateScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("blood_pressure") {
                            val viewModel: HealthDataViewModel = viewModel(factory = factory)
                            BloodPressureScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("blood_sugar") {
                            val viewModel: HealthDataViewModel = viewModel(factory = factory)
                            BloodSugarScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("steps_tracker") {
                            val viewModel: HealthDataViewModel = viewModel(factory = factory)
                            StepsTrackerScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        // Watch Screen
                        composable("watch") {
                            WatchScreen(
                                viewModel = watchViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Additional Screens
                        composable("reports") {
                            ReportsScreen(
                                onNavigateToHeartRate = { navController.navigate("heart_rate") },
                                onNavigateToBloodPressure = { navController.navigate("blood_pressure") },
                                onNavigateToBloodSugar = { navController.navigate("blood_sugar") }
                            )
                        }

                        composable("alerts") {
                            AlertsScreen(
                            )
                        }

                        // Profile Screens
                        composable("profile") {
                            val profileViewModel: ProfileViewModel = viewModel()
                            ProfileScreen(
                                onSignOut = {
                                    authViewModel.signOut()
                                    navController.navigate("onboarding") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onEditProfile = { navController.navigate("edit_profile") },
                                onNavigateToManageCaregivers = { navController.navigate("manage_caregivers") },
                                onNavigateToEmergencyContacts = { navController.navigate("emergency_contacts") },
                                // Add this navigation to settings
                                onNavigateToSettings = { navController.navigate("settings") },
                                onSwitchRole = {
                                    val user = currentUser
                                    if (user != null) {
                                        if (navController.currentBackStackEntry?.destination?.route == "dashboard"
                                            && user.parentIds.isNotEmpty()) {
                                            navController.navigate("caregiver_dashboard") {
                                                popUpTo("dashboard") { inclusive = true }
                                            }
                                        } else if (navController.currentBackStackEntry?.destination?.route == "caregiver_dashboard"
                                            && user.isParent) {
                                            navController.navigate("dashboard") {
                                                popUpTo("caregiver_dashboard") { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                viewModel = profileViewModel,
                                showRoleSwitcher = (currentUser?.isParent == true && currentUser?.parentIds?.isNotEmpty() == true)
                            )
                        }
                        composable("edit_profile") {
                            val profileViewModel: ProfileViewModel = viewModel()
                            EditProfileScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = profileViewModel
                            )
                        }

                        composable("emergency_contacts") {
                            val context = LocalContext.current
                            EmergencyContactsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel(
                                    factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
                                )
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun navigateBasedOnRole(navController: NavController, user: User) {
        if (user.isParent && user.parentIds.isNotEmpty()) {
            navController.navigate("role_selection") {
                popUpTo("onboarding") { inclusive = true }
            }
        } else if (user.isParent) {
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        } else {
            navController.navigate("caregiver_dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    private fun setupSensorSyncWorker() {
        if (auth.currentUser != null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SensorSyncWorker>(
                30, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "samsung_health_sensor_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    override fun onResume() {
        super.onResume()
        watchViewModel.refreshWatchData()
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clearCurrentActivity(this)
        samsungHealthSensorManager.cleanup()
    }
}

object ActivityProvider {
    private var currentActivity: ComponentActivity? = null

    fun setCurrentActivity(activity: ComponentActivity) {
        currentActivity = activity
    }

    fun clearCurrentActivity(activity: ComponentActivity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}