package com.example.wearlslapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Whether granted or not, start the background service; if not granted, sensors will fail gracefully
            ContextCompat.startForegroundService(this, Intent(this, LSLService::class.java))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val neededPermissions = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.BODY_SENSORS_BACKGROUND,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET
        )

        val allGranted = neededPermissions.all {
            PermissionChecker.checkSelfPermission(this, it) == PermissionChecker.PERMISSION_GRANTED
        }

        if (!allGranted) {
            permissionLauncher.launch(neededPermissions)
        } else {
            ContextCompat.startForegroundService(this, Intent(this, LSLService::class.java))
        }
        finish() // Nothing else to show; run in background.
    }
}