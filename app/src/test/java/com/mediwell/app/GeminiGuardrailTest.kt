package com.mediwell.app

import com.mediwell.app.data.client.GeminiClient
import com.mediwell.app.data.model.ConfidenceLevel
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.data.model.FindingType
import com.mediwell.app.data.model.SafetyFinding
import com.mediwell.app.util.Config
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiGuardrailTest {

    private val geminiClient = GeminiClient()

    @Test
    fun testDeterministicTemplateNeverSuggestsDoseChange() {
        val finding = SafetyFinding(
            findingKey = "test_dup",
            drugName1 = "Crocin",
            drugName2 = "Dolo",
            type = FindingType.DUPLICATE_INGREDIENT,
            severity = FindingSeverity.WARNING,
            mechanismOrDetail = "Paracetamol duplicate",
            sourceCitation = "Mediwell Catalogue",
            sourceTable = "brand_ingredient_map",
            confidence = ConfidenceLevel.VERIFIED
        )

        val sentence = geminiClient.generateDeterministicSentence(finding)

        assertNotNull(sentence)
        // Hard guardrails: never suggest stopping, starting, or altering doses
        val lower = sentence.lowercase()
        assertFalse("Template must never instruct to stop taking medication", lower.contains("stop taking"))
        assertFalse("Template must never instruct to reduce dose", lower.contains("reduce your dose"))
        assertFalse("Template must never suggest a numeric dose change", lower.contains("cut dose"))
        assertTrue("Template should explain accidental duplicate/overdose risk", lower.contains("active medicine") || lower.contains("duplicate"))
    }

    @Test
    fun testFixedReferralDisclaimerIntegrity() {
        val requiredReferral = "Always consult a qualified pharmacist or doctor before changing or stopping any medication."
        assertEquals(requiredReferral, Config.FIXED_REFERRAL_DISCLAIMER)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
