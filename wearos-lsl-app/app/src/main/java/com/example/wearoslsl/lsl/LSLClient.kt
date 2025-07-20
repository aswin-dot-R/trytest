package com.example.wearoslsl.lsl

import android.util.Log
import com.example.wearoslsl.data.LSLMarker
import com.example.wearoslsl.data.LSLStreamInfo
import com.example.wearoslsl.data.SensorData
import com.example.wearoslsl.data.VibrationPattern
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class LSLClient {
    private val TAG = "LSLClient"
    
    // LSL multicast configuration
    private val LSL_MULTICAST_ADDRESS = "239.255.172.215"
    private val LSL_MULTICAST_PORT = 16571
    private val LSL_TCP_PORT = 16572
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Streams for incoming LSL data
    private val _incomingMarkers = MutableSharedFlow<VibrationPattern>()
    val incomingMarkers: Flow<VibrationPattern> = _incomingMarkers.asSharedFlow()
    
    private val isListening = AtomicBoolean(false)
    private var discoveryJob: Job? = null
    private var outletJob: Job? = null
    
    // LSL stream outlets
    private var sensorOutlet: LSLOutlet? = null
    private var heartRateOutlet: LSLOutlet? = null
    
    suspend fun startLSLStreams() {
        try {
            // Create sensor data outlet (IMU data)
            val sensorStreamInfo = LSLStreamInfo(
                name = "OnePlusWatch2R_IMU",
                type = "IMU",
                channelCount = 9, // 3x accelerometer, 3x gyroscope, 3x magnetometer
                sampleRate = 50.0 // 50 Hz sampling rate
            )
            
            // Create heart rate outlet
            val heartRateStreamInfo = LSLStreamInfo(
                name = "OnePlusWatch2R_HeartRate",
                type = "HR",
                channelCount = 1,
                sampleRate = 1.0 // 1 Hz for heart rate
            )
            
            sensorOutlet = LSLOutlet(sensorStreamInfo)
            heartRateOutlet = LSLOutlet(heartRateStreamInfo)
            
            // Start discovery service to listen for incoming streams
            startDiscoveryService()
            
            Log.d(TAG, "LSL streams started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LSL streams", e)
            throw e
        }
    }
    
    suspend fun sendSensorData(sensorData: SensorData) {
        try {
            // Send IMU data
            val imuData = floatArrayOf(
                sensorData.accelerometerX ?: 0f,
                sensorData.accelerometerY ?: 0f,
                sensorData.accelerometerZ ?: 0f,
                sensorData.gyroscopeX ?: 0f,
                sensorData.gyroscopeY ?: 0f,
                sensorData.gyroscopeZ ?: 0f,
                sensorData.magnetometerX ?: 0f,
                sensorData.magnetometerY ?: 0f,
                sensorData.magnetometerZ ?: 0f
            )
            
            sensorOutlet?.pushSample(imuData, sensorData.timestamp)
            
            // Send heart rate data if available
            sensorData.heartRate?.let { hr ->
                heartRateOutlet?.pushSample(floatArrayOf(hr), sensorData.timestamp)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sensor data to LSL", e)
        }
    }
    
    private fun startDiscoveryService() {
        if (isListening.get()) return
        
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            isListening.set(true)
            
            try {
                val socket = DatagramSocket()
                val group = InetAddress.getByName(LSL_MULTICAST_ADDRESS)
                
                // Listen for LSL discovery messages
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                
                while (isListening.get() && !currentCoroutineContext().isActive) {
                    try {
                        socket.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        
                        // Parse LSL discovery message and look for vibration patterns
                        if (message.contains("vibration") || message.contains("marker")) {
                            parseVibrationCommand(message)
                        }
                        
                    } catch (e: SocketTimeoutException) {
                        // Timeout is normal, continue listening
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in discovery service", e)
                    }
                }
                
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start discovery service", e)
            } finally {
                isListening.set(false)
            }
        }
    }
    
    private suspend fun parseVibrationCommand(message: String) {
        try {
            // Simple pattern parsing - looking for patterns like "010101"
            val patternRegex = Regex("pattern[:\"]\\s*([01]+)")
            val match = patternRegex.find(message)
            
            match?.let { matchResult ->
                val pattern = matchResult.groupValues[1]
                val vibrationPattern = VibrationPattern(
                    pattern = pattern,
                    duration = 100, // Default 100ms per pulse
                    intensity = 255 // Default full intensity
                )
                
                _incomingMarkers.emit(vibrationPattern)
                Log.d(TAG, "Received vibration pattern: $pattern")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse vibration command", e)
        }
    }
    
    fun stopLSLStreams() {
        isListening.set(false)
        discoveryJob?.cancel()
        outletJob?.cancel()
        
        sensorOutlet?.close()
        heartRateOutlet?.close()
        
        sensorOutlet = null
        heartRateOutlet = null
        
        Log.d(TAG, "LSL streams stopped")
    }
}

// Simplified LSL Outlet implementation
private class LSLOutlet(private val streamInfo: LSLStreamInfo) {
    private val TAG = "LSLOutlet"
    
    init {
        Log.d(TAG, "Created LSL outlet: ${streamInfo.name}")
    }
    
    fun pushSample(data: FloatArray, timestamp: Long = System.currentTimeMillis()) {
        // In a real implementation, this would use the actual LSL library
        // For now, we'll log the data and simulate LSL streaming
        Log.d(TAG, "Pushing sample to ${streamInfo.name}: ${data.contentToString()}")
        
        // Here you would integrate with the actual LSL Java bindings
        // or implement TCP/UDP streaming to LSL receivers
    }
    
    fun close() {
        Log.d(TAG, "Closed LSL outlet: ${streamInfo.name}")
    }
}