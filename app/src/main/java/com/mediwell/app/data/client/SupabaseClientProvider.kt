package com.mediwell.app.data.client

import android.util.Log
import com.mediwell.app.util.Config
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.android.Android
import kotlinx.serialization.json.Json

/**
 * Singleton provider for SupabaseClient.
 * Uses public SUPABASE_ANON_KEY (never service role key).
 */
object SupabaseClientProvider {
    private const val TAG = "SupabaseClient"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    val client: SupabaseClient by lazy {
        Log.i(TAG, "Initializing SupabaseClient with URL: ${Config.supabaseUrl}")
        createSupabaseClient(
            supabaseUrl = Config.supabaseUrl,
            supabaseKey = Config.supabaseAnonKey
        ) {
            httpEngine = Android.create()
            install(Auth) {
                // Settings for Auth
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest) {
                defaultSchema = "public"
            }
            install(Realtime)
        }
    }

    val auth: Auth get() = client.auth
    val postgrest: Postgrest get() = client.postgrest
    val realtime: Realtime get() = client.realtime
}
