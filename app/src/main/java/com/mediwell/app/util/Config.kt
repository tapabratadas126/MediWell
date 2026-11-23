package com.mediwell.app.util

import com.mediwell.app.BuildConfig

/**
 * Central configuration holder for Mediwell.
 * Pulls from BuildConfig (populated via local.properties or env variables).
 * Note: Service role keys are NEVER included here or exposed in the client app.
 */
object Config {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://zggutewsucelczxxxdpu.supabase.co" }
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val geminiApiKey: String = BuildConfig.GEMINI_API_KEY
    val ocrApiKey: String = BuildConfig.OCR_API_KEY

    // Fixed, non-editable referral line required on every report
    const val FIXED_REFERRAL_DISCLAIMER =
        "Always consult a qualified pharmacist or doctor before changing or stopping any medication."

    // RxNorm public endpoint (free, no auth required)
    const val RXNORM_BASE_URL = "https://rxnav.nlm.nih.gov/REST"

    // Gemini API endpoint for Google AI Studio
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val GEMINI_MODEL = "gemini-1.5-flash"

    val isSupabaseConfigured: Boolean
        get() = supabaseAnonKey.isNotBlank() && !supabaseAnonKey.contains("PLACEHOLDER")

    val isGeminiConfigured: Boolean
        get() = geminiApiKey.isNotBlank() && !geminiApiKey.contains("PLACEHOLDER")
}
