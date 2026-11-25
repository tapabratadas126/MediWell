package com.mediwell.app.data.repository

import android.util.Log
import com.mediwell.app.data.client.GeminiClient
import com.mediwell.app.data.client.OpenFdaClient
import com.mediwell.app.data.client.SupabaseClientProvider
import com.mediwell.app.data.model.AllergyRecord
import com.mediwell.app.data.model.CacheFindingsEntity
import com.mediwell.app.data.model.ClinicianFindingItem
import com.mediwell.app.data.model.ClinicianMedicationItem
import com.mediwell.app.data.model.ClinicianViewReport
import com.mediwell.app.data.model.ConfidenceLevel
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.data.model.FindingType
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.model.PatientFindingCard
import com.mediwell.app.data.model.PatientViewReport
import com.mediwell.app.data.model.ReportEntity
import com.mediwell.app.data.model.SafetyFinding
import com.mediwell.app.util.AppResult
import com.mediwell.app.util.Config
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReportRepository(
    private val geminiClient: GeminiClient = GeminiClient(),
    private val openFdaClient: OpenFdaClient = OpenFdaClient()
) {
    private val tag = "ReportRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val localFindingSentenceCache = mutableMapOf<String, String>()

    suspend fun generateSafetyReport(
        userId: String,
        medications: List<MedicationItem>,
        rawFindings: List<SafetyFinding>,
        allergies: List<AllergyRecord> = emptyList()
    ): AppResult<Pair<PatientViewReport, ClinicianViewReport>> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val timestamp = dateFormat.format(Date())

            val processedFindings = rawFindings.map { finding ->
                val findingKey = computeFindingKey(finding)
                val (sentence, rephrasedBy) = getOrRephraseSentence(finding, findingKey)

                finding.copy(
                    findingKey = findingKey,
                    plainLanguageSentence = sentence,
                    rephrasedBy = rephrasedBy
                )
            }

            // Assemble structured, readable Patient View cards (no technical buzzwords)
            val patientFindingCards = processedFindings.map { finding ->
                val involved = when (finding.type) {
                    FindingType.ALLERGY_MATCH -> listOf(finding.drugName1)
                    else -> listOfNotNull(finding.drugName1, finding.drugName2)
                }

                val issueTitle = when (finding.type) {
                    FindingType.ALLERGY_MATCH ->
                        "Possible allergy match (${finding.drugName2 ?: "recorded allergy"})"
                    FindingType.DUPLICATE_INGREDIENT ->
                        "Duplicate active ingredient"
                    FindingType.DRUG_INTERACTION ->
                        "Medicine interaction detected"
                    FindingType.QT_RISK ->
                        "Heart rhythm precaution"
                    FindingType.UNKNOWN ->
                        "Medication safety notice"
                }

                val severityLabel = when (finding.severity) {
                    FindingSeverity.CRITICAL -> "High Risk"
                    FindingSeverity.WARNING -> "Warning"
                    FindingSeverity.INFO -> "Informational"
                }

                val statusLabel = when (finding.confidence) {
                    ConfidenceLevel.VERIFIED -> "Verified"
                    ConfidenceLevel.NEEDS_REVIEW -> "Needs Confirmation"
                }

                val sourceLabel = when (finding.type) {
                    FindingType.ALLERGY_MATCH -> "Your saved allergy profile"
                    else -> "Reference information found"
                }

                val whatToDo = when (finding.type) {
                    FindingType.ALLERGY_MATCH ->
                        "Do not take this medicine until you confirm it with your doctor or pharmacist."
                    else ->
                        "Discuss this finding with your doctor or pharmacist before changing any medicine."
                }

                PatientFindingCard(
                    findingKey = finding.findingKey,
                    title = issueTitle,
                    issue = issueTitle,
                    plainLanguageExplanation = finding.plainLanguageSentence,
                    whatToDo = whatToDo,
                    confidenceBadge = statusLabel,
                    severityBadge = severityLabel,
                    sourceLabel = sourceLabel,
                    additionalDetails = finding.mechanismOrDetail,
                    involvedMedications = involved
                )
            }

            val allergyMatchesCount = processedFindings.count { it.type == FindingType.ALLERGY_MATCH }
            val allergyNote = when {
                allergies.isEmpty() ->
                    "Not assessed — no allergies recorded in your profile."
                allergyMatchesCount > 0 ->
                    "Checked against ${allergies.size} recorded allergy/allergies — $allergyMatchesCount potential match(es) found."
                else ->
                    "Checked against ${allergies.size} recorded allergy/allergies — no relevant match found."
            }

            val medicationLabelDetails = medications.map { med ->
                openFdaClient.fetchMedicationLabel(med.normalizedIngredient ?: med.rawName)
            }

            val patientView = PatientViewReport(
                title = "Medication Safety Summary",
                generatedAt = timestamp,
                totalMedicationsChecked = medications.size,
                totalFindings = processedFindings.size,
                allergiesAssessedCount = allergies.size,
                allergyAssessmentNote = allergyNote,
                findings = patientFindingCards,
                medicationDetails = medicationLabelDetails,
                fixedReferralNotice = Config.FIXED_REFERRAL_DISCLAIMER
            )

            // Clinician View preserves RxCUIs, formal mechanisms, and technical citations
            val clinicianMeds = medications.map { med ->
                ClinicianMedicationItem(
                    name = med.rawName,
                    rxcui = med.matchedRxcui,
                    activeIngredients = med.normalizedIngredient,
                    dose = med.dose,
                    frequency = med.frequency,
                    route = med.route
                )
            }

            val clinicianFindings = processedFindings.map { f ->
                ClinicianFindingItem(
                    drugA = f.drugName1,
                    drugB = f.drugName2,
                    type = f.type.dbValue,
                    severity = f.severity.dbValue,
                    mechanism = f.mechanismOrDetail,
                    sourceCitation = f.sourceCitation,
                    confidence = f.confidence.dbValue
                )
            }

            val clinicianView = ClinicianViewReport(
                generatedAt = timestamp,
                medications = clinicianMeds,
                findings = clinicianFindings,
                recordedAllergies = allergies,
                medicationDetails = medicationLabelDetails,
                disclaimer = Config.FIXED_REFERRAL_DISCLAIMER
            )

            if (Config.isSupabaseConfigured) {
                try {
                    val reportEntity = ReportEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        generatedAt = timestamp,
                        patientViewJson = json.encodeToString(patientView),
                        clinicianViewJson = json.encodeToString(clinicianView)
                    )
                    SupabaseClientProvider.postgrest.from("reports").insert(reportEntity)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to persist report to Supabase: ${e.message}")
                }
            }

            AppResult.Success(Pair(patientView, clinicianView))
        } catch (e: Exception) {
            Log.e(tag, "Report generation failed: ${e.message}", e)
            AppResult.Error("Failed to generate report: ${e.message}", e)
        }
    }

    private fun computeFindingKey(finding: SafetyFinding): String {
        val d1 = finding.drugName1.lowercase().replace(Regex("[^a-z0-9]"), "")
        val d2 = (finding.drugName2 ?: "none").lowercase().replace(Regex("[^a-z0-9]"), "")
        val drugs = listOf(d1, d2).sorted().joinToString("_")
        return "fnd_${finding.type.dbValue}_${finding.severity.dbValue}_$drugs"
    }

    private suspend fun getOrRephraseSentence(
        finding: SafetyFinding,
        findingKey: String
    ): Pair<String, String> {
        localFindingSentenceCache[findingKey]?.let {
            return Pair(it, "cache")
        }

        if (Config.isSupabaseConfigured) {
            try {
                val cached = SupabaseClientProvider.postgrest
                    .from("cache_findings")
                    .select {
                        filter { eq("finding_key", findingKey) }
                    }
                    .decodeSingleOrNull<CacheFindingsEntity>()

                if (cached != null && cached.sentence.isNotBlank()) {
                    localFindingSentenceCache[findingKey] = cached.sentence
                    return Pair(cached.sentence, cached.generatedBy)
                }
            } catch (e: Exception) {
                Log.d(tag, "Supabase cache_findings lookup error: ${e.message}")
            }
        }

        val generatedSentence = geminiClient.rephraseFinding(finding)
        val generator = if (Config.isGeminiConfigured) "gemini" else "deterministic_template"

        localFindingSentenceCache[findingKey] = generatedSentence

        if (Config.isSupabaseConfigured) {
            try {
                val cacheEntry = CacheFindingsEntity(
                    findingKey = findingKey,
                    sentence = generatedSentence,
                    generatedBy = generator
                )
                SupabaseClientProvider.postgrest.from("cache_findings").upsert(cacheEntry)
            } catch (e: Exception) {
                Log.d(tag, "Could not write to cache_findings: ${e.message}")
            }
        }

        return Pair(generatedSentence, generator)
    }
}
