package com.mediwell.app.data.client

import android.util.Log
import com.mediwell.app.data.model.MedicationLabelInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Client for the free, public openFDA Drug Label API (SPL - Structured Product Labeling / DailyMed data).
 * Returns official FDA label sections for each medication:
 * - General uses
 * - Official label instructions
 * - Precautions and warnings
 * - Age-related restrictions
 * - Source and update information
 */
class OpenFdaClient {
    private val tag = "OpenFdaClient"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@OpenFdaClient.json)
        }
    }

    private val labelCache = mutableMapOf<String, MedicationLabelInfo>()

    suspend fun fetchMedicationLabel(medicineName: String): MedicationLabelInfo = withContext(Dispatchers.IO) {
        val cleanName = medicineName.trim()
        if (cleanName.isBlank()) {
            return@withContext defaultLabelInfo(medicineName)
        }

        val cacheKey = cleanName.lowercase()
        labelCache[cacheKey]?.let { return@withContext it }

        try {
            val label = queryOpenFda(cleanName)
            if (label != null) {
                labelCache[cacheKey] = label
                return@withContext label
            }
        } catch (e: Throwable) {
            Log.w(tag, "Failed to query openFDA for '$cleanName': ${e.message}")
        }

        val fallback = defaultLabelInfo(cleanName)
        labelCache[cacheKey] = fallback
        fallback
    }

    private suspend fun queryOpenFda(drugName: String): MedicationLabelInfo? {
        val sanitized = drugName.replace(Regex("""(?i)\b(\d+mg|\d+mcg|\d+ml|tab|tablet|cap|capsule|oral)\b"""), "").trim()
        val queryTerm = if (sanitized.isNotBlank()) sanitized else drugName

        val searchQuery = "openfda.generic_name:\"$queryTerm\"+openfda.brand_name:\"$queryTerm\""

        val response: HttpResponse = httpClient.get("https://api.fda.gov/drug/label.json") {
            parameter("search", searchQuery)
            parameter("limit", "1")
        }

        if (response.status != HttpStatusCode.OK) {
            val fallbackResponse: HttpResponse = httpClient.get("https://api.fda.gov/drug/label.json") {
                parameter("search", "\"$queryTerm\"")
                parameter("limit", "1")
            }
            if (fallbackResponse.status == HttpStatusCode.OK) {
                val parsed = json.decodeFromString<OpenFdaResponse>(fallbackResponse.bodyAsText())
                val result = parsed.results?.firstOrNull()
                if (result != null) return buildLabelInfo(drugName, result)
            }
            return null
        }

        val parsed = json.decodeFromString<OpenFdaResponse>(response.bodyAsText())
        val result = parsed.results?.firstOrNull() ?: return null
        return buildLabelInfo(drugName, result)
    }

    private fun buildLabelInfo(drugName: String, result: OpenFdaLabelResult): MedicationLabelInfo {
        val purposeText = result.purpose?.firstOrNull()
            ?: result.indicationsAndUsage?.firstOrNull()
            ?: "Used as directed by a doctor for its approved medical purpose."

        val dosageText = result.dosageAndAdministration?.firstOrNull()
            ?: "Follow the exact dose and schedule prescribed by your doctor."

        val warningsText = result.boxedWarning?.firstOrNull()
            ?: result.warningsAndCautions?.firstOrNull()
            ?: result.warnings?.firstOrNull()
            ?: "Check with your doctor or pharmacist about possible side effects and precautions."

        val ageText = buildAgeRestrictionText(result.pediatricUse?.firstOrNull(), result.geriatricUse?.firstOrNull())

        val formattedDate = result.effectiveTime?.let { raw ->
            if (raw.length == 8) "${raw.substring(0, 4)}-${raw.substring(4, 6)}-${raw.substring(6, 8)}" else raw
        } ?: "Official product label"

        return MedicationLabelInfo(
            medicineName = drugName,
            purpose = cleanText(purposeText, maxLength = 240),
            typicalDosage = cleanText(dosageText, maxLength = 260),
            administrationGuide = extractAdministrationTips(dosageText),
            warningsAndPrecautions = cleanText(warningsText, maxLength = 280),
            ageRestrictions = ageText,
            lastUpdated = "Label effective: $formattedDate",
            source = "Official Product Label (FDA / DailyMed)"
        )
    }

    private fun buildAgeRestrictionText(pediatric: String?, geriatric: String?): String {
        val parts = mutableListOf<String>()
        if (!pediatric.isNullOrBlank()) {
            parts.add("Children: " + cleanText(pediatric, maxLength = 150))
        }
        if (!geriatric.isNullOrBlank()) {
            parts.add("Older adults: " + cleanText(geriatric, maxLength = 150))
        }
        return if (parts.isEmpty()) {
            "No specific age restriction highlighted in this summary; confirm suitability for children or older adults with a doctor or pharmacist."
        } else {
            parts.joinToString("\n")
        }
    }

    private fun extractAdministrationTips(dosageText: String): String {
        return when {
            dosageText.contains("with food", ignoreCase = true) || dosageText.contains("with meal", ignoreCase = true) ->
                "Take with food or a meal to help absorption and reduce stomach upset."
            dosageText.contains("empty stomach", ignoreCase = true) || dosageText.contains("before meals", ignoreCase = true) ->
                "Take on an empty stomach (about 1 hour before or 2 hours after eating) with water."
            dosageText.contains("bedtime", ignoreCase = true) || dosageText.contains("evening", ignoreCase = true) ->
                "Often taken in the evening or at bedtime, as directed by your doctor."
            else ->
                "Take with a full glass of water at consistent times each day, following your prescription."
        }
    }

    private fun cleanText(text: String, maxLength: Int): String {
        val singleLine = text.replace(Regex("""\s+"""), " ").trim()
        val stripped = singleLine.replace(
            Regex("""^(\d+\s+[A-Z\s]+:|\bINDICATIONS AND USAGE\b|\bDOSAGE AND ADMINISTRATION\b|\bWARNINGS AND PRECAUTIONS\b|\bPEDIATRIC USE\b|\bGERIATRIC USE\b)[:\s]*"""),
            ""
        ).trim()
        return if (stripped.length > maxLength) {
            stripped.take(maxLength).substringBeforeLast(" ") + "..."
        } else {
            stripped
        }
    }

    private fun defaultLabelInfo(drugName: String): MedicationLabelInfo {
        return MedicationLabelInfo(
            medicineName = drugName,
            purpose = "Prescribed for treatment as directed by your doctor.",
            typicalDosage = "Follow your personal prescription instructions carefully.",
            administrationGuide = "Take with a full glass of water at your scheduled times.",
            warningsAndPrecautions = "Tell your doctor or pharmacist about all other medicines, allergies, and health conditions before use.",
            ageRestrictions = "Confirm suitability for children or older adults with your doctor or pharmacist.",
            lastUpdated = "Standard clinical reference",
            source = "General Medication Reference"
        )
    }
}

@Serializable
private data class OpenFdaResponse(
    val results: List<OpenFdaLabelResult>? = null
)

@Serializable
private data class OpenFdaLabelResult(
    @SerialName("purpose") val purpose: List<String>? = null,
    @SerialName("indications_and_usage") val indicationsAndUsage: List<String>? = null,
    @SerialName("dosage_and_administration") val dosageAndAdministration: List<String>? = null,
    @SerialName("warnings") val warnings: List<String>? = null,
    @SerialName("warnings_and_cautions") val warningsAndCautions: List<String>? = null,
    @SerialName("boxed_warning") val boxedWarning: List<String>? = null,
    @SerialName("pediatric_use") val pediatricUse: List<String>? = null,
    @SerialName("geriatric_use") val geriatricUse: List<String>? = null,
    @SerialName("effective_time") val effectiveTime: String? = null
)
