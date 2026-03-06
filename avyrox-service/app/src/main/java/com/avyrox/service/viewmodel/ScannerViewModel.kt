package com.avyrox.service.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.service.data.cases.DiagnosticCaseRepository
import com.avyrox.service.data.local.DiagnosticCaseEntity
import com.avyrox.service.data.obd.ObdConnectionManager
import com.avyrox.service.data.obd.models.ObdScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScannerState {
    object Idle : ScannerState()
    object Connecting : ScannerState()
    object Scanning : ScannerState()
    /** Scan finished — shows results and lets user decide whether to create a case or rescan. */
    data class ScanComplete(val scanResult: ObdScanResult) : ScannerState()
    data class NeedsVehicleInfo(val scanResult: ObdScanResult) : ScannerState()
    object Analyzing : ScannerState()
    data class Done(val case: DiagnosticCaseEntity) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val obdManager: ObdConnectionManager,
    private val caseRepository: DiagnosticCaseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var activeJob: Job? = null

    fun disconnect() {
        activeJob?.cancel()
        activeJob = null
        obdManager.disconnect()
        _isConnected.value = false
        _state.value = ScannerState.Idle
    }

    fun connectBluetooth(address: String) {
        activeJob = viewModelScope.launch {
            _state.value = ScannerState.Connecting
            val result = obdManager.connect(address)
            if (result.isSuccess) {
                _isConnected.value = true
                _state.value = ScannerState.Idle
            } else {
                _state.value = ScannerState.Error("Bluetooth connection failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun connectTcp(host: String, port: Int) {
        activeJob = viewModelScope.launch {
            _state.value = ScannerState.Connecting
            val result = obdManager.connectTcp(host, port)
            if (result.isSuccess) {
                _isConnected.value = true
                _state.value = ScannerState.Idle
            } else {
                _state.value = ScannerState.Error("TCP connection failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun startScan() {
        activeJob = viewModelScope.launch {
            _state.value = ScannerState.Scanning
            try {
                val scanResult = obdManager.runFullScan()
                _state.value = ScannerState.ScanComplete(scanResult)
            } catch (e: Exception) {
                _state.value = ScannerState.Error("Scan failed: ${e.message}")
            }
        }
    }

    fun proceedToCreateCase(scanResult: ObdScanResult) {
        _state.value = ScannerState.NeedsVehicleInfo(scanResult)
    }

    fun backToScanComplete(scanResult: ObdScanResult) {
        _state.value = ScannerState.ScanComplete(scanResult)
    }

    fun submitVehicleInfo(
        scanResult: ObdScanResult,
        make: String,
        model: String,
        year: String,
        plate: String,
        symptoms: String,
    ) {
        viewModelScope.launch {
            _state.value = ScannerState.Analyzing
            val case = caseRepository.analyzeAndCreateCase(
                scanResult = scanResult,
                vehicleMake = make,
                vehicleModel = model,
                vehicleYear = year,
                vehiclePlate = plate,
                symptoms = symptoms,
            )
            _state.value = ScannerState.Done(case)
        }
    }

    fun reset() { _state.value = ScannerState.Idle }

    override fun onCleared() {
        super.onCleared()
        obdManager.disconnect()
        _isConnected.value = false
    }
}
