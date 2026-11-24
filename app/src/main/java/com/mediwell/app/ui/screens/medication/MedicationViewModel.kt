package com.mediwell.app.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.MedicationInputForm
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.ocr.OcrService
import com.mediwell.app.data.repository.MedicationRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MedicationEntryState {
    data object Idle : MedicationEntryState()
    data object Normalizing : MedicationEntryState()
    data object ScanningOcr : MedicationEntryState()
    data class Success(val message: String) : MedicationEntryState()
    data class Error(val message: String) : MedicationEntryState()
}

class MedicationViewModel(
    private val repository: MedicationRepository,
    private val userId: String
) : ViewModel() {

    val medications: StateFlow<List<MedicationItem>> = repository.medications

    private val _entryState = MutableStateFlow<MedicationEntryState>(MedicationEntryState.Idle)
    val entryState: StateFlow<MedicationEntryState> = _entryState.asStateFlow()

    private val _form = MutableStateFlow(MedicationInputForm())
    val form: StateFlow<MedicationInputForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadMedications(userId)
        }
    }

    fun updateName(name: String) {
        _form.value = _form.value.copy(name = name)
    }

    fun updateDose(dose: String) {
        _form.value = _form.value.copy(dose = dose)
    }

    fun updateFrequency(frequency: String) {
        _form.value = _form.value.copy(frequency = frequency)
    }

    fun updateRoute(route: String) {
        _form.value = _form.value.copy(route = route)
    }

    fun setQuickFill(name: String, dose: String, freq: String = "Once daily", route: String = "Oral") {
        _form.value = MedicationInputForm(name = name, dose = dose, frequency = freq, route = route)
    }

    fun processOcr(base64Image: String) {
        viewModelScope.launch {
            try {
                _entryState.value = MedicationEntryState.ScanningOcr
                when (val result = OcrService.extractText(base64Image)) {
                    is AppResult.Success -> {
                        val rawText = result.data
                        val parsed = parseOcrText(rawText)
                        val (extractedName, extractedDose, extractedFreq, extractedRoute) = parsed

                        // Pre-fill form with extracted OCR data and source = "ocr"
                        _form.value = _form.value.copy(
                            name      = extractedName.ifBlank { _form.value.name },
                            dose      = extractedDose.ifBlank { _form.value.dose },
                            frequency = extractedFreq.ifBlank { _form.value.frequency },
                            route     = extractedRoute.ifBlank { _form.value.route },
                            source    = "ocr"
                        )

                        // Persist scanned prescription OCR log in Supabase safely
                        try {
                            repository.saveScannedPrescription(
                                userId = userId,
                                rawOcrText = rawText,
                                detectedName = extractedName.takeIf { it.isNotBlank() },
                                detectedDose = extractedDose.takeIf { it.isNotBlank() },
                                detectedFrequency = extractedFreq.takeIf { it.isNotBlank() }
                            )
                        } catch (repoErr: Throwable) {
                            Log.w("MedicationViewModel", "Failed to persist OCR log to Supabase", repoErr)
                        }

                        _entryState.value = MedicationEntryState.Success("Prescription scanned! Please review details.")
                    }
                    is AppResult.Error -> {
                        _entryState.value = MedicationEntryState.Error(result.message)
                    }
                    is AppResult.Loading -> Unit
                }
            } catch (t: Throwable) {
                Log.e("MedicationViewModel", "Unexpected exception during OCR handling", t)
                _entryState.value = MedicationEntryState.Error("OCR processing error: ${t.localizedMessage ?: "Unknown failure"}")
            }
        }
    }

    private data class ParsedOcrData(
        val name: String,
        val dose: String,
        val frequency: String,
        val route: String = ""
    )

    /**
     * Prescription OCR parser tuned for typical Indian / international formats, e.g.:
     *
     *  Tab. Metformin 500 mg  BD  x 30 days  PO
     *  Cap Amoxicillin 250mg TID × 7 days oral
     *  Inj. Insulin Glargine 10 units SC HS
     *  Syp. Paracetamol 125mg/5ml  5ml TDS   SOS
     *  Warfarin 5 mg Once daily  evening
     *
     * Strategy:
     *  1. Try to match the full "structured line" pattern first (dosage form + name + strength + freq + route).
     *  2. Fall back to individual field extraction across all lines.
     */
    private fun parseOcrText(rawText: String): ParsedOcrData {
        // ── Shared atomic patterns ─────────────────────────────────────────────
        val strengthPat = """(?i)(\d+(?:\.\d+)?(?:/\d+(?:\.\d+)?)?)\s*(mg|mcg|g|ml|l|iu|units?|mmol|mEq|%|nanogram|microgram)"""
        val strengthRe  = Regex(strengthPat)

        // Quantity + unit (e.g. "5 ml", "2 tab", "1 puff")
        val qtyUnitRe = Regex("""(?i)(\d+(?:\.\d+)?)\s*(tab(?:let)?s?|cap(?:sule)?s?|ml|puff|drop|unit|patch|sachet|teaspoon|tsp|tablespoon|tbsp|application)""")

        // Dosage-form prefix words (stripped before name extraction)
        val dosageFormPrefixRe = Regex(
            """(?i)^(tab(?:let)?\.?|cap(?:sule)?\.?|inj(?:ection)?\.?|syp(?:rup)?\.?|liq(?:uid)?\.?|susp(?:ension)?\.?|oint(?:ment)?\.?|drop\.?|gel\.?|patch\.?|inhaler|mdi|neb(?:uliser)?\.?|iv\.?|im\.?|sc\.?|ear drop|eye drop|nasal spray)\s+"""
        )

        // Frequency synonyms → canonical label
        val freqMap: List<Pair<Regex, String>> = listOf(
            Regex("""(?i)\bq(?:uar)?\.?i\.?d\.?\b|\bqid\b|\b4\s*times?\s*(?:a\s*)?day\b|\bfour\s*times?\s*(?:a\s*)?day\b""") to "Four times daily",
            Regex("""(?i)\bt\.?i\.?d\.?\b|\btid\b|\bthrice\s*(?:a\s*)?day\b|\b3\s*times?\s*(?:a\s*)?day\b|\btds\b""")     to "Thrice daily",
            Regex("""(?i)\bb\.?i\.?d\.?\b|\bbid\b|\btwice\s*(?:a\s*)?day\b|\b2\s*times?\s*(?:a\s*)?day\b|\bbd\b""")        to "Twice daily",
            Regex("""(?i)\bsos\b|\bprn\b|\bas\s*needed\b|\bwhen\s*required\b|\bif\s*needed\b""")                           to "Every 6 hours as needed",
            Regex("""(?i)\bhs\b|\bat\s*bedtime\b|\bnight\b|\bnocte\b""")                                                  to "At bedtime",
            Regex("""(?i)\bonce\s*(?:a\s*)?day\b|\bo\.?d\.?\b|\bdaily\b|\bq\.?d\.?\b|\bevery\s*(?:24|1)\s*hours?\b""")    to "Once daily",
            Regex("""(?i)\bevery\s*(\d+)\s*hours?\b""")                                                                    to "Every \$1 hours",
            Regex("""(?i)\bonce\s*(?:a\s*)?week(?:ly)?\b""")                                                              to "Once weekly",
            Regex("""(?i)\btwice\s*(?:a\s*)?week(?:ly)?\b""")                                                             to "Twice weekly",
            Regex("""(?i)\bonce\s*(?:a\s*)?month(?:ly)?\b""")                                                             to "Once monthly",
        )

        // Route synonyms → canonical label
        val routeMap: List<Pair<Regex, String>> = listOf(
            Regex("""(?i)\b(by\s*mouth|per\s*os|p\.?o\.?|oral(?:ly)?)\b""")                   to "Oral",
            Regex("""(?i)\b(sublingual|s\.?l\.?|under\s*the?\s*tongue)\b""")                   to "Sublingual",
            Regex("""(?i)\b(inhal(?:ation|ed)?|via\s*inhaler|puff)\b""")                       to "Inhalation",
            Regex("""(?i)\b(topical(?:ly)?|apply\s*to|on\s*skin)\b""")                        to "Topical",
            Regex("""(?i)\b(subcutaneous(?:ly)?|s\.?c\.?|subcut)\b""")                        to "Subcutaneous",
            Regex("""(?i)\b(intramusc(?:ular)?(?:ly)?|i\.?m\.?)\b""")                         to "Intramuscular",
            Regex("""(?i)\b(intravenous(?:ly)?|i\.?v\.?)\b""")                                to "Intramuscular",
            Regex("""(?i)\b(ophthalmic|eye\s*drop)\b""")                                      to "Ophthalmic",
            Regex("""(?i)\b(otic|ear\s*drop)\b""")                                            to "Ophthalmic",
        )

        // Words that look like drug names but are definitely noise
        val stopWords = setOf(
            "rx", "tab", "cap", "tablet", "capsule", "syrup", "injection", "inj", "syp",
            "ml", "mg", "mcg", "iu", "daily", "oral", "take", "use", "apply", "with",
            "food", "water", "before", "after", "morning", "afternoon", "evening", "night",
            "days", "weeks", "months", "times", "once", "twice", "every", "hours", "qty",
            "quantity", "sig", "dispense", "refills", "dr", "date", "patient", "name",
            "age", "sex", "dob", "address", "clinic", "hospital", "pharmacy"
        )

        // ── Normalize text ─────────────────────────────────────────────────────
        val lines = rawText
            .replace(Regex("""[,;:]+"""), " ")            // commas → spaces
            .replace(Regex("""×|x(?=\s*\d)"""), " x ")   // "×" / "x30" spacing
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 2 }

        if (lines.isEmpty()) return ParsedOcrData("", "", "")

        var extractedName = ""
        var extractedDose = ""
        var extractedFreq = ""
        var extractedRoute = ""

        // ── Pass 1: Try structured line match ─────────────────────────────────
        // Pattern: [dosage form?] DRUG_NAME STRENGTH FREQ? ROUTE?
        // e.g.  "Tab. Metformin 500mg BD PO"
        val structuredLineRe = Regex(
            """(?i)^(?:tab(?:let)?\.?\s+|cap(?:sule)?\.?\s+|syp\.?\s+|inj\.?\s+)?([A-Za-z][\w\s\-]{1,40?}?)\s+""" +
            """(\d+(?:\.\d+)?(?:/\d+(?:\.\d+)?)?)\s*(mg|mcg|g|ml|l|iu|units?|%)\b"""
        )

        for (line in lines) {
            if (extractedName.isNotBlank() && extractedDose.isNotBlank()) break
            val m = structuredLineRe.find(line) ?: continue

            val candidate = m.groupValues[1]
                .replace(dosageFormPrefixRe, "")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.lowercase() !in stopWords && it.length >= 2 }
                .joinToString(" ")

            if (candidate.isNotBlank() && extractedName.isBlank()) {
                extractedName = candidate.split(" ").firstOrNull { it.length >= 3 } ?: candidate
            }
            if (extractedDose.isBlank()) {
                extractedDose = "${m.groupValues[2]} ${m.groupValues[3]}"
            }
        }

        // ── Pass 2: Fallback line-by-line extraction ───────────────────────────
        for (line in lines) {
            // Strength / dose
            if (extractedDose.isBlank()) {
                strengthRe.find(line)?.let { m ->
                    // Also try to grab a leading quantity (e.g. "5 ml BD" → dose = "5 ml")
                    val qty = qtyUnitRe.find(line)
                    extractedDose = qty?.value ?: m.value
                }
            }

            // Frequency
            if (extractedFreq.isBlank()) {
                for ((re, canonical) in freqMap) {
                    val fm = re.find(line)
                    if (fm != null) {
                        // Replace backreference \$1 for "every N hours" pattern
                        extractedFreq = canonical.replace("\$1", fm.groupValues.getOrNull(1) ?: "")
                        break
                    }
                }
            }

            // Route
            if (extractedRoute.isBlank()) {
                for ((re, canonical) in routeMap) {
                    if (re.containsMatchIn(line)) { extractedRoute = canonical; break }
                }
            }

            // Name – from lines that have a strength token (most drug lines do)
            if (extractedName.isBlank() && strengthRe.containsMatchIn(line)) {
                val stripped = line
                    .replace(dosageFormPrefixRe, "")
                    .replace(strengthRe, "")
                    .replace(freqMap.map { it.first }.reduce { a, b -> Regex("${a.pattern}|${b.pattern}") }, "")
                    .replace(routeMap.map { it.first }.reduce { a, b -> Regex("${a.pattern}|${b.pattern}") }, "")
                    .replace(Regex("""x\s*\d+\s*(day|week|month)s?\b""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""[^a-zA-Z\s\-]"""), "")
                    .split(Regex("\\s+"))
                    .filter { tok -> tok.length >= 2 && tok.lowercase() !in stopWords }
                    .joinToString(" ")
                    .trim()

                if (stripped.isNotBlank()) {
                    // Take the longest token as drug name (usually the brand/generic name)
                    extractedName = stripped.split(" ").maxByOrNull { it.length } ?: stripped
                }
            }
        }

        // ── Pass 3: Last resort – first long alphabetic token not in stopwords ─
        if (extractedName.isBlank()) {
            for (line in lines) {
                val tok = line
                    .replace(dosageFormPrefixRe, "")
                    .split(Regex("\\s+"))
                    .firstOrNull { it.length >= 4 && it.all { c -> c.isLetter() || c == '-' } && it.lowercase() !in stopWords }
                if (tok != null) { extractedName = tok; break }
            }
        }

        // Title-case the extracted name
        extractedName = extractedName.trim().split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        return ParsedOcrData(
            name      = extractedName,
            dose      = extractedDose.trim(),
            frequency = extractedFreq,
            route     = extractedRoute
        )
    }

    fun addMedication() {
        val currentForm = _form.value
        if (!currentForm.isValid) {
            _entryState.value = MedicationEntryState.Error("Please enter a valid medication name")
            return
        }

        viewModelScope.launch {
            _entryState.value = MedicationEntryState.Normalizing
            when (val result = repository.addMedication(userId, currentForm)) {
                is AppResult.Success -> {
                    _entryState.value = MedicationEntryState.Success("Medication normalized and added")
                    _form.value = MedicationInputForm()
                }
                is AppResult.Error -> {
                    _entryState.value = MedicationEntryState.Error(result.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun clearState() {
        _entryState.value = MedicationEntryState.Idle
    }
}
