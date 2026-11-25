package com.mediwell.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.mediwell.app.data.auth.AuthRepository
import com.mediwell.app.data.client.GeminiClient
import com.mediwell.app.data.client.RxNormClient
import com.mediwell.app.data.repository.MedicationRepository
import com.mediwell.app.data.repository.ProfileRepository
import com.mediwell.app.data.repository.ReportRepository
import com.mediwell.app.data.repository.SafetyEngineRepository
import com.mediwell.app.ui.screens.auth.AuthScreen
import com.mediwell.app.ui.screens.auth.AuthViewModel
import com.mediwell.app.ui.screens.dashboard.DashboardScreen
import com.mediwell.app.ui.screens.dashboard.DashboardViewModel
import com.mediwell.app.ui.screens.medication.MedicationEntryScreen
import com.mediwell.app.ui.screens.medication.MedicationViewModel
import com.mediwell.app.ui.screens.medicines.MedicineLibraryScreen
import com.mediwell.app.ui.screens.profile.ProfileScreen
import com.mediwell.app.ui.screens.profile.ProfileViewModel
import com.mediwell.app.ui.screens.report.ReportScreen
import com.mediwell.app.ui.screens.report.ReportViewModel
import com.mediwell.app.ui.screens.reports.ReportsListScreen
import com.mediwell.app.ui.theme.*
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard,     "Home",     Icons.Default.Home),
    BottomNavItem(Screen.Prescriptions, "Rx",       Icons.Default.Description),
    BottomNavItem(Screen.MedicineLibrary,"Medicines",Icons.Default.Medication),
    BottomNavItem(Screen.Reports,       "Reports",  Icons.Default.Assessment),
    BottomNavItem(Screen.Profile,       "Profile",  Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val context         = LocalContext.current
    val coroutineScope  = rememberCoroutineScope()

    val authRepository      = remember { AuthRepository(context) }
    val rxNormClient        = remember { RxNormClient() }
    val medicationRepository= remember { MedicationRepository(rxNormClient) }
    val safetyEngine        = remember { SafetyEngineRepository() }
    val geminiClient        = remember { GeminiClient() }
    val reportRepository    = remember { ReportRepository(geminiClient) }
    val profileRepository   = remember { ProfileRepository() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination  = navBackStackEntry?.destination

    // Determine whether bottom bar should be visible
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        containerColor = Ivory,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = White, tonalElevation = 0.dp) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor    = White,
                                selectedTextColor    = ForestGreen,
                                indicatorColor       = ForestGreen,
                                unselectedIconColor  = TextTertiary,
                                unselectedTextColor  = TextTertiary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Auth.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Auth (pre-login, no bottom bar) ──────────────────────────────
            composable(Screen.Auth.route) {
                val vm: AuthViewModel = viewModel { AuthViewModel(authRepository) }
                AuthScreen(viewModel = vm, onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                })
            }

            // ── Dashboard ────────────────────────────────────────────────────
            composable(Screen.Dashboard.route) {
                val userId = authRepository.getCurrentUserId()
                val vm: DashboardViewModel = viewModel {
                    DashboardViewModel(medicationRepository, reportRepository, profileRepository, userId)
                }
                DashboardScreen(
                    viewModel      = vm,
                    onUploadRx     = { navController.navigate(Screen.Prescriptions.route) },
                    onViewProfile  = { navController.navigate(Screen.Profile.route) },
                    onViewReport   = { navController.navigate(Screen.Reports.route) },
                    onSignOut      = {
                        coroutineScope.launch { authRepository.signOut() }
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Prescriptions / Medication entry ─────────────────────────────
            composable(Screen.Prescriptions.route) {
                val userId = authRepository.getCurrentUserId()
                val vm: MedicationViewModel = viewModel {
                    MedicationViewModel(medicationRepository, userId)
                }
                MedicationEntryScreen(
                    viewModel       = vm,
                    onRunSafetyCheck= { navController.navigate(Screen.Report.route) },
                    onSignOut       = {
                        coroutineScope.launch { authRepository.signOut() }
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Legacy route alias kept for existing back-stack entries
            composable(Screen.MedicationEntry.route) {
                val userId = authRepository.getCurrentUserId()
                val vm: MedicationViewModel = viewModel {
                    MedicationViewModel(medicationRepository, userId)
                }
                MedicationEntryScreen(
                    viewModel       = vm,
                    onRunSafetyCheck= { navController.navigate(Screen.Report.route) },
                    onSignOut       = {
                        coroutineScope.launch { authRepository.signOut() }
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Safety Report (detail) ────────────────────────────────────────
            composable(Screen.Report.route) {
                val userId           = authRepository.getCurrentUserId()
                val currentMedications = medicationRepository.medications.value
                val vm: ReportViewModel = viewModel {
                    ReportViewModel(safetyEngine, reportRepository, profileRepository, userId, currentMedications)
                }
                ReportScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            // ── Medicine Library ──────────────────────────────────────────────
            composable(Screen.MedicineLibrary.route) {
                MedicineLibraryScreen(
                    medications = medicationRepository.medications.value,
                    onBack      = { navController.popBackStack() }
                )
            }

            // ── Reports history ───────────────────────────────────────────────
            composable(Screen.Reports.route) {
                val userId = authRepository.getCurrentUserId()
                ReportsListScreen(
                    userId     = userId,
                    reportRepository = reportRepository,
                    onViewReport= { navController.navigate(Screen.Report.route) },
                    onBack      = { navController.popBackStack() }
                )
            }

            // ── Profile ───────────────────────────────────────────────────────
            composable(Screen.Profile.route) {
                val userId = authRepository.getCurrentUserId()
                val vm: ProfileViewModel = viewModel {
                    ProfileViewModel(profileRepository, userId)
                }
                ProfileScreen(viewModel = vm)
            }
        }
    }
}
