# Mediwell — Native Android Medication Safety Prototype

A native Android application built with **Kotlin** and **Jetpack Compose** that implements a strict, clinical-grade medication safety pipeline. Mediwell normalizes drug names via **RxNorm**, queries curated pharmacology datasets and DDInter (222,000+ interaction rows) in **Supabase**, verifies duplicate active ingredients, and produces a plain-language safety report for patients alongside an ontologically detailed clinician view.

---

## 🛡️ Non-Negotiable Clinical Guardrails

1. **Deterministic Clinical Facts Only**: The LLM (Google AI Studio Gemini API) **NEVER** decides clinical severity, duplicate status, or whether an interaction exists. All safety findings are produced strictly by deterministic database lookups and clinical rules.
2. **Single-Finding Rephrasing**: Gemini is invoked **only** for translating structured findings into plain, compassionate patient language, receiving **one structured finding at a time**.
3. **No Autonomous Dose Alterations**: The app and LLM are strictly prohibited from recommending stopping, starting, or modifying drug dosages.
4. **Permanent Professional Referral**: Every single generated report concludes with a **fixed, non-editable referral banner**:
   > *"Always consult a qualified pharmacist or doctor before changing or stopping any medication."*
5. **No Service Role Key in App**: Only the public Supabase Anon key (`SUPABASE_ANON_KEY`) is used client-side; reference tables have public read-only RLS policies.

---

## 🏗️ Architecture & Pipeline Flow

```
[ Patient Entry: Name, Dose, Frequency, Route ]
                     │
                     ▼
[ 1. RxNorm Normalization (RxNav REST API) ]
   ├── Check `rxnorm_cache` in Supabase
   ├── Check `brand_ingredient_map` (Indian brands catalogue)
   └── Call NLM RxNav exact (search=2) or approximateTerm -> RxCUI
                     │
                     ▼
[ 2. Deterministic Safety Engine ]
   ├── Duplicate Ingredient Check (Group by resolved active substance)
   ├── Pairwise DDI Search:
   │    ├── `interaction_curated` (Cardio, CNS, Anti-infectives)
   │    ├── `interaction_dataset` (DDInter 222k rows)
   │    └── `qt_risk_drugs` (Cardiac QT prolongation co-prescription)
   └── Confidence Tagging (`Verified` vs `Needs Review`)
                     │
                     ▼
[ 3. Plain-Language Explanation Engine ]
   ├── 1. Check `cache_findings` in Supabase by deterministic finding key
   └── 2. On Cache Miss: Call Gemini API (1 finding at a time)
          Prompt: "restate this exact information in plain language;
                   do not add any fact, drug name, or severity not given here."
                     │
                     ▼
[ 4. Dual-View Safety Report ]
   ├── Patient View: Plain language, friendly explanations, pill tags
   ├── Clinician View: RxCUIs, mechanisms, DDInter/Curated citations, timestamps
   └── Fixed Referral Banner to Pharmacist/Doctor
```

---

## 🗄️ Supabase Schema (`mediwell-prototype`, ID: `zggutewsucelczxxxdpu`)

| Table Name | Scope & Purpose |
|---|---|
| `medications` | User prescriptions (`user_id`, `raw_name`, `matched_rxcui`, `dose`, `frequency`, `route`, `source`) |
| `findings` | Recorded safety findings (`user_id`, `medication_id_1`, `medication_id_2`, `type`, `severity`, `confidence`) |
| `reports` | Dual view snapshots (`patient_view_json`, `clinician_view_json`, `generated_at`) |
| `rxnorm_cache` | Reference table (`normalized_name` PK, `rxcui`, `last_synced`) |
| `brand_ingredient_map` | 253,974 Indian pharmaceutical formulations mapping brand names to primary ingredients & strengths |
| `interaction_dataset` | 222,391 DDInter interaction records (`drug_a`, `drug_b`, `level`, `source`) |
| `interaction_curated` | High-risk cardio/CNS/anti-infective interaction table with mechanisms & citations |
| `qt_risk_drugs` | Medications associated with QT-interval prolongation |
| `cache_findings` | Plain-language sentence cache (`finding_key` PK, `sentence`, `generated_by`) |

---

## 🚀 Setup & Local Configuration

1. Copy `local.properties.example` to `local.properties`:
   ```properties
   SUPABASE_URL=https://zggutewsucelczxxxdpu.supabase.co
   SUPABASE_ANON_KEY=your_supabase_anon_key_here
   GEMINI_API_KEY=your_gemini_api_key_here
   OCR_API_KEY=your_ocr_api_key_here
   ```
2. Open the project in **Android Studio** (Ladybug / Iguana or later).
3. Build and run on an Android 8.0+ (API 26+) device or emulator.
4. Run unit tests:
   ```bash
   ./gradlew test
   ```

---

## 🧪 Included Test Suite

- `SafetyEngineTest`:
  - `testDuplicateActiveIngredientDetection`: Verifies duplicate detection (e.g. Crocin + Dolo both containing Paracetamol).
  - `testDrugInteractionGoldenRuleWarfarinAspirin`: Verifies critical hemorrhage risk detection.
  - `testDrugInteractionGoldenRuleStatinCYP3A4`: Verifies Simvastatin + Clarithromycin CYP3A4 myopathy detection.
  - `testQtProlongationRiskDetection`: Verifies dual-QT risk drug co-prescription alerting.
- `GeminiGuardrailTest`:
  - Verifies that deterministic templates and rephrased output never output dose changes or instructions to stop medications.
  - Verifies exact phrase match for the permanent referral disclaimer.
