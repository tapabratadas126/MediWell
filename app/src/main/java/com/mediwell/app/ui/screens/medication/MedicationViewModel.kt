package com.mediwell.app.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediwell.app.data.model.MedicationInputForm
import com.mediwell.app.data.model.MedicationItem
import com.mediwell.app.data.repository.MedicationRepository
import com.mediwell.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MedicationEntryState {
    data object Idle : MedicationEntryState()
    data object Normalizing : MedicationEntryState()
    data class Success(val message: String) : MedicationEntryState()
    data class Error(val message: String) : MedicationEntryState()
}

class MedicationViewModel(
    private val repository: MedicationRepository,
    private val userId: String
) : ViewModel() {

    val medications: StateFlow<List<MedicationItem>> = repository.medications

    private val _entryState = MutableStateFlow<MedicationEntryState>(MedicationEntryState.Idle)
    val entryState: StateFlow<MedicationEntryState> = _entryState.asStateFlow()

    private val _form = MutableStateFlow(MedicationInputForm())
    val form: StateFlow<MedicationInputForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadMedications(userId)
        }
    }

    fun updateName(name: String) {
        _form.value = _form.value.copy(name = name)
    }

    fun updateDose(dose: String) {
        _form.value = _form.value.copy(dose = dose)
    }

    fun updateFrequency(frequency: String) {
        _form.value = _form.value.copy(frequency = frequency)
    }

    fun updateRoute(route: String) {
        _form.value = _form.value.copy(route = route)
    }

    fun setQuickFill(name: String, dose: String, freq: String = "Once daily", route: String = "Oral") {
        _form.value = MedicationInputForm(name = name, dose = dose, frequency = freq, route = route)
    }

    fun addMedication() {
        val currentForm = _form.value
        if (!currentForm.isValid) {
            _entryState.value = MedicationEntryState.Error("Please enter a valid medication name")
            return
        }

        viewModelScope.launch {
            _entryState.value = MedicationEntryState.Normalizing
            when (val result = repository.addMedication(userId, currentForm)) {
                is AppResult.Success -> {
                    _entryState.value = MedicationEntryState.Success("Medication normalized and added")
                    // Reset form
                    _form.value = MedicationInputForm()
                }
                is AppResult.Error -> {
                    _entryState.value = MedicationEntryState.Error(result.message)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun clearState() {
        _entryState.value = MedicationEntryState.Idle
    }
}
