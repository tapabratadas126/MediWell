package com.mediwell.app.ui.navigation

sealed class Screen(val route: String) {
    // Pre-login
    data object Auth : Screen("auth")

    // Main tabs
    data object Dashboard     : Screen("dashboard")
    data object Prescriptions : Screen("prescriptions")
    data object SafetyReview  : Screen("safety_review")
    data object Reports       : Screen("reports")
    data object Profile       : Screen("profile")

    // Detail screens (not in bottom nav)
    data object MedicationEntry : Screen("medication_entry")
    data object Report          : Screen("report")
    data object MedicineLibrary : Screen("medicine_library")
    data object Alternatives    : Screen("alternatives")
    data object Settings        : Screen("settings")
}
