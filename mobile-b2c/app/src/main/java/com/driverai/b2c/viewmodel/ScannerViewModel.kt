package com.driverai.b2c.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverai.b2c.data.obd.ObdConnectionManager
import com.driverai.b2c.data.obd.models.ObdScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the OBD scanner screen */
sealed class ScanState {
    /** Initial state — nothing started yet */
    object Idle : ScanState()

    /** Bluetooth permission is being requested by the UI */
    object RequestingPermission : ScanState()

    /** User should pick one of these paired devices */
    data class SelectingDevice(val pairedDevices: List<BluetoothDevice>) : ScanState()

    /** Socket is being opened and ELM327 init is running */
    object Connecting : ScanState()

    /** OBD commands are being sent (Mode 03, Mode 02) */
    object Scanning : ScanState()

    /** Scan finished successfully */
    data class Success(val result: ObdScanResult) : ScanState()

    /** Something went wrong at any step */
    data class Error(val message: String) : ScanState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val obdConnectionManager: ObdConnectionManager
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /**
     * Entry point — called when the user taps "Start OBD Scan".
     * Checks Bluetooth availability and lists paired devices.
     * The UI is responsible for requesting BLUETOOTH_CONNECT permission
     * before calling this, or calling [onPermissionDenied] if denied.
     */
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

    /**
     * Called when the user taps a device in the list.
     * Opens a BT connection then immediately runs the full OBD scan.
     */
    fun onDeviceSelected(deviceAddress: String) {
        viewModelScope.launch {
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

    /** Called when the user taps "Try Again" from the Error state */
    fun onRetry() {
        _state.value = ScanState.Idle
    }

    /** Called when the user cancels device selection */
    fun onCancelSelection() {
        _state.value = ScanState.Idle
    }

    fun onPermissionDenied() {
        _state.value = ScanState.Error("Bluetooth permission is required to connect to the OBD adapter")
    }

    override fun onCleared() {
        super.onCleared()
        obdConnectionManager.disconnect()
    }
}
