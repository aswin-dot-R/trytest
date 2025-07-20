package com.example.wearoslsl.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import com.example.wearoslsl.R
import com.example.wearoslsl.data.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

class SensorDataService : Service(), SensorEventListener {
    
    private val TAG = "SensorDataService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "sensor_data_channel"
    
    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock
    
    // Health Services
    private val healthClient by lazy { HealthServices.getClient(this) }
    private val measureClient by lazy { healthClient.measureClient }
    
    // Sensors
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    
    // Data streams
    private val _sensorDataFlow = MutableSharedFlow<SensorData>()
    val sensorDataFlow: Flow<SensorData> = _sensorDataFlow.asSharedFlow()
    
    // Current sensor values
    private var currentAccel = FloatArray(3) { 0f }
    private var currentGyro = FloatArray(3) { 0f }
    private var currentMag = FloatArray(3) { 0f }
    private var currentHeartRate = 0f
    
    private val isCollecting = AtomicBoolean(false)
    private var serviceScope: CoroutineScope? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): SensorDataService = this@SensorDataService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Get sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        // Acquire wake lock to keep sensors active
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearOSLSL::SensorDataService"
        )
        
        createNotificationChannel()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        Log.d(TAG, "SensorDataService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    fun startSensorCollection() {
        if (isCollecting.get()) return
        
        isCollecting.set(true)
        wakeLock.acquire(10*60*1000L /*10 minutes*/)
        
        // Register traditional sensors
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        
        // Start heart rate monitoring using Health Services
        startHeartRateMonitoring()
        
        // Start data emission coroutine
        serviceScope?.launch {
            while (isCollecting.get()) {
                val sensorData = SensorData(
                    timestamp = System.currentTimeMillis(),
                    heartRate = if (currentHeartRate > 0) currentHeartRate else null,
                    accelerometerX = currentAccel[0],
                    accelerometerY = currentAccel[1],
                    accelerometerZ = currentAccel[2],
                    gyroscopeX = currentGyro[0],
                    gyroscopeY = currentGyro[1],
                    gyroscopeZ = currentGyro[2],
                    magnetometerX = currentMag[0],
                    magnetometerY = currentMag[1],
                    magnetometerZ = currentMag[2]
                )
                
                _sensorDataFlow.emit(sensorData)
                delay(20) // 50 Hz data emission
            }
        }
        
        Log.d(TAG, "Sensor collection started")
    }
    
    fun stopSensorCollection() {
        if (!isCollecting.get()) return
        
        isCollecting.set(false)
        sensorManager.unregisterListener(this)
        stopHeartRateMonitoring()
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        Log.d(TAG, "Sensor collection stopped")
    }
    
    private fun startHeartRateMonitoring() {
        serviceScope?.launch {
            try {
                val capabilities = measureClient.getCapabilitiesAsync().await()
                
                if (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure) {
                    val callback = object : MeasureCallback {
                        override fun onAvailabilityChanged(
                            dataType: DataType<*, *>,
                            availability: Availability
                        ) {
                            Log.d(TAG, "Heart rate availability: $availability")
                        }
                        
                        override fun onDataReceived(data: DataPointContainer) {
                            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                            heartRateData.forEach { dataPoint ->
                                currentHeartRate = dataPoint.value
                                Log.d(TAG, "Heart rate: $currentHeartRate BPM")
                            }
                        }
                    }
                    
                    measureClient.registerMeasureCallback(
                        DataType.HEART_RATE_BPM,
                        callback
                    )
                    
                    Log.d(TAG, "Heart rate monitoring started")
                } else {
                    Log.w(TAG, "Heart rate monitoring not supported on this device")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start heart rate monitoring", e)
            }
        }
    }
    
    private fun stopHeartRateMonitoring() {
        try {
            measureClient.unregisterMeasureCallbackAsync(
                DataType.HEART_RATE_BPM,
                object : MeasureCallback {
                    override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
                    override fun onDataReceived(data: DataPointContainer) {}
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop heart rate monitoring", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    currentAccel = sensorEvent.values.copyOf()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    currentGyro = sensorEvent.values.copyOf()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    currentMag = sensorEvent.values.copyOf()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sensor Data Collection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Collects sensor data for LSL streaming"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WearOS LSL")
            .setContentText("Collecting sensor data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSensorCollection()
        serviceScope?.cancel()
        Log.d(TAG, "SensorDataService destroyed")
    }
}