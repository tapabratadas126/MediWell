package com.mediwell.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for exact/normalized lookup:
 * GET /REST/rxcui.json?name={name}&search=2
 */
@Serializable
data class RxNormIdGroupResponse(
    @SerialName("idGroup")
    val idGroup: RxNormIdGroup? = null
)

@Serializable
data class RxNormIdGroup(
    @SerialName("name")
    val name: String? = null,
    @SerialName("rxnormId")
    val rxnormId: List<String>? = null
)

/**
 * Response for approximate terms:
 * GET /REST/approximateTerm.json?term={name}&maxEntries=5
 */
@Serializable
data class RxNormApproximateResponse(
    @SerialName("approximateGroup")
    val approximateGroup: RxNormApproximateGroup? = null
)

@Serializable
data class RxNormApproximateGroup(
    @SerialName("candidate")
    val candidate: List<RxNormCandidate>? = null
)

@Serializable
data class RxNormCandidate(
    @SerialName("rxcui")
    val rxcui: String? = null,
    @SerialName("score")
    val score: String? = null,
    @SerialName("rank")
    val rank: String? = null
)

/**
 * Response for concept properties:
 * GET /REST/rxcui/{rxcui}/properties.json
 */
@Serializable
data class RxNormPropertiesResponse(
    @SerialName("properties")
    val properties: RxNormProperties? = null
)

@Serializable
data class RxNormProperties(
    @SerialName("rxcui")
    val rxcui: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("synonym")
    val synonym: String? = null,
    @SerialName("tty")
    val tty: String? = null // IN, PIN, MIN, SBD, SCD
)

/**
 * Normalized result wrapper for the app.
 */
data class RxNormMatchResult(
    val queryName: String,
    val matchedRxcui: String?,
    val conceptName: String?,
    val isExact: Boolean,
    val isApproximate: Boolean,
    val isVerified: Boolean
)
