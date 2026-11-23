package com.mediwell.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.auth.AuthRepository
import com.mediwell.app.data.auth.UserSession
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Authenticated(val session: UserSession) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isSignUp = MutableStateFlow(false)
    val isSignUp: StateFlow<Boolean> = _isSignUp.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserSession.collect { session ->
                if (session != null) {
                    _uiState.value = AuthUiState.Authenticated(session)
                }
            }
        }
    }

    fun updateEmail(value: String) {
        _email.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun toggleAuthMode() {
        _isSignUp.value = !_isSignUp.value
        clearError()
    }

    fun setAuthMode(isSignUpMode: Boolean) {
        _isSignUp.value = isSignUpMode
        clearError()
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun submitEmailAuth() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email and password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = if (_isSignUp.value) {
                authRepository.signUpWithEmail(currentEmail, currentPassword)
            } else {
                authRepository.signInWithEmail(currentEmail, currentPassword)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun signInWithGoogle(serverClientId: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signInWithGoogle(serverClientId)) {
                is AppResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun continueAsDemoUser() {
        val result = authRepository.continueWithDemoSession("Prototype User", "patient@mediwell.app")
        if (result is AppResult.Success) {
            _uiState.value = AuthUiState.Authenticated(result.data)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }
}
