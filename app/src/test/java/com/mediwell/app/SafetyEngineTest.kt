package com.mediwell.app

import com.mediwell.app.data.model.AllergyRecord
import com.mediwell.app.data.model.ConfidenceLevel
import com.mediwell.app.data.model.FindingSeverity
import com.mediwell.app.data.model.FindingType
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.repository.SafetyEngineRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafetyEngineTest {

    private lateinit var safetyEngine: SafetyEngineRepository

    @Before
    fun setUp() {
        safetyEngine = SafetyEngineRepository()
    }

    @Test
    fun testDuplicateActiveIngredientDetection() = runBlocking {
        // Patient takes both Crocin (paracetamol) and Dolo (paracetamol)
        val medications = listOf(
            MedicationItem(
                id = "med_1",
                userId = "user_1",
                rawName = "Crocin 650",
                matchedRxcui = "161",
                matchedBrandId = "brand_crocin",
                dose = "650 mg",
                frequency = "Twice daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Paracetamol",
                isVerified = true
            ),
            MedicationItem(
                id = "med_2",
                userId = "user_1",
                rawName = "Dolo 650",
                matchedRxcui = "161",
                matchedBrandId = "brand_dolo",
                dose = "650 mg",
                frequency = "Thrice daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Paracetamol",
                isVerified = true
            )
        )

        val findings = safetyEngine.runSafetyAnalysis("user_1", medications)

        val duplicate = findings.find { it.type == FindingType.DUPLICATE_INGREDIENT }
        assertNotNull("Should detect duplicate ingredient finding", duplicate)
        assertEquals(FindingSeverity.WARNING, duplicate?.severity)
        assertEquals(ConfidenceLevel.VERIFIED, duplicate?.confidence)
        assertTrue(duplicate?.mechanismOrDetail?.contains("Paracetamol", ignoreCase = true) == true)
    }

    @Test
    fun testDrugInteractionGoldenRuleWarfarinAspirin() = runBlocking {
        val medications = listOf(
            MedicationItem(
                id = "med_1",
                userId = "user_1",
                rawName = "Warfarin",
                matchedRxcui = "11289",
                matchedBrandId = null,
                dose = "5 mg",
                frequency = "Once daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Warfarin",
                isVerified = true
            ),
            MedicationItem(
                id = "med_2",
                userId = "user_1",
                rawName = "Aspirin",
                matchedRxcui = "1191",
                matchedBrandId = null,
                dose = "75 mg",
                frequency = "Once daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Aspirin",
                isVerified = true
            )
        )

        val findings = safetyEngine.runSafetyAnalysis("user_1", medications)

        val ddi = findings.find { it.type == FindingType.DRUG_INTERACTION }
        assertNotNull("Should detect Warfarin + Aspirin interaction", ddi)
        assertEquals(FindingSeverity.CRITICAL, ddi?.severity)
        assertEquals(ConfidenceLevel.VERIFIED, ddi?.confidence)
        assertTrue(ddi?.mechanismOrDetail?.contains("hemorrhage", ignoreCase = true) == true ||
                   ddi?.mechanismOrDetail?.contains("bleeding", ignoreCase = true) == true)
    }

    @Test
    fun testDrugInteractionGoldenRuleStatinCYP3A4() = runBlocking {
        val medications = listOf(
            MedicationItem(
                id = "med_1",
                userId = "user_1",
                rawName = "Simvastatin",
                matchedRxcui = "36567",
                matchedBrandId = null,
                dose = "20 mg",
                frequency = "At bedtime",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Simvastatin",
                isVerified = true
            ),
            MedicationItem(
                id = "med_2",
                userId = "user_1",
                rawName = "Clarithromycin",
                matchedRxcui = "21212",
                matchedBrandId = null,
                dose = "500 mg",
                frequency = "Twice daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Clarithromycin",
                isVerified = true
            )
        )

        val findings = safetyEngine.runSafetyAnalysis("user_1", medications)

        val ddi = findings.find { it.type == FindingType.DRUG_INTERACTION }
        assertNotNull("Should detect Simvastatin + Clarithromycin interaction", ddi)
        assertEquals(FindingSeverity.CRITICAL, ddi?.severity)
        assertTrue(ddi?.mechanismOrDetail?.contains("rhabdomyolysis", ignoreCase = true) == true ||
                   ddi?.mechanismOrDetail?.contains("myopathy", ignoreCase = true) == true)
    }

    @Test
    fun testQtProlongationRiskDetection() = runBlocking {
        val medications = listOf(
            MedicationItem(
                id = "med_1",
                userId = "user_1",
                rawName = "Amiodarone",
                matchedRxcui = "703",
                matchedBrandId = null,
                dose = "200 mg",
                frequency = "Once daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Amiodarone",
                isVerified = true
            ),
            MedicationItem(
                id = "med_2",
                userId = "user_1",
                rawName = "Azithromycin",
                matchedRxcui = "18631",
                matchedBrandId = null,
                dose = "500 mg",
                frequency = "Once daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Azithromycin",
                isVerified = true
            )
        )

        val findings = safetyEngine.runSafetyAnalysis("user_1", medications)

        val qtFinding = findings.find { it.type == FindingType.QT_RISK }
        assertNotNull("Should detect QT Prolongation risk", qtFinding)
        assertEquals(FindingSeverity.CRITICAL, qtFinding?.severity)
    }

    @Test
    fun testAllergyExactAndClassMatchDetection() = runBlocking {
        val medications = listOf(
            MedicationItem(
                id = "med_1",
                userId = "user_1",
                rawName = "Amoxicillin 500mg",
                matchedRxcui = "723",
                matchedBrandId = null,
                dose = "500 mg",
                frequency = "Thrice daily",
                route = "Oral",
                source = "manual_entry",
                normalizedIngredient = "Amoxicillin",
                isVerified = true
            )
        )
        val allergies = listOf(
            AllergyRecord(
                id = "alg_1",
                userId = "user_1",
                allergenName = "Penicillin",
                reaction = "Hives and rash",
                severity = "Severe"
            )
        )

        val findings = safetyEngine.runSafetyAnalysis("user_1", medications, allergies)

        val allergyFinding = findings.find { it.type == FindingType.ALLERGY_MATCH }
        assertNotNull("Should detect Penicillin class allergy for Amoxicillin", allergyFinding)
        assertEquals(FindingSeverity.CRITICAL, allergyFinding?.severity)
        assertTrue(allergyFinding?.mechanismOrDetail?.contains("Penicillin", ignoreCase = true) == true)
    }
}
