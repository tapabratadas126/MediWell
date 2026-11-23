package com.mediwell.app.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object MedicationEntry : Screen("medication_entry")
    data object Report : Screen("report")
}
