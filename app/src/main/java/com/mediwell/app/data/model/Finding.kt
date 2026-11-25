package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `findings` table in Supabase.
 * RLS: auth.uid() = user_id
 */
@Serializable
data class FindingEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("medication_id_1")
    val medicationId1: String,
    @SerialName("medication_id_2")
    val medicationId2: String? = null,
    @SerialName("type")
    val type: String, // "duplicate_ingredient", "drug_interaction", "qt_risk"
    @SerialName("severity")
    val severity: String, // "critical", "warning", "info"
    @SerialName("source")
    val source: String, // "interaction_curated", "interaction_dataset", "brand_ingredient_map", "qt_risk_drugs"
    @SerialName("confidence")
    val confidence: String, // "verified", "needs_review"
    @SerialName("created_at")
    val createdAt: String? = null
)

enum class FindingType(val dbValue: String, val label: String) {
    DUPLICATE_INGREDIENT("duplicate_ingredient", "Duplicate Ingredient"),
    DRUG_INTERACTION("drug_interaction", "Drug Interaction"),
    QT_RISK("qt_risk", "QT Prolongation Risk"),
    ALLERGY_MATCH("allergy_match", "Allergy Risk"),
    UNKNOWN("unknown", "Safety Notice");

    companion object {
        fun from(value: String): FindingType = entries.find { it.dbValue.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class FindingSeverity(val dbValue: String, val label: String) {
    CRITICAL("critical", "High Risk / Critical"),
    WARNING("warning", "Moderate / Warning"),
    INFO("info", "Minor / Informational");

    companion object {
        fun from(value: String): FindingSeverity = entries.find { it.dbValue.equals(value, ignoreCase = true) } ?: WARNING
    }
}

enum class ConfidenceLevel(val dbValue: String, val label: String) {
    VERIFIED("verified", "Verified"),
    NEEDS_REVIEW("needs_review", "Needs Review");

    companion object {
        fun from(value: String): ConfidenceLevel = entries.find { it.dbValue.equals(value, ignoreCase = true) } ?: NEEDS_REVIEW
    }
}

/**
 * Domain representation of a safety finding.
 */
@Serializable
data class SafetyFinding(
    val findingKey: String,
    val drugName1: String,
    val drugName2: String? = null,
    val rxcui1: String? = null,
    val rxcui2: String? = null,
    val type: FindingType,
    val severity: FindingSeverity,
    val mechanismOrDetail: String,
    val sourceCitation: String,
    val sourceTable: String,
    val confidence: ConfidenceLevel,
    val plainLanguageSentence: String = "",
    val rephrasedBy: String = "deterministic_template" // "cache", "gemini", "deterministic_template"
)
