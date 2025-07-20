package com.example.wearoslsl.data

import kotlinx.serialization.Serializable

@Serializable
data class SensorData(
    val timestamp: Long,
    val heartRate: Float? = null,
    val accelerometerX: Float? = null,
    val accelerometerY: Float? = null,
    val accelerometerZ: Float? = null,
    val gyroscopeX: Float? = null,
    val gyroscopeY: Float? = null,
    val gyroscopeZ: Float? = null,
    val magnetometerX: Float? = null,
    val magnetometerY: Float? = null,
    val magnetometerZ: Float? = null
)

@Serializable
data class LSLMarker(
    val timestamp: Long,
    val marker: String,
    val value: Int = 1
)

@Serializable
data class VibrationPattern(
    val pattern: String, // e.g., "010101"
    val duration: Long = 100, // milliseconds per pulse
    val intensity: Int = 255 // vibration intensity (0-255)
)

data class LSLStreamInfo(
    val name: String,
    val type: String,
    val channelCount: Int,
    val sampleRate: Double,
    val format: String = "float32",
    val sourceId: String = "WearOS_OnePlus_Watch_2R"
)