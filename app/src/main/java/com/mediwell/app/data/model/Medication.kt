package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `medications` table in Supabase.
 * RLS rule: auth.uid() = user_id
 */
@Serializable
data class MedicationEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("raw_name")
    val rawName: String,
    @SerialName("matched_rxcui")
    val matchedRxcui: String? = null,
    @SerialName("matched_brand_id")
    val matchedBrandId: String? = null,
    @SerialName("dose")
    val dose: String? = null,
    @SerialName("frequency")
    val frequency: String? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("source")
    val source: String = "manual_entry", // "manual_entry", "ocr", "prescription"
    @SerialName("ocr_confidence")
    val ocrConfidence: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Domain model representing a medication entry.
 */
data class MedicationItem(
    val id: String,
    val userId: String,
    val rawName: String,
    val matchedRxcui: String?,
    val matchedBrandId: String?,
    val dose: String,
    val frequency: String,
    val route: String,
    val source: String,
    val normalizedIngredient: String? = null,
    val isVerified: Boolean = false
) {
    val displaySubtitle: String
        get() = listOfNotNull(
            dose.takeIf { it.isNotBlank() },
            frequency.takeIf { it.isNotBlank() },
            route.takeIf { it.isNotBlank() }
        ).joinToString(" • ").ifBlank { "No dose/schedule details" }
}

/**
 * Manual medication-entry form input.
 */
data class MedicationInputForm(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "Once daily",
    val route: String = "Oral",
    val source: String = "manual_entry"
) {
    val isValid: Boolean
        get() = name.trim().length >= 2
}
