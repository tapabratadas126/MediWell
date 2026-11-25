package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientProfile(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val city: String? = null,
    val allergies: List<String> = emptyList(),
    @SerialName("medical_conditions") val medicalConditions: List<String> = emptyList(),
    @SerialName("kidney_conditions") val kidneyConditions: List<String> = emptyList(),
    @SerialName("liver_conditions") val liverConditions: List<String> = emptyList(),
    @SerialName("other_medicines") val otherMedicines: List<String> = emptyList(),
    @SerialName("consent_data_use") val consentDataUse: Boolean = false,
    @SerialName("consent_share_professional") val consentShareProfessional: Boolean = false
)
