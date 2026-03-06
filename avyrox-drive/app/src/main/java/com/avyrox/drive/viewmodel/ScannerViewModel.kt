package com.avyrox.drive.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avyrox.drive.data.network.ScanAnalyzeResponse
import com.avyrox.drive.data.obd.ObdConnectionManager
import com.avyrox.drive.data.obd.models.ObdScanResult
import com.avyrox.drive.data.scan.ScanRepository
import com.avyrox.drive.data.vehicle.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanState {
    object Idle : ScanState()
    object RequestingPermission : ScanState()
    data class SelectingDevice(val pairedDevices: List<BluetoothDevice>) : ScanState()
    object Connecting : ScanState()
    object Scanning : ScanState()
    /** OBD scan done — user can now trigger AI analysis */
    data class Success(val result: ObdScanResult) : ScanState()
    /** Backend API call in progress */
    object Analyzing : ScanState()
    /** Full analysis from backend received */
    data class AnalysisReady(val analysis: ScanAnalyzeResponse) : ScanState()
    data class Error(val message: String) : ScanState()
}

private const val EMULATOR_HOST = "192.168.0.174"  // PC Wi-Fi IP — change if needed
private const val EMULATOR_PORT = 35000

private enum class ConnectionType { NONE, BLUETOOTH, TCP }

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val obdConnectionManager: ObdConnectionManager,
    private val scanRepository: ScanRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var lastConnectionType = ConnectionType.NONE
    private var lastDeviceAddress = ""
    private var scanJob: Job? = null

    fun onCancel() {
        scanJob?.cancel()
        scanJob = null
        obdConnectionManager.disconnect()
        _state.value = ScanState.Idle
    }

    @SuppressLint("MissingPermission")
    fun onStartScan() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            _state.value = ScanState.Error("This device does not support Bluetooth")
            return
        }
        if (!adapter.isEnabled) {
            _state.value = ScanState.Error("Please enable Bluetooth and try again")
            return
        }
        val paired = adapter.bondedDevices.toList()
        if (paired.isEmpty()) {
            _state.value = ScanState.Error("No paired Bluetooth devices found.\nPair your ELM327 adapter in Android Bluetooth settings first.")
            return
        }
        _state.value = ScanState.SelectingDevice(paired)
    }

    fun onDeviceSelected(deviceAddress: String) {
        lastConnectionType = ConnectionType.BLUETOOTH
        lastDeviceAddress = deviceAddress
        scanJob = viewModelScope.launch {
            _state.value = ScanState.Connecting
            val connectResult = obdConnectionManager.connect(deviceAddress)
            if (connectResult.isFailure) {
                _state.value = ScanState.Error(
                    "Connection failed: ${connectResult.exceptionOrNull()?.message ?: "unknown error"}"
                )
                return@launch
            }
            _state.value = ScanState.Scanning
            try {
                val result = obdConnectionManager.runFullScan()
                _state.value = ScanState.Success(result)
            } catch (e: Exception) {
                _state.value = ScanState.Error("Scan failed: ${e.message ?: "unknown error"}")
            } finally {
                obdConnectionManager.disconnect()
            }
        }
    }

    /**
     * Called after OBD scan — sends result to backend for AI analysis.
     * Uses the first registered vehicle. If none registered, shows error.
     */
    fun onAnalyzeWithAI() {
        val obdResult = (state.value as? ScanState.Success)?.result ?: return
        viewModelScope.launch {
            _state.value = ScanState.Analyzing

            val vehicles = vehicleRepository.vehicles.first()
            if (vehicles.isEmpty()) {
                _state.value = ScanState.Error("Please add a vehicle in the \"My Cars\" tab first")
                return@launch
            }

            val vehicle = vehicles.first()
            val result = scanRepository.analyzeOBDResult(
                obdResult = obdResult,
                vehicle = vehicle,
            )

            result.fold(
                onSuccess = { _state.value = ScanState.AnalysisReady(it) },
                onFailure = { _state.value = ScanState.Error("Analysis failed: ${it.localizedMessage}") },
            )
        }
    }

    /** Connect to local TCP emulator (debug only). Phone and PC must be on same Wi-Fi. */
    fun onConnectToEmulator() {
        lastConnectionType = ConnectionType.TCP
        scanJob = viewModelScope.launch {
            _state.value = ScanState.Connecting
            val result = obdConnectionManager.connectTcp(EMULATOR_HOST, EMULATOR_PORT)
            if (result.isFailure) {
                _state.value = ScanState.Error(
                    "Emulator connection failed: ${result.exceptionOrNull()?.message}\n" +
                    "Make sure Docker emulator is running and phone is on same Wi-Fi as PC."
                )
                return@launch
            }
            _state.value = ScanState.Scanning
            try {
                val scanResult = obdConnectionManager.runFullScan()
                _state.value = ScanState.Success(scanResult)
            } catch (e: Exception) {
                _state.value = ScanState.Error("Scan failed: ${e.message}")
            } finally {
                obdConnectionManager.disconnect()
            }
        }
    }

    /** Re-scan using the same connection as before (Bluetooth or TCP emulator). */
    fun onScanAgain() {
        when (lastConnectionType) {
            ConnectionType.BLUETOOTH -> onDeviceSelected(lastDeviceAddress)
            ConnectionType.TCP       -> onConnectToEmulator()
            ConnectionType.NONE      -> _state.value = ScanState.Idle
        }
    }

    fun onRetry() { _state.value = ScanState.Idle }
    fun onCancelSelection() { _state.value = ScanState.Idle }
    fun onPermissionDenied() {
        _state.value = ScanState.Error("Bluetooth permission is required to connect to the OBD adapter")
    }

    override fun onCleared() {
        super.onCleared()
        obdConnectionManager.disconnect()
    }
}
