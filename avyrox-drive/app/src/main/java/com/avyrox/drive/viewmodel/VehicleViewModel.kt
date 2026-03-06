package com.avyrox.drive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.drive.data.vehicle.VehicleEntity
import com.avyrox.drive.data.vehicle.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VehicleUiState {
    object Loading : VehicleUiState()
    data class Ready(
        val vehicles: List<VehicleEntity>,
        val isSyncing: Boolean = false,
        val error: String? = null,
    ) : VehicleUiState()
}

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val repository: VehicleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<VehicleUiState>(VehicleUiState.Loading)
    val state: StateFlow<VehicleUiState> = _state.asStateFlow()

    init {
        // Observe Room — UI always has up-to-date local data
        viewModelScope.launch {
            repository.vehicles.collect { list ->
                val current = _state.value
                _state.value = VehicleUiState.Ready(
                    vehicles = list,
                    isSyncing = current is VehicleUiState.Ready && current.isSyncing,
                )
            }
        }
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            setSync(true)
            val result = repository.sync()
            setSync(false)
            if (result.isFailure) {
                setError("Could not connect to server. Showing cached data.")
            }
        }
    }

    fun addVehicle(make: String, model: String, year: Int, engineType: String?, vin: String?, mileage: Int?) {
        viewModelScope.launch {
            setSync(true)
            val result = repository.addVehicle(make, model, year, engineType, vin, mileage)
            setSync(false)
            if (result.isFailure) {
                setError("Failed to save vehicle. Check your connection.")
            }
        }
    }

    fun deleteVehicle(id: String) {
        viewModelScope.launch {
            repository.deleteVehicle(id)
        }
    }

    fun clearError() {
        val current = _state.value as? VehicleUiState.Ready ?: return
        _state.value = current.copy(error = null)
    }

    private fun setSync(syncing: Boolean) {
        val current = _state.value as? VehicleUiState.Ready ?: return
        _state.value = current.copy(isSyncing = syncing)
    }

    private fun setError(message: String) {
        val current = _state.value as? VehicleUiState.Ready ?: return
        _state.value = current.copy(error = message)
    }
}
