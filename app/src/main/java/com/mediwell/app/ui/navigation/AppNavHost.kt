package com.mediwell.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mediwell.app.data.auth.AuthRepository
import com.mediwell.app.data.client.GeminiClient
import com.mediwell.app.data.client.RxNormClient
import com.mediwell.app.data.repository.MedicationRepository
import com.mediwell.app.data.repository.ReportRepository
import com.mediwell.app.data.repository.SafetyEngineRepository
import com.mediwell.app.ui.screens.auth.AuthScreen
import com.mediwell.app.ui.screens.auth.AuthViewModel
import com.mediwell.app.ui.screens.medication.MedicationEntryScreen
import com.mediwell.app.ui.screens.medication.MedicationViewModel
import com.mediwell.app.ui.screens.report.ReportScreen
import com.mediwell.app.ui.screens.report.ReportViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Repositories
    val authRepository = remember { AuthRepository(context) }
    val rxNormClient = remember { RxNormClient() }
    val medicationRepository = remember { MedicationRepository(rxNormClient) }
    val safetyEngineRepository = remember { SafetyEngineRepository() }
    val geminiClient = remember { GeminiClient() }
    val reportRepository = remember { ReportRepository(geminiClient) }

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(authRepository)
            }
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.MedicationEntry.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MedicationEntry.route) {
            val userId = authRepository.getCurrentUserId()
            val medicationViewModel: MedicationViewModel = viewModel {
                MedicationViewModel(medicationRepository, userId)
            }
            MedicationEntryScreen(
                viewModel = medicationViewModel,
                onRunSafetyCheck = {
                    navController.navigate(Screen.Report.route)
                },
                onSignOut = {
                    authRepository.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Report.route) {
            val userId = authRepository.getCurrentUserId()
            val currentMedications = medicationRepository.medications.value
            val reportViewModel: ReportViewModel = viewModel {
                ReportViewModel(
                    safetyEngineRepository = safetyEngineRepository,
                    reportRepository = reportRepository,
                    userId = userId,
                    medications = currentMedications
                )
            }
            ReportScreen(
                viewModel = reportViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
