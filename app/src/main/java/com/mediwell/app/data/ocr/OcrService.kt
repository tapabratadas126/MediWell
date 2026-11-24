package com.mediwell.app.data.ocr

import android.util.Log
import com.mediwell.app.BuildConfig
import com.mediwell.app.util.AppResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service that calls OCR.space free REST API to extract raw text from a base64-encoded image.
 * Uses BuildConfig.OCR_API_KEY.
 */
object OcrService {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractText(base64Image: String): AppResult<String> {
        val apiKey = BuildConfig.OCR_API_KEY
        if (apiKey.isBlank() || apiKey == "OCR_API_KEY_PLACEHOLDER") {
            return AppResult.Error("OCR API key is not configured. Set OCR_API_KEY in local.properties.")
        }
        return try {
            val formattedBase64 = if (!base64Image.startsWith("data:image")) {
                "data:image/jpeg;base64,$base64Image"
            } else {
                base64Image
            }

            val httpResponse: HttpResponse = client.submitForm(
                url = "https://api.ocr.space/parse/image",
                formParameters = parameters {
                    append("apikey", apiKey)
                    append("base64Image", formattedBase64)
                    append("language", "eng")
                    append("isOverlayRequired", "false")
                    append("detectOrientation", "true")
                    append("scale", "true")
                }
            )

            val responseBody = httpResponse.bodyAsText()
            val parsed = json.decodeFromString<OcrSpaceResponse>(responseBody)

            if (parsed.isErroredOnProcessing == true || parsed.errorMessage != null) {
                val errorMsg = parsed.errorMessage?.joinToString(", ") ?: "OCR processing failed"
                AppResult.Error(errorMsg)
            } else {
                val parsedText = parsed.parsedResults?.firstOrNull()?.parsedText
                if (parsedText.isNullOrBlank()) {
                    AppResult.Error("No text detected in the prescription image.")
                } else {
                    AppResult.Success(parsedText)
                }
            }
        } catch (e: Exception) {
            Log.e("OcrService", "OCR failed", e)
            AppResult.Error("OCR request failed: ${e.message ?: "Unknown error"}")
        }
    }
}

@Serializable
private data class OcrSpaceResponse(
    val OCRExitCode: Int? = null,
    val IsErroredOnProcessing: Boolean? = null,
    val ErrorMessage: List<String>? = null,
    val ErrorDetails: String? = null,
    val ParsedResults: List<ParsedResult>? = null
) {
    val isErroredOnProcessing: Boolean? get() = IsErroredOnProcessing
    val errorMessage: List<String>? get() = ErrorMessage
    val parsedResults: List<ParsedResult>? get() = ParsedResults
}

@Serializable
private data class ParsedResult(
    val ParsedText: String? = null
) {
    val parsedText: String? get() = ParsedText
}
