package com.driverai.b2c.data.obd

/**
 * ELM327 AT command constants and initialization sequence.
 *
 * The ELM327 communicates over Classic Bluetooth SPP (Serial Port Profile).
 * Every command ends with carriage return '\r'. The adapter signals readiness with '>'.
 */
object ElmProtocol {
    /** UUID for Bluetooth Serial Port Profile (SPP) — used by all ELM327 adapters */
    const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    // --- AT commands ---
    const val RESET          = "AT Z\r"    // warm reset, clears state
    const val ECHO_OFF       = "AT E0\r"   // disable command echo
    const val LINEFEEDS_OFF  = "AT L0\r"   // disable line feed chars
    const val SPACES_OFF     = "AT S0\r"   // remove spaces from response bytes
    const val HEADERS_OFF    = "AT H0\r"   // hide CAN/ISO header bytes
    const val AUTO_PROTOCOL  = "AT SP 0\r" // auto-detect OBD protocol

    /** Commands sent in order on every new connection */
    val INIT_SEQUENCE = listOf(
        RESET,
        ECHO_OFF,
        LINEFEEDS_OFF,
        SPACES_OFF,
        HEADERS_OFF,
        AUTO_PROTOCOL
    )

    /** ELM327 prompt character — marks end of response */
    const val PROMPT = '>'

    /** Max time (ms) to wait for a response before timing out */
    const val COMMAND_TIMEOUT_MS = 5_000L

    /** Extra delay after AT Z reset — adapter needs time to boot */
    const val RESET_DELAY_MS = 1_500L

    /** "NO DATA" response — vehicle doesn't support the requested PID */
    const val NO_DATA = "NODATA"

    /** "ERROR" and "?" responses indicate unsupported command */
    val ERROR_RESPONSES = setOf("ERROR", "?", "UNABLE TO CONNECT", "BUS INIT: ERROR")
}
