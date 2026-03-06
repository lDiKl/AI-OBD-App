package com.avyrox.service.data.obd.models

/**
 * Engine sensor snapshot captured at the moment a DTC was set.
 * All values are nullable — not all vehicles support all PIDs.
 */
data class FreezeFrameData(
    val engineRpm: Int?,       // revolutions per minute
    val vehicleSpeed: Int?,    // km/h
    val coolantTemp: Int?,     // °C  (raw byte - 40)
    val engineLoad: Int?       // %   (raw byte * 100 / 255)
)
