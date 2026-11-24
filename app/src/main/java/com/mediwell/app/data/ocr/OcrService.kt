package com.mediwell.app.data.ocr

import android.util.Log
import com.mediwell.app.BuildConfig
import com.mediwell.app.util.AppResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service that calls OCR.space free REST API to extract raw text from a base64-encoded image.
 * Protected with comprehensive error handling to guarantee crashes are never propagated.
 */
object OcrService {
    private const val TAG = "OcrService"
    private val client = HttpClient()
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }

    suspend fun extractText(base64Image: String): AppResult<String> {
        return try {
            val apiKey = BuildConfig.OCR_API_KEY
            if (apiKey.isBlank() || apiKey == "OCR_API_KEY_PLACEHOLDER") {
                return AppResult.Error("OCR API key is not configured. Set OCR_API_KEY in local.properties.")
            }

            if (base64Image.isBlank()) {
                return AppResult.Error("Image data was empty. Please select a clear photo of the prescription.")
            }

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
            Log.d(TAG, "OCR response status: ${httpResponse.status}, body length: ${responseBody.length}")

            val parsed = try {
                json.decodeFromString<OcrSpaceResponse>(responseBody)
            } catch (jsonErr: Throwable) {
                Log.e(TAG, "Failed to parse OCR response JSON: $responseBody", jsonErr)
                return AppResult.Error("Could not read response from OCR service. Please try again.")
            }

            if (parsed.isErroredOnProcessing == true || !parsed.errorMessage.isNullOrEmpty()) {
                val errorMsg = parsed.errorMessage?.filter { it.isNotBlank() }?.joinToString(", ")
                    ?: parsed.errorDetails
                    ?: "OCR processing failed"
                Log.w(TAG, "OCR returned processing error: $errorMsg")
                AppResult.Error(errorMsg)
            } else {
                val parsedText = parsed.parsedResults?.firstOrNull()?.parsedText
                if (parsedText.isNullOrBlank()) {
                    AppResult.Error("No text detected in the prescription image. Try capturing in better lighting.")
                } else {
                    AppResult.Success(parsedText)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal OCR exception caught safely", e)
            AppResult.Error("Unable to scan prescription: ${e.localizedMessage ?: "Network or processing error"}")
        }
    }
}

@Serializable
private data class OcrSpaceResponse(
    @SerialName("OCRExitCode") val ocrExitCode: Int? = null,
    @SerialName("IsErroredOnProcessing") val isErroredOnProcessing: Boolean? = null,
    @SerialName("ErrorMessage") val errorMessage: List<String>? = null,
    @SerialName("ErrorDetails") val errorDetails: String? = null,
    @SerialName("ParsedResults") val parsedResults: List<ParsedResult>? = null
)

@Serializable
private data class ParsedResult(
    @SerialName("ParsedText") val parsedText: String? = null
)
