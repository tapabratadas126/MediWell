package com.mediwell.app.data.client

import android.util.Log
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.data.model.FindingType
import com.mediwell.app.data.model.SafetyFinding
import com.mediwell.app.util.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Google AI Studio (Gemini API) Client.
 *
 * CRITICAL ARCHITECTURAL GUARDRAIL:
 * - LLM is used EXCLUSIVELY for rephrasing structured clinical findings into plain language.
 * - The LLM NEVER decides clinical severity, duplicate status, or interaction risk.
 * - The LLM receives EXACTLY ONE finding at a time.
 * - The LLM is instructed: "restate this exact information in plain language; do not add any
 *   fact, drug name, or severity not given here."
 * - Fallback to deterministic template is guaranteed if offline or API key is absent.
 */
class GeminiClient {
    private val tag = "GeminiClient"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@GeminiClient.json)
        }
    }

    @Serializable
    private data class GeminiRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig = GenerationConfig()
    )

    @Serializable
    private data class Content(
        val parts: List<Part>
    )

    @Serializable
    private data class Part(
        val text: String
    )

    @Serializable
    private data class GenerationConfig(
        val temperature: Double = 0.2, // Low temperature for high factual precision
        val maxOutputTokens: Int = 120
    )

    @Serializable
    private data class GeminiResponse(
        val candidates: List<Candidate>? = null
    )

    @Serializable
    private data class Candidate(
        val content: Content? = null
    )

    /**
     * Rephrase a single structured finding into plain patient language.
     * Guaranteed never to throw; falls back to deterministic template on error.
     */
    suspend fun rephraseFinding(finding: SafetyFinding): String = withContext(Dispatchers.IO) {
        if (!Config.isGeminiConfigured) {
            Log.d(tag, "Gemini API key not configured. Using deterministic template.")
            return@withContext generateDeterministicSentence(finding)
        }

        try {
            val prompt = buildConstrainedPrompt(finding)
            val endpoint = "${Config.GEMINI_BASE_URL}/models/${Config.GEMINI_MODEL}:generateContent?key=${Config.geminiApiKey}"

            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                )
            )

            val response = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseBody = response.body<GeminiResponse>()
            val generatedText = responseBody.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()

            if (!generatedText.isNullOrBlank()) {
                // Sanity check: Ensure no forbidden phrases (e.g. suggesting dose changes)
                val sanitized = sanitizeModelOutput(generatedText, finding)
                return@withContext sanitized
            }
        } catch (e: Exception) {
            Log.w(tag, "Gemini rephrase call failed: ${e.message}. Falling back to template.")
        }

        generateDeterministicSentence(finding)
    }

    /**
     * Build the strictly constrained prompt with hard guardrails.
     */
    private fun buildConstrainedPrompt(finding: SafetyFinding): String {
        val drugBClause = if (!finding.drugName2.isNullOrBlank()) " and ${finding.drugName2}" else ""
        return """
            Role: Medical plain-language translator for patients.
            Instruction: Restate this exact clinical finding in 1 or 2 simple, calm, plain-language sentences for a patient.
            Hard Guardrails (Non-Negotiable):
            1. Do NOT add any new fact, new medication name, or severity level not provided below.
            2. Do NOT diagnose or suggest changing, stopping, or starting a medication dose.
            3. Restate ONLY the clinical information given in the finding.

            Structured Finding:
            - Involved Medication(s): ${finding.drugName1}$drugBClause
            - Finding Type: ${finding.type.label}
            - Severity Level: ${finding.severity.label}
            - Clinical Details / Mechanism: ${finding.mechanismOrDetail}
            - Source: ${finding.sourceCitation}

            Plain language sentence:
        """.trimIndent()
    }

    /**
     * Guardrail: Strip any dangerous hallucinated recommendations (e.g. "stop taking", "reduce dose").
     */
    private fun sanitizeModelOutput(text: String, finding: SafetyFinding): String {
        val lower = text.lowercase()
        val dangerousPhrases = listOf(
            "stop taking", "discontinue", "cut dose", "reduce your dose",
            "increase your dose", "double your dose", "skip your dose"
        )
        for (phrase in dangerousPhrases) {
            if (lower.contains(phrase)) {
                Log.w(tag, "Model output contained unauthorized dose/stop advice ('$phrase'). Discarding and using template.")
                return generateDeterministicSentence(finding)
            }
        }
        return text.replace("\"", "").trim()
    }

    /**
     * Safe deterministic template used on cache miss, offline, or API error.
     */
    fun generateDeterministicSentence(finding: SafetyFinding): String {
        return when (finding.type) {
            FindingType.DUPLICATE_INGREDIENT -> {
                if (finding.drugName2 != null) {
                    "${finding.drugName1} and ${finding.drugName2} both contain the same active medicine (${finding.mechanismOrDetail}). Taking both together may cause an accidental overdose."
                } else {
                    "${finding.drugName1} appears more than once in your active medication list. Taking duplicate doses may increase the risk of side effects."
                }
            }
            FindingType.DRUG_INTERACTION -> {
                val drug2 = finding.drugName2 ?: "another medication"
                when (finding.severity) {
                    FindingSeverity.CRITICAL ->
                        "${finding.drugName1} has a significant interaction with $drug2. ${finding.mechanismOrDetail}"
                    FindingSeverity.WARNING ->
                        "Using ${finding.drugName1} alongside $drug2 may interact. ${finding.mechanismOrDetail}"
                    FindingSeverity.INFO ->
                        "There is a noted minor interaction between ${finding.drugName1} and $drug2: ${finding.mechanismOrDetail}"
                }
            }
            FindingType.QT_RISK -> {
                "${finding.drugName1} and ${finding.drugName2 ?: "your other medication"} are known to affect heart rhythm (QT interval). Using them together requires medical monitoring."
            }
            FindingType.UNKNOWN -> {
                "A potential safety notice was identified for ${finding.drugName1}: ${finding.mechanismOrDetail}"
            }
        }
    }
}
