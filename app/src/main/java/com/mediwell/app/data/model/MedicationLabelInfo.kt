package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured per-medicine information fetched from official FDA / DailyMed package inserts.
 * Contains purely deterministic label data (not LLM hallucinations).
 */
@Serializable
data class MedicationLabelInfo(
    val medicineName: String,
    val purpose: String,
    val typicalDosage: String,
    val administrationGuide: String,
    val warningsAndPrecautions: String,
    val source: String = "openFDA / DailyMed Structured Product Label (SPL)"
)
