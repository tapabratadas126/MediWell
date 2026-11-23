package com.mediwell.app.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mediwell.app.data.client.SupabaseClientProvider
import com.mediwell.app.util.AppResult
import com.mediwell.app.util.Config
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class UserSession(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean = false
)

class AuthRepository(private val context: Context) {
    private val tag = "AuthRepository"
    private val supabaseAuth = SupabaseClientProvider.auth
    private val credentialManager = CredentialManager.create(context)

    // Flow of session state
    val sessionStatus: Flow<SessionStatus> = supabaseAuth.sessionStatus

    val currentUserSession: Flow<UserSession?> = supabaseAuth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.value.user
                if (user != null) {
                    UserSession(
                        userId = user.id,
                        email = user.email,
                        displayName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: user.email
                    )
                } else null
            }
            else -> cachedDemoSession
        }
    }

    private var cachedDemoSession: UserSession? = null

    /**
     * Get the current active user ID for database operations.
     */
    fun getCurrentUserId(): String {
        return supabaseAuth.currentUserOrNull()?.id 
            ?: cachedDemoSession?.userId 
            ?: "guest_user_${UUID.randomUUID().toString().take(8)}"
    }

    /**
     * Initiates Google Sign-In using Android's modern Credential Manager API.
     * Passes the ID token to Supabase Auth GoTrue.
     */
    suspend fun signInWithGoogle(serverClientId: String? = null): AppResult<UserSession> {
        try {
            if (!Config.isSupabaseConfigured) {
                Log.w(tag, "Supabase anon key is a placeholder. Using demo authenticated session.")
                return continueWithDemoSession("Dr. Guest (Demo)", "guest.patient@mediwell.app")
            }

            // If a Google Web Client ID is available, use Credential Manager
            if (!serverClientId.isNullOrBlank()) {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Sign in to Supabase with Google ID Token
                    supabaseAuth.signInWith(IDToken) {
                        this.idToken = idToken
                        this.provider = Google
                    }

                    val user = supabaseAuth.currentUserOrNull()
                    return if (user != null) {
                        AppResult.Success(
                            UserSession(
                                userId = user.id,
                                email = user.email,
                                displayName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                            )
                        )
                    } else {
                        AppResult.Error("Supabase authentication succeeded but user was null")
                    }
                }
            }

            // Fallback for demo/development when Web Client ID is not yet provisioned in Google Cloud Console
            return continueWithDemoSession("Prototype User", "user@mediwell-prototype.app")
        } catch (e: GetCredentialException) {
            Log.e(tag, "Credential Manager error: ${e.message}", e)
            return AppResult.Error("Google Sign-In failed: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In exception: ${e.message}", e)
            return AppResult.Error("Authentication error: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Allows seamless testing in development/prototype environments without blocking the user.
     */
    fun continueWithDemoSession(name: String, email: String): AppResult<UserSession> {
        val session = UserSession(
            userId = "prototype_user_001",
            email = email,
            displayName = name,
            isAnonymous = true
        )
        cachedDemoSession = session
        return AppResult.Success(session)
    }

    suspend fun signOut() {
        cachedDemoSession = null
        try {
            if (Config.isSupabaseConfigured) {
                supabaseAuth.signOut()
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign out error: ${e.message}")
        }
    }
}
