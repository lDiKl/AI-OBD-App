package com.avyrox.drive.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.avyrox.drive.data.obd.models.FreezeFrameData
import com.avyrox.drive.data.obd.models.ObdScanResult
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

/**
 * Manages the full lifecycle of an ELM327 Bluetooth connection:
 *   1. Open Classic BT socket (SPP)
 *   2. Run ELM327 initialization sequence
 *   3. Send OBD-II commands and parse responses
 *   4. Close socket when done
 *
 * All I/O runs on Dispatchers.IO via withContext.
 * Annotated @Singleton so a single instance is shared across the app.
 */
@Singleton
class ObdConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var socket: BluetoothSocket? = null
    private var tcpSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    /**
     * Connect to an ELM327 adapter by its Bluetooth MAC address.
     * The device must already be paired in Android Bluetooth settings.
     *
     * @return Result.success(Unit) on success, Result.failure(exception) on error
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect() // close any previous connection first

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth not supported on this device"))

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled"))
            }

            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val uuid = UUID.fromString(ElmProtocol.SPP_UUID)

            Log.d(TAG, "Connecting to $deviceAddress…")
            val newSocket = device.createRfcommSocketToServiceRecord(uuid)

            // Cancel BT discovery before connecting — it slows down the connection
            bluetoothAdapter.cancelDiscovery()

            newSocket.connect()

            socket = newSocket
            outputStream = newSocket.outputStream
            inputStream = newSocket.inputStream

            Log.d(TAG, "Connected. Running ELM327 init sequence…")
            initialize()

            Log.d(TAG, "Init complete.")
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * Run a full diagnostic scan:
     *   1. Read stored DTCs (Mode 03)
     *   2. If codes found, read freeze frame data (Mode 02)
     *
     * Must be called after [connect] succeeds.
     */
    suspend fun runFullScan(): ObdScanResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting full scan…")

        // Step 1: Read DTCs
        val dtcRaw = sendCommand(ObdCommands.GET_DTCS)
        val dtcCodes = ObdResponseParser.parseDtcCodes(dtcRaw)
        Log.d(TAG, "DTCs found: ${dtcCodes.size} — $dtcCodes")

        // Step 2: Read freeze frame only if there are codes
        val freezeFrame: FreezeFrameData? = if (dtcCodes.isNotEmpty()) {
            val responses = mutableMapOf<String, String>()
            for ((key, command) in ObdCommands.FREEZE_FRAME_COMMANDS) {
                responses[key] = sendCommand(command)
            }
            ObdResponseParser.parseFreezeFrame(responses)
        } else null

        ObdScanResult(dtcCodes = dtcCodes, freezeFrame = freezeFrame)
    }

    /**
     * Send a single OBD/AT command and return the raw response string.
     * Waits for the ELM327 prompt character '>' before returning.
     * Strips the prompt and trims whitespace.
     */
    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IOException("Not connected")
        val input = inputStream ?: throw IOException("Not connected")

        Log.d(TAG, ">> $command".trim())
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
                delay(10) // yield briefly to avoid busy-loop
            }
        }

        val result = response.toString()
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        Log.d(TAG, "<< $result")
        result
    }

    /**
     * Connect to the TCP emulator (debug only).
     * Phone and PC must be on the same Wi-Fi network.
     */
    suspend fun connectTcp(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            Log.d(TAG, "Connecting to TCP emulator $host:$port…")
            val newSocket = Socket(host, port)
            tcpSocket = newSocket
            outputStream = newSocket.getOutputStream()
            inputStream = newSocket.getInputStream()
            Log.d(TAG, "TCP connected. Running ELM327 init…")
            initialize()
            Log.d(TAG, "Init complete.")
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "TCP connection failed: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    /** Close the Bluetooth/TCP socket and release all streams. */
    fun disconnect() {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
            tcpSocket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Error closing socket: ${e.message}")
        } finally {
            outputStream = null
            inputStream = null
            socket = null
            tcpSocket = null
        }
    }

    val isConnected: Boolean get() = socket?.isConnected == true || tcpSocket?.isConnected == true

    /**
     * Run the ELM327 initialization sequence.
     * Sends each AT command in order, waits for response.
     * After AT Z (reset) there is an extra delay for the adapter to boot.
     */
    private suspend fun initialize() {
        for (command in ElmProtocol.INIT_SEQUENCE) {
            sendCommand(command)
            if (command == ElmProtocol.RESET) {
                delay(ElmProtocol.RESET_DELAY_MS)
            }
        }
    }
}
