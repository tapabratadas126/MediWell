package com.mediwell.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.PatientProfile
import com.mediwell.app.data.repository.MedicationRepository
import com.mediwell.app.data.repository.ProfileRepository
import com.mediwell.app.data.repository.ReportRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val activeMedicationCount: Int = 0,
    val attentionItemCount: Int = 0,
    val recentReportSummary: String? = null,
    val profileComplete: Boolean = false,
    val profile: PatientProfile? = null
)

class DashboardViewModel(
    private val medicationRepository: MedicationRepository,
    private val reportRepository: ReportRepository,
    private val profileRepository: ProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Load profile
            val profileResult = profileRepository.fetchProfile(userId)
            val profile = if (profileResult is AppResult.Success) profileResult.data else null

            // Count active medications from local state
            val medCount = medicationRepository.medications.value.size

            // Check profile completeness
            val profileComplete = profile != null &&
                !profile.fullName.isNullOrBlank() &&
                (!profile.allergies.isEmpty() || !profile.medicalConditions.isEmpty())

            val displayName = profile?.fullName?.ifBlank { null }
                ?: userId.take(8)

            _state.value = DashboardState(
                isLoading              = false,
                displayName            = displayName,
                activeMedicationCount  = medCount,
                attentionItemCount     = 0, // updated after report loads
                recentReportSummary    = null,
                profileComplete        = profileComplete,
                profile                = profile
            )
        }
    }
}
