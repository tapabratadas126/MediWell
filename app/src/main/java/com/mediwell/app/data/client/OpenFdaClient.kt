package com.mediwell.app.data.client

import android.util.Log
import com.mediwell.app.data.model.MedicationLabelInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
 * - Purpose / Indications
 * - Dosage and Administration
 * - Warnings and Precautions
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

    // In-memory cache to prevent redundant HTTP lookups
    private val labelCache = mutableMapOf<String, MedicationLabelInfo>()

    suspend fun fetchMedicationLabel(medicineName: String): MedicationLabelInfo = withContext(Dispatchers.IO) {
        val cleanName = medicineName.trim()
        if (cleanName.isBlank()) {
            return@withContext defaultLabelInfo(medicineName)
        }

        val cacheKey = cleanName.lowercase()
        labelCache[cacheKey]?.let { return@withContext it }

        try {
            // First try generic_name search, then brand_name search
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
        // Remove common dosage forms from search term (e.g., "Metformin 500mg" -> "Metformin")
        val sanitized = drugName.replace(Regex("""(?i)\b(\d+mg|\d+mcg|\d+ml|tab|tablet|cap|capsule|oral)\b"""), "").trim()
        val queryTerm = if (sanitized.isNotBlank()) sanitized else drugName

        val searchQuery = "openfda.generic_name:\"$queryTerm\"+openfda.brand_name:\"$queryTerm\""

        val response: HttpResponse = httpClient.get("https://api.fda.gov/drug/label.json") {
            parameter("search", searchQuery)
            parameter("limit", "1")
        }

        if (response.status != HttpStatusCode.OK) {
            // Fallback: search anywhere in label if specific openfda fields had no match
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
            ?: "Prescription medication indicated according to FDA approved labeling."

        val dosageText = result.dosageAndAdministration?.firstOrNull()
            ?: "Follow the exact dosage regimen directed by your healthcare provider."

        val warningsText = result.warnings?.firstOrNull()
            ?: result.warningsAndCautions?.firstOrNull()
            ?: result.boxedWarning?.firstOrNull()
            ?: "Consult your physician or pharmacist regarding specific contraindications and safety advisories."

        return MedicationLabelInfo(
            medicineName = drugName,
            purpose = cleanText(purposeText, maxLength = 280),
            typicalDosage = cleanText(dosageText, maxLength = 300),
            administrationGuide = extractAdministrationTips(dosageText),
            warningsAndPrecautions = cleanText(warningsText, maxLength = 320),
            source = "openFDA / DailyMed (Official FDA Drug Label)"
        )
    }

    private fun extractAdministrationTips(dosageText: String): String {
        return when {
            dosageText.contains("with food", ignoreCase = true) || dosageText.contains("with meal", ignoreCase = true) ->
                "Take with food or meals to optimize absorption and minimize gastrointestinal upset."
            dosageText.contains("empty stomach", ignoreCase = true) || dosageText.contains("before meals", ignoreCase = true) ->
                "Take on an empty stomach (typically 1 hour before or 2 hours after meals) with a full glass of water."
            dosageText.contains("bedtime", ignoreCase = true) || dosageText.contains("evening", ignoreCase = true) ->
                "Commonly taken in the evening or at bedtime as advised by your physician."
            else ->
                "Take with a full glass of water at consistent times each day as directed on your prescription label."
        }
    }

    private fun cleanText(text: String, maxLength: Int): String {
        val singleLine = text.replace(Regex("""\s+"""), " ").trim()
        // Strip common SPL section headers like "1 INDICATIONS AND USAGE"
        val stripped = singleLine.replace(Regex("""^(\d+\s+[A-Z\s]+:|\bINDICATIONS AND USAGE\b|\bDOSAGE AND ADMINISTRATION\b|\bWARNINGS AND PRECAUTIONS\b)[:\s]*"""), "")
        return if (stripped.length > maxLength) {
            stripped.take(maxLength).substringBeforeLast(" ") + "..."
        } else {
            stripped
        }
    }

    private fun defaultLabelInfo(drugName: String): MedicationLabelInfo {
        return MedicationLabelInfo(
            medicineName = drugName,
            purpose = "Prescription medication prescribed by your physician for therapeutic treatment.",
            typicalDosage = "Follow your prescribing physician's personalized dosage instructions precisely.",
            administrationGuide = "Take orally with a full glass of water at scheduled intervals as directed on your prescription package.",
            warningsAndPrecautions = "Inform your healthcare professional about all current medications, allergies, and existing medical conditions before use.",
            source = "Clinical Reference Standard"
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
    @SerialName("boxed_warning") val boxedWarning: List<String>? = null
)
