package com.mediwell.app.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.ClinicianViewReport
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.model.PatientViewReport
import com.mediwell.app.data.model.SafetyFinding
import com.mediwell.app.data.repository.ProfileRepository
import com.mediwell.app.data.repository.ReportRepository
import com.mediwell.app.data.repository.SafetyEngineRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportUiState {
    data object Loading : ReportUiState()
    data class Ready(
        val patientView: PatientViewReport,
        val clinicianView: ClinicianViewReport,
        val rawFindings: List<SafetyFinding>
    ) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ReportViewModel(
    private val safetyEngineRepository: SafetyEngineRepository,
    private val reportRepository: ReportRepository,
    private val profileRepository: ProfileRepository,
    private val userId: String,
    private val medications: List<MedicationItem>
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _selectedViewTab = MutableStateFlow(0) // 0: Patient View, 1: Clinician View
    val selectedViewTab: StateFlow<Int> = _selectedViewTab.asStateFlow()

    init {
        runAnalysisAndGenerateReport()
    }

    fun selectTab(index: Int) {
        _selectedViewTab.value = index
    }

    fun runAnalysisAndGenerateReport() {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            try {
                // Step 1: Fetch saved allergies from the backend so they actively inform the review
                val allergyResult = profileRepository.fetchAllergies(userId)
                val allergies = (allergyResult as? AppResult.Success)?.data ?: emptyList()

                // Step 2: Run deterministic safety analysis (interactions, duplicates, allergies)
                val findings = safetyEngineRepository.runSafetyAnalysis(userId, medications, allergies)

                // Step 3: Generate structured dual-view report
                when (val reportResult = reportRepository.generateSafetyReport(userId, medications, findings, allergies)) {
                    is AppResult.Success -> {
                        val (patientView, clinicianView) = reportResult.data
                        _uiState.value = ReportUiState.Ready(
                            patientView = patientView,
                            clinicianView = clinicianView,
                            rawFindings = findings
                        )
                    }
                    is AppResult.Error -> {
                        _uiState.value = ReportUiState.Error(reportResult.message)
                    }
                    is AppResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Failed to run safety analysis: ${e.message}")
            }
        }
    }
}
