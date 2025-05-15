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
import com.ermek.parentwellness.ui.caregiver.CaregiverDashboardScreen
import com.ermek.parentwellness.ui.caregiver.ManageCaregiversScreen
import com.ermek.parentwellness.ui.caregiver.ManageParentsScreen
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
import com.ermek.parentwellness.ui.setup.SetupBirthdayScreen
import com.ermek.parentwellness.ui.setup.SetupGenderScreen
import com.ermek.parentwellness.ui.setup.SetupNameScreen
import com.ermek.parentwellness.ui.setup.SetupViewModel
import com.ermek.parentwellness.ui.setup.SetupWelcomeScreen
import com.ermek.parentwellness.ui.theme.ParentWellnessTheme
import com.ermek.parentwellness.ui.watch.WatchScreen
import com.ermek.parentwellness.ui.watch.WatchViewModel
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
                // Store this token for testing
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
                        } else if (authState is AuthState.NeedsSetup) {
                            currentUser = (authState as AuthState.NeedsSetup).user
                        }
                    }

                    // Determine starting destination based on authentication state
                    val startDestination = when (authState) {
                        is AuthState.Authenticated -> {
                            val user = (authState as AuthState.Authenticated).user
                            if (user.isParent && user.parentIds.isNotEmpty()) {
                                // User has both roles - show role selection
                                "role_selection"
                            } else if (user.isParent) {
                                // User is a parent
                                "dashboard"
                            } else {
                                // User is a caregiver
                                "caregiver_dashboard"
                            }
                        }
                        is AuthState.NeedsSetup -> "setup_welcome"
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
                                onNavigateToLogin = {
                                    navController.navigate("login")
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        // Authentication Screens
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onNavigateToForgotPassword = {
                                    // TODO: Implement forgot password flow
                                },
                                onLoginSuccess = {
                                    when (authState) {
                                        is AuthState.Authenticated -> {
                                            val user = (authState as AuthState.Authenticated).user
                                            navigateBasedOnRole(navController, user)
                                        }
                                        is AuthState.NeedsSetup -> navController.navigate("setup_welcome")
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
                                        is AuthState.NeedsSetup -> navController.navigate("setup_welcome")
                                        else -> {}
                                    }
                                }
                            )
                        }

                        // Setup Flow
                        composable("setup_welcome") {
                            SetupWelcomeScreen(
                                onContinue = {
                                    navController.navigate("setup_name")
                                }
                            )
                        }

                        composable("setup_name") {
                            val setupViewModel = SetupViewModel()
                            SetupNameScreen(
                                viewModel = setupViewModel,
                                onBack = { navController.popBackStack() },
                                onContinue = {
                                    navController.navigate("setup_gender")
                                }
                            )
                        }

                        composable("setup_gender") {
                            val setupViewModel = SetupViewModel()
                            SetupGenderScreen(
                                viewModel = setupViewModel,
                                onBack = { navController.popBackStack() },
                                onContinue = {
                                    navController.navigate("setup_birthday")
                                }
                            )
                        }

                        composable("setup_birthday") {
                            val setupViewModel = SetupViewModel()
                            SetupBirthdayScreen(
                                viewModel = setupViewModel,
                                onBack = { navController.popBackStack() },
                                onContinue = {
                                    setupViewModel.completeSetup()
                                    // After setup, navigate based on the user's role
                                    val user = (authState as? AuthState.NeedsSetup)?.user
                                    if (user != null) {
                                        navigateBasedOnRole(navController, user)
                                    } else {
                                        navController.navigate("dashboard") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
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
                                onNavigateToHeartRate = {
                                    navController.navigate("heart_rate")
                                },
                                onNavigateToBloodPressure = {
                                    navController.navigate("blood_pressure")
                                },
                                onNavigateToBloodSugar = {
                                    navController.navigate("blood_sugar")
                                },
                                onNavigateToStepsTracker = {
                                    navController.navigate("steps_tracker")
                                },
                                onNavigateToReports = {
                                    navController.navigate("reports")
                                },
                                onNavigateToAlerts = {
                                    navController.navigate("alerts")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                },
                                onNavigateToWatch = {
                                    navController.navigate("watch")
                                },
                                onSwitchToCaregiverMode = {
                                    // Only show this option if user has caregiver relationships
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
                            CaregiverDashboardScreen(
                                onNavigateToManageParents = {
                                    navController.navigate("manage_parents")
                                },
                                onNavigateToParentDetail = { parentId ->
                                    // Navigate to detail screen with parentId
                                    // You can implement this later
                                },
                                onNavigateToHeartRate = {
                                    navController.navigate("heart_rate")
                                },
                                onNavigateToBloodPressure = {
                                    navController.navigate("blood_pressure")
                                },
                                onNavigateToBloodSugar = {
                                    navController.navigate("blood_sugar")
                                },
                                onNavigateToStepsTracker = {
                                    navController.navigate("steps_tracker")
                                },
                                onSwitchToParentMode = {
                                    // Only show this option if user is also a parent
                                    if (currentUser?.isParent == true) {
                                        navController.navigate("dashboard") {
                                            popUpTo("caregiver_dashboard") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("manage_parents") {
                            ManageParentsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSelectParent = { parent ->
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("manage_caregivers") {
                            ManageCaregiversScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Health Tracking Screens
                        composable("heart_rate") {
                            val viewModel: HealthDataViewModel = viewModel(
                                factory = factory
                            )
                            HeartRateScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("blood_pressure") {
                            val viewModel: HealthDataViewModel = viewModel(
                                factory = factory
                            )
                            BloodPressureScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("blood_sugar") {
                            val viewModel: HealthDataViewModel = viewModel(
                                factory = factory
                            )
                            BloodSugarScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        composable("steps_tracker") {
                            val viewModel: HealthDataViewModel = viewModel(
                                factory = factory
                            )
                            StepsTrackerScreen(
                                onBack = { navController.popBackStack() },
                                healthDataViewModel = viewModel
                            )
                        }

                        // Watch Screen - Updated to use Samsung Health Sensor integration
                        composable("watch") {
                            WatchScreen(
                                viewModel = watchViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Additional Screens
                        composable("reports") {
                            ReportsScreen(
                                onNavigateToHeartRate = {
                                    navController.navigate("heart_rate")
                                },
                                onNavigateToBloodPressure = {
                                    navController.navigate("blood_pressure")
                                },
                                onNavigateToBloodSugar = {
                                    navController.navigate("blood_sugar")
                                }
                            )
                        }

                        composable("alerts") {
                            AlertsScreen()
                        }

                        // Profile Screens
                        composable("profile") {
                            val profileViewModel = ProfileViewModel()
                            ProfileScreen(
                                onSignOut = {
                                    authViewModel.signOut()
                                    navController.navigate("onboarding") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onEditProfile = {
                                    navController.navigate("edit_profile")
                                },
                                onNavigateToManageCaregivers = {
                                    navController.navigate("manage_caregivers")
                                },
                                onNavigateToEmergencyContacts = {
                                    navController.navigate("emergency_contacts")
                                },
                                onSwitchRole = {
                                    // Switch between parent and caregiver roles if user has both roles
                                    val user = currentUser
                                    if (user != null) {
                                        if (navController.currentBackStackEntry?.destination?.route == "dashboard"
                                            && user.parentIds.isNotEmpty()) {
                                            // Currently in parent mode, switch to caregiver
                                            navController.navigate("caregiver_dashboard") {
                                                popUpTo("dashboard") { inclusive = true }
                                            }
                                        } else if (navController.currentBackStackEntry?.destination?.route == "caregiver_dashboard"
                                            && user.isParent) {
                                            // Currently in caregiver mode, switch to parent
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
                            val profileViewModel = ProfileViewModel()
                            EditProfileScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
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
                    }
                }
            }
        }
    }

    /**
     * Helper function to navigate based on user role
     */
    private fun navigateBasedOnRole(navController: NavController, user: User) {
        if (user.isParent && user.parentIds.isNotEmpty()) {
            // User has both roles - show role selection
            navController.navigate("role_selection") {
                popUpTo("onboarding") { inclusive = true }
            }
        } else if (user.isParent) {
            // User is a parent
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        } else {
            // User is a caregiver
            navController.navigate("caregiver_dashboard") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    /**
     * Set up Sensor sync worker for background processing
     */
    private fun setupSensorSyncWorker() {
        // Only set up the worker if the user is logged in
        if (auth.currentUser != null) {
            // Define work constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create periodic work request - sync every 30 minutes
            val workRequest = PeriodicWorkRequestBuilder<SensorSyncWorker>(
                30, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex period
            )
                .setConstraints(constraints)
                .build()

            // Enqueue the work
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "samsung_health_sensor_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    /**
     * Handle activity resume lifecycle event
     * Refresh Samsung Health data when the activity is resumed
     */
    override fun onResume() {
        super.onResume()
        // Refresh watch data when activity is resumed
        watchViewModel.refreshWatchData()
    }

    /**
     * Clean up resources when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        // Clean up activity reference
        ActivityProvider.clearCurrentActivity(this)

        // Clean up Samsung Health Sensor Manager resources
        samsungHealthSensorManager.cleanup()
    }
}

/**
 * Utility class to hold the current activity reference
 * Used for Samsung Health SDK resolution
 */
object ActivityProvider {
    private var currentActivity: ComponentActivity? = null

    fun setCurrentActivity(activity: ComponentActivity) {
        currentActivity = activity
    }

    fun getCurrentActivity(): ComponentActivity? {
        return currentActivity
    }

    fun clearCurrentActivity(activity: ComponentActivity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}