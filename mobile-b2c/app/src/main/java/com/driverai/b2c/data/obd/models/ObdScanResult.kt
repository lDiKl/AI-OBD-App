package com.driverai.b2c.data.obd.models

data class ObdScanResult(
    val dtcCodes: List<DtcCode>,
    val freezeFrame: FreezeFrameData?,
    val scannedAt: Long = System.currentTimeMillis()
) {
    val hasErrors: Boolean get() = dtcCodes.isNotEmpty()
}
