package com.shopai.b2b.data.obd

/**
 * ELM327 AT command constants and initialization sequence.
 */
object ElmProtocol {
    const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    const val RESET          = "AT Z\r"
    const val ECHO_OFF       = "AT E0\r"
    const val LINEFEEDS_OFF  = "AT L0\r"
    const val SPACES_OFF     = "AT S0\r"
    const val HEADERS_OFF    = "AT H0\r"
    const val AUTO_PROTOCOL  = "AT SP 0\r"

    val INIT_SEQUENCE = listOf(
        RESET, ECHO_OFF, LINEFEEDS_OFF, SPACES_OFF, HEADERS_OFF, AUTO_PROTOCOL
    )

    const val PROMPT = '>'
    const val COMMAND_TIMEOUT_MS = 5_000L
    const val RESET_DELAY_MS = 1_500L
    const val NO_DATA = "NODATA"
    val ERROR_RESPONSES = setOf("ERROR", "?", "UNABLE TO CONNECT", "BUS INIT: ERROR")
}
