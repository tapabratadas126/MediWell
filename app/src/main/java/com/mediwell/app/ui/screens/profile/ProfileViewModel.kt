package com.mediwell.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.AllergyRecord
import com.mediwell.app.data.model.PatientProfile
import com.mediwell.app.data.repository.ProfileRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ProfileSaveState {
    object Idle    : ProfileSaveState()
    object Saving  : ProfileSaveState()
    data class Success(val message: String = "Profile saved") : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _profile   = MutableStateFlow(PatientProfile(userId = userId))
    val profile: StateFlow<PatientProfile> = _profile.asStateFlow()

    private val _allergies = MutableStateFlow<List<AllergyRecord>>(emptyList())
    val allergies: StateFlow<List<AllergyRecord>> = _allergies.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val profileResult = profileRepository.fetchProfile(userId)
            if (profileResult is AppResult.Success && profileResult.data != null) {
                _profile.value = profileResult.data
            } else {
                _profile.value = PatientProfile(userId = userId)
            }

            val allergyResult = profileRepository.fetchAllergies(userId)
            if (allergyResult is AppResult.Success) {
                _allergies.value = allergyResult.data
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

    fun saveAllergy(existingId: String?, allergenName: String, reaction: String, severity: String) {
        val cleanName = allergenName.trim()
        if (cleanName.isBlank()) {
            _saveState.value = ProfileSaveState.Error("Please enter an allergen name.")
            return
        }
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            val record = AllergyRecord(
                id = existingId?.ifBlank { null } ?: UUID.randomUUID().toString(),
                userId = userId,
                allergenName = cleanName,
                reaction = reaction.trim().ifBlank { null },
                severity = severity.ifBlank { "Not specified" }
            )
            when (val result = profileRepository.upsertAllergy(record)) {
                is AppResult.Success -> {
                    val updated = _allergies.value.toMutableList()
                    val idx = updated.indexOfFirst { it.id == result.data.id }
                    if (idx >= 0) updated[idx] = result.data else updated.add(result.data)
                    _allergies.value = updated
                    _profile.value = _profile.value.copy(allergies = updated.map { it.allergenName })
                    _saveState.value = ProfileSaveState.Success("Allergy saved to your profile")
                }
                is AppResult.Error -> {
                    _saveState.value = ProfileSaveState.Error(result.message)
                }
                else -> Unit
            }
        }
    }

    fun deleteAllergy(record: AllergyRecord) {
        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            when (val result = profileRepository.deleteAllergy(record.id, userId)) {
                is AppResult.Success -> {
                    val updated = _allergies.value.filterNot { it.id == record.id }
                    _allergies.value = updated
                    _profile.value = _profile.value.copy(allergies = updated.map { it.allergenName })
                    _saveState.value = ProfileSaveState.Success("Allergy removed")
                }
                is AppResult.Error -> {
                    _saveState.value = ProfileSaveState.Error(result.message)
                }
                else -> Unit
            }
        }
    }

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
            val profileToSave = _profile.value.copy(
                allergies = _allergies.value.map { it.allergenName }
            )
            val result = profileRepository.upsertProfile(profileToSave)
            _saveState.value = when (result) {
                is AppResult.Success -> ProfileSaveState.Success("Profile saved successfully")
                is AppResult.Error   -> ProfileSaveState.Error(result.message)
                else                 -> ProfileSaveState.Error("Unknown error")
            }
        }
    }

    fun clearSaveState() { _saveState.value = ProfileSaveState.Idle }
}
