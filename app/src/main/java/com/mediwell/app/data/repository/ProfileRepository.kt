package com.mediwell.app.data.repository

import android.util.Log
import com.mediwell.app.data.client.SupabaseClientProvider
import com.mediwell.app.data.model.AllergyRecord
import com.mediwell.app.data.model.PatientProfile
import com.mediwell.app.util.AppResult
import com.mediwell.app.util.Config
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileRepository {
    private val tag = "ProfileRepository"

    suspend fun fetchProfile(userId: String): AppResult<PatientProfile?> = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) return@withContext AppResult.Success(null)
        return@withContext try {
            val result = SupabaseClientProvider.client.from("patient_profiles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<PatientProfile>()
            AppResult.Success(result)
        } catch (e: Exception) {
            Log.e(tag, "fetchProfile error: ${e.message}", e)
            AppResult.Success(null)
        }
    }

    suspend fun upsertProfile(profile: PatientProfile): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) {
            return@withContext AppResult.Error("Backend is not configured.")
        }
        return@withContext try {
            val toSave = if (profile.id.isBlank()) profile.copy(id = UUID.randomUUID().toString()) else profile
            SupabaseClientProvider.client.from("patient_profiles")
                .upsert(toSave) {
                    onConflict = "user_id"
                }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "upsertProfile error: ${e.message}", e)
            AppResult.Error("Failed to save profile: ${e.localizedMessage}", e)
        }
    }

    /**
     * Fetch structured allergy records from `patient_allergies`.
     * Falls back to `patient_profiles.allergies` if `patient_allergies` is empty or unavailable.
     */
    suspend fun fetchAllergies(userId: String): AppResult<List<AllergyRecord>> = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) return@withContext AppResult.Success(emptyList())
        return@withContext try {
            val records = SupabaseClientProvider.client.from("patient_allergies")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<AllergyRecord>()

            if (records.isNotEmpty()) {
                AppResult.Success(records)
            } else {
                // Fallback to legacy string array on patient_profiles if present
                val profile = (fetchProfile(userId) as? AppResult.Success)?.data
                val legacy = profile?.allergies.orEmpty()
                    .filter { it.isNotBlank() }
                    .map { name ->
                        AllergyRecord(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            allergenName = name.trim(),
                            severity = "Not specified"
                        )
                    }
                AppResult.Success(legacy)
            }
        } catch (e: Exception) {
            Log.w(tag, "fetchAllergies error (trying profile fallback): ${e.message}")
            val profile = (fetchProfile(userId) as? AppResult.Success)?.data
            val legacy = profile?.allergies.orEmpty()
                .filter { it.isNotBlank() }
                .map { name ->
                    AllergyRecord(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        allergenName = name.trim(),
                        severity = "Not specified"
                    )
                }
            AppResult.Success(legacy)
        }
    }

    suspend fun upsertAllergy(record: AllergyRecord): AppResult<AllergyRecord> = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) {
            return@withContext AppResult.Error("Backend is not configured.")
        }
        val cleanName = record.allergenName.trim()
        if (cleanName.isBlank()) {
            return@withContext AppResult.Error("Allergen name cannot be empty.")
        }
        val saved = record.copy(
            id = record.id.ifBlank { UUID.randomUUID().toString() },
            allergenName = cleanName,
            reaction = record.reaction?.trim()?.ifBlank { null },
            severity = record.severity.ifBlank { "Not specified" }
        )
        return@withContext try {
            SupabaseClientProvider.client.from("patient_allergies")
                .upsert(saved)
            syncProfileAllergiesList(saved.userId)
            AppResult.Success(saved)
        } catch (e: Exception) {
            Log.e(tag, "upsertAllergy error: ${e.message}", e)
            AppResult.Error("Failed to save allergy: ${e.localizedMessage}", e)
        }
    }

    suspend fun deleteAllergy(allergyId: String, userId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (!Config.isSupabaseConfigured) {
            return@withContext AppResult.Error("Backend is not configured.")
        }
        return@withContext try {
            SupabaseClientProvider.client.from("patient_allergies")
                .delete {
                    filter {
                        eq("id", allergyId)
                        eq("user_id", userId)
                    }
                }
            syncProfileAllergiesList(userId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "deleteAllergy error: ${e.message}", e)
            AppResult.Error("Failed to delete allergy: ${e.localizedMessage}", e)
        }
    }

    private suspend fun syncProfileAllergiesList(userId: String) {
        try {
            val currentAllergies = SupabaseClientProvider.client.from("patient_allergies")
                .select { filter { eq("user_id", userId) } }
                .decodeList<AllergyRecord>()
                .map { it.allergenName }
            val existingProfile = (fetchProfile(userId) as? AppResult.Success)?.data
                ?: PatientProfile(id = UUID.randomUUID().toString(), userId = userId)
            upsertProfile(existingProfile.copy(allergies = currentAllergies))
        } catch (e: Exception) {
            Log.d(tag, "syncProfileAllergiesList skipped: ${e.message}")
        }
    }
}
