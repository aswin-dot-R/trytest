package com.example.wearoslsl.service

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.wearoslsl.data.VibrationPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VibrationService(private val context: Context) {
    
    private val TAG = "VibrationService"
    private val vibrator: Vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun executeVibrationPattern(pattern: VibrationPattern) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Executing vibration pattern: ${pattern.pattern}")
                
                for (char in pattern.pattern) {
                    when (char) {
                        '1' -> {
                            // Vibrate
                            vibrate(pattern.duration, pattern.intensity)
                        }
                        '0' -> {
                            // Pause
                            delay(pattern.duration)
                        }
                    }
                }
                
                Log.d(TAG, "Vibration pattern completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing vibration pattern", e)
            }
        }
    }
    
    private suspend fun vibrate(duration: Long, intensity: Int) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Use VibrationEffect for API 26+
                val amplitude = (intensity * 255 / 100).coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(duration, amplitude)
                vibrator.vibrate(effect)
            } else {
                // Fallback for older APIs
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
            
            // Wait for vibration to complete
            delay(duration)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during vibration", e)
        }
    }
    
    fun stopVibration() {
        try {
            vibrator.cancel()
            Log.d(TAG, "Vibration stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }
    
    fun testVibration() {
        executeVibrationPattern(
            VibrationPattern(
                pattern = "101010",
                duration = 200,
                intensity = 150
            )
        )
    }
}