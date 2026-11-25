package com.mediwell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Structured per-medicine information fetched from official FDA / DailyMed package inserts.
 * Contains purely deterministic label data (not LLM-generated).
 */
@Serializable
data class MedicationLabelInfo(
    val medicineName: String,
    val purpose: String,
    val typicalDosage: String,
    val administrationGuide: String,
    val warningsAndPrecautions: String,
    val ageRestrictions: String = "No specific age restriction noted in summary; check with a doctor or pharmacist for children or older adults.",
    val lastUpdated: String = "Current official label",
    val source: String = "Official Product Label (FDA / DailyMed)"
)
