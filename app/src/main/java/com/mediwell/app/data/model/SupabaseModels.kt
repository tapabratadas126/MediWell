package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `rxnorm_cache` reference table.
 * Public read-only.
 */
@Serializable
data class RxNormCacheEntity(
    @SerialName("normalized_name")
    val normalizedName: String,
    @SerialName("rxcui")
    val rxcui: String,
    @SerialName("last_synced")
    val lastSynced: String? = null
)

/**
 * Maps to `interaction_dataset` reference table (222,391 rows from DDInter).
 * Columns: id, ddinter_id_a, drug_a, ddinter_id_b, drug_b, level, source
 */
@Serializable
data class InteractionDatasetEntity(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("ddinter_id_a")
    val ddinterIdA: String? = null,
    @SerialName("drug_a")
    val drugA: String,
    @SerialName("ddinter_id_b")
    val ddinterIdB: String? = null,
    @SerialName("drug_b")
    val drugB: String,
    @SerialName("level")
    val level: String? = null, // e.g. "Major", "Moderate", "Minor"
    @SerialName("source")
    val source: String? = null
)

/**
 * Maps to `interaction_curated` reference table (Cardio/CNS/Antiinfective curated).
 * Columns: id, drug_a, drug_b, severity, mechanism, source_citation, source_url, added_by, created_at
 */
@Serializable
data class InteractionCuratedEntity(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("drug_a")
    val drugA: String,
    @SerialName("drug_b")
    val drugB: String,
    @SerialName("severity")
    val severity: String, // "critical", "warning", "info"
    @SerialName("mechanism")
    val mechanism: String? = null,
    @SerialName("source_citation")
    val sourceCitation: String? = null,
    @SerialName("source_url")
    val sourceUrl: String? = null,
    @SerialName("added_by")
    val addedBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Maps to `brand_ingredient_map` reference table (253,974 Indian pharmaceutical products).
 * Columns: product_id PK, brand_name, manufacturer, dosage_form, pack_size, pack_unit,
 *          num_active_ingredients, primary_ingredient, primary_strength,
 *          active_ingredients, therapeutic_class, price_inr
 */
@Serializable
data class BrandIngredientMapEntity(
    @SerialName("product_id")
    val productId: String,
    @SerialName("brand_name")
    val brandName: String,
    @SerialName("manufacturer")
    val manufacturer: String? = null,
    @SerialName("dosage_form")
    val dosageForm: String? = null,
    @SerialName("pack_size")
    val packSize: String? = null,
    @SerialName("pack_unit")
    val packUnit: String? = null,
    @SerialName("num_active_ingredients")
    val numActiveIngredients: Int? = null,
    @SerialName("primary_ingredient")
    val primaryIngredient: String? = null,
    @SerialName("primary_strength")
    val primaryStrength: String? = null,
    @SerialName("active_ingredients")
    val activeIngredients: String? = null,
    @SerialName("therapeutic_class")
    val therapeuticClass: String? = null,
    @SerialName("price_inr")
    val priceInr: Double? = null
)

/**
 * Maps to `qt_risk_drugs` reference table.
 * Columns: drug_name PK, risk_category
 */
@Serializable
data class QtRiskDrugEntity(
    @SerialName("drug_name")
    val drugName: String,
    @SerialName("risk_category")
    val riskCategory: String
)

/**
 * Maps to `cache_findings` table.
 * Columns: finding_key PK, sentence, generated_by, updated_at
 */
@Serializable
data class CacheFindingsEntity(
    @SerialName("finding_key")
    val findingKey: String,
    @SerialName("sentence")
    val sentence: String,
    @SerialName("generated_by")
    val generatedBy: String = "gemini",
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Maps to `prescriptions` or `scanned_prescriptions` table in Supabase.
 * Stores raw OCR scanning history and parsed medications.
 */
@Serializable
data class ScannedPrescriptionEntity(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("raw_ocr_text")
    val rawOcrText: String,
    @SerialName("detected_medication_name")
    val detectedMedicationName: String? = null,
    @SerialName("detected_dose")
    val detectedDose: String? = null,
    @SerialName("detected_frequency")
    val detectedFrequency: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

