package com.mediwell.app.data.repository

import android.util.Log
import com.mediwell.app.data.client.SupabaseClientProvider
import com.mediwell.app.data.model.PatientProfile
import com.mediwell.app.util.AppResult
import io.github.jan.supabase.postgrest.from

class ProfileRepository {
    private val tag = "ProfileRepository"

    suspend fun fetchProfile(userId: String): AppResult<PatientProfile?> {
        return try {
            val result = SupabaseClientProvider.client.from("patient_profiles")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<PatientProfile>()
            AppResult.Success(result)
        } catch (e: Exception) {
            Log.e(tag, "fetchProfile error: ${e.message}", e)
            // Table may not exist yet — return null profile gracefully
            AppResult.Success(null)
        }
    }

    suspend fun upsertProfile(profile: PatientProfile): AppResult<Unit> {
        return try {
            SupabaseClientProvider.client.from("patient_profiles")
                .upsert(profile) {
                    onConflict = "user_id"
                }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "upsertProfile error: ${e.message}", e)
            AppResult.Error("Failed to save profile: ${e.localizedMessage}", e)
        }
    }
}
