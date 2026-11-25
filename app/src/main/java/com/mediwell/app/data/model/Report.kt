package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `reports` table in Supabase.
 * RLS: auth.uid() = user_id
 */
@Serializable
data class ReportEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("generated_at")
    val generatedAt: String? = null,
    @SerialName("patient_view_json")
    val patientViewJson: String,
    @SerialName("clinician_view_json")
    val clinicianViewJson: String
)

@Serializable
data class PatientViewReport(
    val title: String = "Medication Safety Report",
    val generatedAt: String,
    val totalMedicationsChecked: Int,
    val totalFindings: Int,
    val allergiesAssessedCount: Int = 0,
    val allergyAssessmentNote: String = "Not assessed — no allergies recorded in your profile.",
    val findings: List<PatientFindingCard>,
    val medicationDetails: List<MedicationLabelInfo> = emptyList(),
    val fixedReferralNotice: String
)

@Serializable
data class PatientFindingCard(
    val findingKey: String,
    val title: String,
    val issue: String = "",
    val plainLanguageExplanation: String,
    val whatToDo: String = "Discuss this finding with your doctor or pharmacist before making any changes.",
    val confidenceBadge: String, // "Verified" or "Needs Confirmation"
    val severityBadge: String,   // "High Risk", "Warning", "Informational"
    val sourceLabel: String = "Reference information found",
    val additionalDetails: String = "",
    val involvedMedications: List<String>
)

@Serializable
data class ClinicianViewReport(
    val generatedAt: String,
    val medications: List<ClinicianMedicationItem>,
    val findings: List<ClinicianFindingItem>,
    val recordedAllergies: List<AllergyRecord> = emptyList(),
    val medicationDetails: List<MedicationLabelInfo> = emptyList(),
    val disclaimer: String
)

@Serializable
data class ClinicianMedicationItem(
    val name: String,
    val rxcui: String?,
    val activeIngredients: String?,
    val dose: String?,
    val frequency: String?,
    val route: String?
)

@Serializable
data class ClinicianFindingItem(
    val drugA: String,
    val drugB: String?,
    val type: String,
    val severity: String,
    val mechanism: String,
    val sourceCitation: String,
    val confidence: String
)
