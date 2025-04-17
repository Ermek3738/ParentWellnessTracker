package com.ermek.parentwellness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ermek.parentwellness.data.samsung.SamsungHealthConnector
import com.ermek.parentwellness.data.worker.HealthSyncWorker
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
import com.ermek.parentwellness.ui.health.BloodPressureScreen
import com.ermek.parentwellness.ui.health.BloodSugarScreen
import com.ermek.parentwellness.ui.health.HeartRateScreen
import com.ermek.parentwellness.ui.health.StepsTrackerScreen
import com.ermek.parentwellness.ui.onboarding.OnboardingScreen
import com.ermek.parentwellness.ui.profile.EditProfileScreen
import com.ermek.parentwellness.ui.profile.ProfileScreen
import com.ermek.parentwellness.ui.profile.ProfileViewModel
import com.ermek.parentwellness.ui.reports.ReportsScreen
import com.ermek.parentwellness.ui.setup.SetupBirthdayScreen
import com.ermek.parentwellness.ui.setup.SetupGenderScreen
import com.ermek.parentwellness.ui.setup.SetupNameScreen
import com.ermek.parentwellness.ui.setup.SetupViewModel
import com.ermek.parentwellness.ui.setup.SetupWelcomeScreen
import com.ermek.parentwellness.ui.theme.ParentWellnessTheme
import com.ermek.parentwellness.ui.watch.WatchScreen
import com.ermek.parentwellness.ui.watch.WatchViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var watchViewModel: WatchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WatchViewModel
        watchViewModel = ViewModelProvider(this)[WatchViewModel::class.java]

        // Set up WorkManager for periodic sync
        setupWatchSyncWorker()
        setupSamsungHealthSync()

        setContent {
            ParentWellnessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel by viewModels()
                    val navController = rememberNavController()
                    val authState by authViewModel.authState.collectAsState()

                    // Determine starting destination based on authentication state
                    val startDestination = when (authState) {
                        is AuthState.Authenticated -> "dashboard"
                        is AuthState.NeedsSetup -> "setup_welcome"
                        else -> "onboarding"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
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
                                        is AuthState.Authenticated -> navController.navigate("dashboard") {
                                            popUpTo("onboarding") { inclusive = true }
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
                                        is AuthState.Authenticated -> navController.navigate("dashboard") {
                                            popUpTo("onboarding") { inclusive = true }
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
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Main App Screens
                        composable("dashboard") {
                            val dashboardViewModel = DashboardViewModel()
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
                            HeartRateScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("blood_pressure") {
                            BloodPressureScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("blood_sugar") {
                            BloodSugarScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("steps_tracker") {
                            StepsTrackerScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Watch Screen - Updated to use Samsung Health integration
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
                                viewModel = profileViewModel
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
                    }
                }
            }
        }
    }

    /**
     * Set up Samsung Health sync using WorkManager for background processing
     */
    private fun setupSamsungHealthSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "samsung_health_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    private fun setupWatchSyncWorker() {
        // Only set up the worker if the user is logged in
        if (auth.currentUser != null) {
            // Define work constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create periodic work request - sync every 15 minutes
            val workRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex period
            )
                .setConstraints(constraints)
                .build()

            // Enqueue the work
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "health_sync_worker",
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
}