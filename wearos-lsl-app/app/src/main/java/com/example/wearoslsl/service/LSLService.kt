package com.example.wearoslsl.service

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wearoslsl.R
import com.example.wearoslsl.lsl.LSLClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicBoolean

class LSLService : Service() {
    
    private val TAG = "LSLService"
    private val NOTIFICATION_ID = 1002
    private val CHANNEL_ID = "lsl_service_channel"
    
    private val binder = LocalBinder()
    private lateinit var lslClient: LSLClient
    
    private var sensorService: SensorDataService? = null
    private var isSensorServiceBound = false
    
    private val isActive = AtomicBoolean(false)
    private var serviceScope: CoroutineScope? = null
    
    private val sensorServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SensorDataService.LocalBinder
            sensorService = binder.getService()
            isSensorServiceBound = true
            
            // Start listening to sensor data and forward to LSL
            startDataForwarding()
            
            Log.d(TAG, "Connected to sensor service")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
            isSensorServiceBound = false
            Log.d(TAG, "Disconnected from sensor service")
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): LSLService = this@LSLService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        lslClient = LSLClient()
        createNotificationChannel()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        Log.d(TAG, "LSLService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    fun startLSLStreaming() {
        if (isActive.get()) return
        
        serviceScope?.launch {
            try {
                // Start LSL streams
                lslClient.startLSLStreams()
                
                // Bind to sensor service to get data
                val intent = Intent(this@LSLService, SensorDataService::class.java)
                bindService(intent, sensorServiceConnection, Context.BIND_AUTO_CREATE)
                
                isActive.set(true)
                Log.d(TAG, "LSL streaming started")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LSL streaming", e)
            }
        }
    }
    
    fun stopLSLStreaming() {
        if (!isActive.get()) return
        
        isActive.set(false)
        
        // Unbind from sensor service
        if (isSensorServiceBound) {
            unbindService(sensorServiceConnection)
            isSensorServiceBound = false
        }
        
        // Stop LSL streams
        lslClient.stopLSLStreams()
        
        Log.d(TAG, "LSL streaming stopped")
    }
    
    private fun startDataForwarding() {
        serviceScope?.launch {
            try {
                sensorService?.sensorDataFlow?.collectLatest { sensorData ->
                    if (isActive.get()) {
                        lslClient.sendSensorData(sensorData)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in data forwarding", e)
            }
        }
    }
    
    fun getLSLClient(): LSLClient = lslClient
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LSL Streaming Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Streams sensor data via LSL"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WearOS LSL")
            .setContentText("Streaming data via LSL...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLSLStreaming()
        serviceScope?.cancel()
        Log.d(TAG, "LSLService destroyed")
    }
}