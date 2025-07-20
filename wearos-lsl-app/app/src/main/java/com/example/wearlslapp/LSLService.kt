package com.example.wearlslapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import edu.ucsd.sccn.LSL
import java.util.concurrent.atomic.AtomicBoolean

class LSLService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var vibrator: Vibrator
    private lateinit var sensorManager: SensorManager

    // LSL outlets
    private var imuOutlet: LSL.StreamOutlet? = null
    private var hrOutlet: LSL.StreamOutlet? = null

    private val running = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startForegroundService()
        initLSLStreams()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) {
            startListeningSensor()
            startPatternListener()
        }
        return START_STICKY
    }

    // Foreground notification
    private fun startForegroundService() {
        val channelId = "wearlsl_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Wear LSL Service", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WearLSL Active")
            .setContentText("Streaming sensors via LSL")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notification)
    }

    private fun initLSLStreams() {
        // IMU: accel(x,y,z) + gyro(x,y,z) => 6 channels float32
        val imuInfo = LSL.StreamInfo(
            "wear_imu",
            "IMU",
            6,
            LSL.IRREGULAR_RATE,
            LSL.ChannelFormat.float32,
            "wear_imu_${Build.MODEL}"
        )
        imuOutlet = LSL.StreamOutlet(imuInfo)

        // Heart rate: bpm float32 single channel
        val hrInfo = LSL.StreamInfo(
            "wear_hr",
            "HeartRate",
            1,
            LSL.IRREGULAR_RATE,
            LSL.ChannelFormat.float32,
            "wear_hr_${Build.MODEL}"
        )
        hrOutlet = LSL.StreamOutlet(hrInfo)
    }

    private fun startListeningSensor() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val heart = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        // Sensor delay UI is fine; change if you need higher rate
        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        heart?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun startPatternListener() {
        serviceScope.launch {
            // Resolve first available pattern stream of type "Markers"
            val results = LSL.resolve_stream("type", "VibrationPattern", 1, 5.0)
            if (results.isEmpty()) return@launch
            val inlet = LSL.StreamInlet(results[0])
            val str = arrayOf("")
            while (running.get()) {
                val ts = inlet.pull_sample(str, 0.0)
                if (ts != null) {
                    val patternString = str[0]
                    vibrateFromPattern(patternString)
                }
            }
        }
    }

    private fun vibrateFromPattern(patternString: String) {
        if (patternString.isBlank()) return
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        patternString.forEachIndexed { idx, c ->
            val on = c == '1'
            timings.add(100L) // 100 ms per slot
            amplitudes.add(if (on) 255 else 0)
        }
        // Convert to arrays
        val timingsArray = timings.toLongArray()
        val amplitudesArray = amplitudes.toIntArray()
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(timingsArray, amplitudesArray, -1)
        } else {
            // Legacy API; fallback simple vibrate
            null
        }
        if (effect != null) {
            vibrator.vibrate(effect)
        } else {
            // Fallback: simple vibration for sum of on states
            val total = patternString.count { it == '1' } * 100L
            vibrator.vibrate(total)
        }
    }

    // Sensor callbacks
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyro = event.values.clone()
            }
            Sensor.TYPE_HEART_RATE -> {
                hrOutlet?.push_sample(floatArrayOf(event.values[0]))
            }
        }
        maybePushImuSample()
    }

    private var lastAccel: FloatArray? = null
    private var lastGyro: FloatArray? = null

    private fun maybePushImuSample() {
        val a = lastAccel
        val g = lastGyro
        if (a != null && g != null) {
            val sample = floatArrayOf(a[0], a[1], a[2], g[0], g[1], g[2])
            imuOutlet?.push_sample(sample)
            lastAccel = null
            lastGyro = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        running.set(false)
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}