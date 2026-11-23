package com.mediwell.app.data.repository

import android.util.Log
import com.mediwell.app.data.client.RxNormClient
import com.mediwell.app.data.client.SupabaseClientProvider
import com.mediwell.app.data.model.BrandIngredientMapEntity
import com.mediwell.app.data.model.MedicationEntity
import com.mediwell.app.data.model.MedicationInputForm
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.model.RxNormCacheEntity
import com.mediwell.app.util.AppResult
import com.mediwell.app.util.Config
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class MedicationRepository(
    private val rxNormClient: RxNormClient = RxNormClient()
) {
    private val tag = "MedicationRepository"

    // In-memory cache of user's active medications
    private val _medications = MutableStateFlow<List<MedicationItem>>(emptyList())
    val medications: StateFlow<List<MedicationItem>> = _medications.asStateFlow()

    // Local in-memory cache for RxNorm lookups to avoid redundant API calls
    private val localRxNormCache = mutableMapOf<String, String>()

    /**
     * Add a medication through the full normalization pipeline:
     * 1. Check rxnorm_cache in Supabase
     * 2. Check brand_ingredient_map in Supabase for Indian brands
     * 3. Call RxNorm API if needed to resolve RxCUI
     * 4. Persist to medications table in Supabase
     */
    suspend fun addMedication(userId: String, form: MedicationInputForm): AppResult<MedicationItem> = withContext(Dispatchers.IO) {
        val rawName = form.name.trim()
        if (rawName.isBlank()) {
            return@withContext AppResult.Error("Medication name cannot be empty")
        }

        try {
            var matchedRxcui: String? = null
            var matchedBrandId: String? = null
            var normalizedIngredient: String? = null
            var isVerified = false

            // Step 1: Check rxnorm_cache in Supabase / memory
            matchedRxcui = checkRxNormCache(rawName)

            // Step 2: Query brand_ingredient_map for Indian brands
            val brandMatch = lookupIndianBrand(rawName)
            if (brandMatch != null) {
                matchedBrandId = brandMatch.productId
                normalizedIngredient = brandMatch.primaryIngredient ?: brandMatch.activeIngredients
                isVerified = true
                Log.d(tag, "Matched Indian brand: ${brandMatch.brandName} -> ingredient: $normalizedIngredient")
            }

            // Step 3: Call RxNorm API to normalize to an RxCUI if not yet cached
            if (matchedRxcui == null) {
                val queryTerm = normalizedIngredient ?: rawName
                val rxResult = rxNormClient.normalizeDrugName(queryTerm)
                matchedRxcui = rxResult.matchedRxcui
                if (normalizedIngredient == null && rxResult.conceptName != null) {
                    normalizedIngredient = rxResult.conceptName
                }
                if (rxResult.isExact) {
                    isVerified = true
                }
                // Save to cache if found
                if (matchedRxcui != null) {
                    localRxNormCache[rawName.lowercase()] = matchedRxcui
                }
            } else {
                isVerified = true
            }

            // Step 4: Construct entity
            val entity = MedicationEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                rawName = rawName,
                matchedRxcui = matchedRxcui,
                matchedBrandId = matchedBrandId,
                dose = form.dose.trim(),
                frequency = form.frequency.trim(),
                route = form.route.trim(),
                source = "manual_entry"
            )

            // Persist to Supabase if configured
            if (Config.isSupabaseConfigured) {
                try {
                    SupabaseClientProvider.postgrest.from("medications").insert(entity)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to persist to Supabase: ${e.message}. Preserving in local list.")
                }
            }

            val item = MedicationItem(
                id = entity.id,
                userId = entity.userId,
                rawName = entity.rawName,
                matchedRxcui = entity.matchedRxcui,
                matchedBrandId = entity.matchedBrandId,
                dose = entity.dose ?: "",
                frequency = entity.frequency ?: "",
                route = entity.route ?: "",
                source = entity.source,
                normalizedIngredient = normalizedIngredient ?: entity.rawName,
                isVerified = isVerified
            )

            _medications.value = _medications.value + item
            AppResult.Success(item)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add medication: ${e.message}", e)
            AppResult.Error("Could not add medication: ${e.message}", e)
        }
    }

    /**
     * Check rxnorm_cache table in Supabase or local cache.
     */
    private suspend fun checkRxNormCache(name: String): String? {
        val key = name.lowercase().trim()
        if (localRxNormCache.containsKey(key)) {
            return localRxNormCache[key]
        }

        if (Config.isSupabaseConfigured) {
            try {
                val result = SupabaseClientProvider.postgrest
                    .from("rxnorm_cache")
                    .select {
                        filter {
                            eq("normalized_name", key)
                        }
                    }
                    .decodeSingleOrNull<RxNormCacheEntity>()

                if (result != null) {
                    localRxNormCache[key] = result.rxcui
                    return result.rxcui
                }
            } catch (e: Exception) {
                Log.d(tag, "Cache miss or error for '$key': ${e.message}")
            }
        }
        return null
    }

    /**
     * Search Indian pharmaceutical products catalogue table `brand_ingredient_map`.
     */
    private suspend fun lookupIndianBrand(name: String): BrandIngredientMapEntity? {
        if (!Config.isSupabaseConfigured) return null

        return try {
            val clean = name.trim()
            val results = SupabaseClientProvider.postgrest
                .from("brand_ingredient_map")
                .select {
                    filter {
                        ilike("brand_name", "$clean%")
                    }
                    limit(1)
                }
                .decodeList<BrandIngredientMapEntity>()

            results.firstOrNull()
        } catch (e: Exception) {
            Log.d(tag, "Brand ingredient map lookup error: ${e.message}")
            null
        }
    }

    /**
     * Load medications for user from Supabase.
     */
    suspend fun loadMedications(userId: String) = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) return@withContext

        try {
            val list = SupabaseClientProvider.postgrest
                .from("medications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MedicationEntity>()

            _medications.value = list.map { entity ->
                MedicationItem(
                    id = entity.id,
                    userId = entity.userId,
                    rawName = entity.rawName,
                    matchedRxcui = entity.matchedRxcui,
                    matchedBrandId = entity.matchedBrandId,
                    dose = entity.dose ?: "",
                    frequency = entity.frequency ?: "",
                    route = entity.route ?: "",
                    source = entity.source,
                    normalizedIngredient = entity.rawName,
                    isVerified = entity.matchedRxcui != null
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not load medications from Supabase: ${e.message}")
        }
    }

    /**
     * Delete a medication by ID.
     */
    suspend fun deleteMedication(id: String) = withContext(Dispatchers.IO) {
        _medications.value = _medications.value.filter { it.id != id }
        if (Config.isSupabaseConfigured) {
            try {
                SupabaseClientProvider.postgrest
                    .from("medications")
                    .delete {
                        filter {
                            eq("id", id)
                        }
                    }
            } catch (e: Exception) {
                Log.w(tag, "Failed to delete medication from Supabase: ${e.message}")
            }
        }
    }

    /**
     * Clear all current medications.
     */
    fun clearAll() {
        _medications.value = emptyList()
    }
}
