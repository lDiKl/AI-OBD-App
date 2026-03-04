package com.shopai.b2b.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.shopai.b2b.data.obd.models.FreezeFrameData
import com.shopai.b2b.data.obd.models.ObdScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ObdConnectionManager"

@Singleton
class ObdConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var socket: BluetoothSocket? = null
    private var tcpSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth not supported"))
            if (!bluetoothAdapter.isEnabled)
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled"))

            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val uuid = UUID.fromString(ElmProtocol.SPP_UUID)
            Log.d(TAG, "Connecting to $deviceAddress…")
            val newSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            newSocket.connect()
            socket = newSocket
            outputStream = newSocket.outputStream
            inputStream = newSocket.inputStream
            Log.d(TAG, "Connected. Running ELM327 init…")
            initialize()
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun runFullScan(): ObdScanResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting full scan…")
        val dtcRaw = sendCommand(ObdCommands.GET_DTCS)
        val dtcCodes = ObdResponseParser.parseDtcCodes(dtcRaw)
        Log.d(TAG, "DTCs: ${dtcCodes.size} — $dtcCodes")

        val freezeFrame: FreezeFrameData? = if (dtcCodes.isNotEmpty()) {
            val responses = mutableMapOf<String, String>()
            for ((key, command) in ObdCommands.FREEZE_FRAME_COMMANDS) {
                responses[key] = sendCommand(command)
            }
            ObdResponseParser.parseFreezeFrame(responses)
        } else null

        ObdScanResult(dtcCodes = dtcCodes, freezeFrame = freezeFrame)
    }

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IOException("Not connected")
        val input = inputStream ?: throw IOException("Not connected")
        Log.d(TAG, ">> ${command.trim()}")
        out.write(command.toByteArray())
        out.flush()

        val response = StringBuilder()
        val deadline = System.currentTimeMillis() + ElmProtocol.COMMAND_TIMEOUT_MS
        val buffer = ByteArray(1)
        while (System.currentTimeMillis() < deadline) {
            if (input.available() > 0) {
                val bytesRead = input.read(buffer)
                if (bytesRead > 0) {
                    val ch = buffer[0].toInt().toChar()
                    if (ch == ElmProtocol.PROMPT) break
                    response.append(ch)
                }
            } else {
                delay(10)
            }
        }

        val result = response.toString().replace("\r", "").replace("\n", "").trim()
        Log.d(TAG, "<< $result")
        result
    }

    suspend fun connectTcp(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            Log.d(TAG, "Connecting TCP $host:$port…")
            val newSocket = Socket(host, port)
            tcpSocket = newSocket
            outputStream = newSocket.getOutputStream()
            inputStream = newSocket.getInputStream()
            initialize()
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "TCP failed: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            outputStream?.close(); inputStream?.close()
            socket?.close(); tcpSocket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Error closing: ${e.message}")
        } finally {
            outputStream = null; inputStream = null; socket = null; tcpSocket = null
        }
    }

    val isConnected: Boolean get() = socket?.isConnected == true || tcpSocket?.isConnected == true

    private suspend fun initialize() {
        for (command in ElmProtocol.INIT_SEQUENCE) {
            sendCommand(command)
            if (command == ElmProtocol.RESET) delay(ElmProtocol.RESET_DELAY_MS)
        }
    }
}
