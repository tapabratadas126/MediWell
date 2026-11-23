package com.mediwell.app.data.client

import android.util.Log
import com.mediwell.app.data.model.RxNormApproximateResponse
import com.mediwell.app.data.model.RxNormIdGroupResponse
import com.mediwell.app.data.model.RxNormMatchResult
import com.mediwell.app.data.model.RxNormProperties
import com.mediwell.app.data.model.RxNormPropertiesResponse
import com.mediwell.app.util.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Client for the free, public NIH NLM RxNav / RxNorm REST API.
 * Performs drug name -> RxCUI normalization.
 *
 * Attribution: "This product uses publicly available data from the U.S. National
 * Library of Medicine (NLM), National Institutes of Health, Department of Health
 * and Human Services; NLM is not responsible for the product and does not endorse
 * or recommend this or any other product."
 */
class RxNormClient {
    private val tag = "RxNormClient"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@RxNormClient.json)
        }
    }

    /**
     * Normalizes a drug name to an RxCUI.
     * Strategy:
     * 1. Exact / normalized lookup (search=2)
     * 2. If no exact match, approximate fallback (approximateTerm)
     * 3. Fetch properties (TTY, concept name)
     */
    suspend fun normalizeDrugName(rawName: String): RxNormMatchResult = withContext(Dispatchers.IO) {
        val cleanName = rawName.trim()
        if (cleanName.isBlank()) {
            return@withContext RxNormMatchResult(
                queryName = rawName,
                matchedRxcui = null,
                conceptName = null,
                isExact = false,
                isApproximate = false,
                isVerified = false
            )
        }

        try {
            // 1. Try exact/normalized search
            val exactResult = findRxcuiExactOrNormalized(cleanName)
            if (exactResult != null) {
                val props = getConceptProperties(exactResult)
                return@withContext RxNormMatchResult(
                    queryName = cleanName,
                    matchedRxcui = exactResult,
                    conceptName = props?.name ?: cleanName,
                    isExact = true,
                    isApproximate = false,
                    isVerified = true
                )
            }

            // 2. Try approximate fallback
            val approxResult = findRxcuiApproximate(cleanName)
            if (approxResult != null) {
                val props = getConceptProperties(approxResult)
                return@withContext RxNormMatchResult(
                    queryName = cleanName,
                    matchedRxcui = approxResult,
                    conceptName = props?.name ?: cleanName,
                    isExact = false,
                    isApproximate = true,
                    isVerified = false // Needs review because it was approximate
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error normalizing drug name '$cleanName' via RxNorm: ${e.message}", e)
        }

        // Unmatched fallback
        RxNormMatchResult(
            queryName = cleanName,
            matchedRxcui = null,
            conceptName = null,
            isExact = false,
            isApproximate = false,
            isVerified = false
        )
    }

    private suspend fun findRxcuiExactOrNormalized(name: String): String? {
        val url = "${Config.RXNORM_BASE_URL}/rxcui.json"
        val response = httpClient.get(url) {
            parameter("name", name)
            parameter("search", 2)
        }

        if (response.status != HttpStatusCode.OK) return null
        val body = response.body<RxNormIdGroupResponse>()
        return body.idGroup?.rxnormId?.firstOrNull()
    }

    private suspend fun findRxcuiApproximate(name: String): String? {
        val url = "${Config.RXNORM_BASE_URL}/approximateTerm.json"
        val response = httpClient.get(url) {
            parameter("term", name)
            parameter("maxEntries", 5)
            parameter("option", 1)
        }

        if (response.status != HttpStatusCode.OK) return null
        val body = response.body<RxNormApproximateResponse>()
        return body.approximateGroup?.candidate?.firstOrNull()?.rxcui
    }

    private suspend fun getConceptProperties(rxcui: String): RxNormProperties? {
        return try {
            val url = "${Config.RXNORM_BASE_URL}/rxcui/$rxcui/properties.json"
            val response = httpClient.get(url)
            if (response.status == HttpStatusCode.OK) {
                response.body<RxNormPropertiesResponse>().properties
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
