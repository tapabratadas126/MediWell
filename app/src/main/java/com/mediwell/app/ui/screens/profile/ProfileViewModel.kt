package com.mediwell.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.PatientProfile
import com.mediwell.app.data.repository.ProfileRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSaveState {
    object Idle    : ProfileSaveState()
    object Saving  : ProfileSaveState()
    object Success : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _profile   = MutableStateFlow(PatientProfile(userId = userId))
    val profile: StateFlow<PatientProfile> = _profile.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = profileRepository.fetchProfile(userId)
            if (result is AppResult.Success && result.data != null) {
                _profile.value = result.data
            } else {
                _profile.value = PatientProfile(userId = userId)
            }
            _isLoading.value = false
        }
    }

    fun updateFullName(name: String)         { _profile.value = _profile.value.copy(fullName = name) }
    fun updatePhone(phone: String)           { _profile.value = _profile.value.copy(phone = phone) }
    fun updateDateOfBirth(dob: String)       { _profile.value = _profile.value.copy(dateOfBirth = dob) }
    fun updateCity(city: String)             { _profile.value = _profile.value.copy(city = city) }
    fun updateConsentDataUse(v: Boolean)     { _profile.value = _profile.value.copy(consentDataUse = v) }
    fun updateConsentShare(v: Boolean)       { _profile.value = _profile.value.copy(consentShareProfessional = v) }

    fun addAllergy(item: String)             { if (item.isNotBlank()) _profile.value = _profile.value.copy(allergies = _profile.value.allergies + item.trim()) }
    fun removeAllergy(item: String)          { _profile.value = _profile.value.copy(allergies = _profile.value.allergies - item) }
    fun addCondition(item: String)           { if (item.isNotBlank()) _profile.value = _profile.value.copy(medicalConditions = _profile.value.medicalConditions + item.trim()) }
    fun removeCondition(item: String)        { _profile.value = _profile.value.copy(medicalConditions = _profile.value.medicalConditions - item) }
    fun addKidneyCondition(item: String)     { if (item.isNotBlank()) _profile.value = _profile.value.copy(kidneyConditions = _profile.value.kidneyConditions + item.trim()) }
    fun removeKidneyCondition(item: String)  { _profile.value = _profile.value.copy(kidneyConditions = _profile.value.kidneyConditions - item) }
    fun addLiverCondition(item: String)      { if (item.isNotBlank()) _profile.value = _profile.value.copy(liverConditions = _profile.value.liverConditions + item.trim()) }
    fun removeLiverCondition(item: String)   { _profile.value = _profile.value.copy(liverConditions = _profile.value.liverConditions - item) }
    fun addOtherMedicine(item: String)       { if (item.isNotBlank()) _profile.value = _profile.value.copy(otherMedicines = _profile.value.otherMedicines + item.trim()) }
    fun removeOtherMedicine(item: String)    { _profile.value = _profile.value.copy(otherMedicines = _profile.value.otherMedicines - item) }

    fun save() {
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            val result = profileRepository.upsertProfile(_profile.value)
            _saveState.value = when (result) {
                is AppResult.Success -> ProfileSaveState.Success
                is AppResult.Error   -> ProfileSaveState.Error(result.message)
                else                 -> ProfileSaveState.Error("Unknown error")
            }
        }
    }

    fun clearSaveState() { _saveState.value = ProfileSaveState.Idle }
}
