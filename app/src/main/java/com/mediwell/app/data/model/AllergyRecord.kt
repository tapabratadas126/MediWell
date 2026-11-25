package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured patient allergy record stored in Supabase `patient_allergies` table.
 */
@Serializable
data class AllergyRecord(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("allergen_name")
    val allergenName: String,
    @SerialName("reaction")
    val reaction: String? = null,
    @SerialName("severity")
    val severity: String = "Not specified", // "Mild", "Moderate", "Severe", "Not specified"
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
