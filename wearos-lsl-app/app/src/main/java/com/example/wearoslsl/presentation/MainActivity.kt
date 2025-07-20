package com.example.wearoslsl.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.wearoslsl.lsl.LSLClient
import com.example.wearoslsl.service.SensorDataService
import com.example.wearoslsl.service.VibrationService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private lateinit var lslClient: LSLClient
    private lateinit var vibrationService: VibrationService
    
    private var sensorService: SensorDataService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SensorDataService.LocalBinder
            sensorService = binder.getService()
            isServiceBound = true
            Log.d(TAG, "Sensor service connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
            isServiceBound = false
            Log.d(TAG, "Sensor service disconnected")
        }
    }
    
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lslClient = LSLClient()
        vibrationService = VibrationService(this)
        
        setContent {
            WearOSLSLApp(
                lslClient = lslClient,
                vibrationService = vibrationService,
                onStartSensors = { startSensorService() },
                onStopSensors = { stopSensorService() }
            )
        }
    }
    
    private fun startSensorService() {
        val intent = Intent(this, SensorDataService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun stopSensorService() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        
        val intent = Intent(this, SensorDataService::class.java)
        stopService(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSensorService()
        lslClient.stopLSLStreams()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WearOSLSLApp(
    lslClient: LSLClient,
    vibrationService: VibrationService,
    onStartSensors: () -> Unit,
    onStopSensors: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State management
    var isLSLActive by remember { mutableStateOf(false) }
    var isSensorActive by remember { mutableStateOf(false) }
    var lastHeartRate by remember { mutableStateOf("--") }
    var statusMessage by remember { mutableStateOf("Ready to start") }
    
    // Permission handling
    val permissions = remember {
        listOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK
        )
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    // Navigation
    val navController = rememberSwipeDismissableNavController()
    
    MaterialTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(
                    permissionsState = permissionsState,
                    isLSLActive = isLSLActive,
                    isSensorActive = isSensorActive,
                    lastHeartRate = lastHeartRate,
                    statusMessage = statusMessage,
                    onToggleLSL = {
                        coroutineScope.launch {
                            try {
                                if (isLSLActive) {
                                    lslClient.stopLSLStreams()
                                    statusMessage = "LSL stopped"
                                } else {
                                    lslClient.startLSLStreams()
                                    statusMessage = "LSL streaming active"
                                    
                                    // Listen for vibration patterns
                                    lslClient.incomingMarkers.collectLatest { pattern ->
                                        vibrationService.executeVibrationPattern(pattern)
                                    }
                                }
                                isLSLActive = !isLSLActive
                            } catch (e: Exception) {
                                statusMessage = "LSL error: ${e.message}"
                            }
                        }
                    },
                    onToggleSensors = {
                        if (isSensorActive) {
                            onStopSensors()
                            statusMessage = "Sensors stopped"
                        } else {
                            onStartSensors()
                            statusMessage = "Sensors active"
                        }
                        isSensorActive = !isSensorActive
                    },
                    onTestVibration = {
                        vibrationService.testVibration()
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    permissionsState: MultiplePermissionsState,
    isLSLActive: Boolean,
    isSensorActive: Boolean,
    lastHeartRate: String,
    statusMessage: String,
    onToggleLSL: () -> Unit,
    onToggleSensors: () -> Unit,
    onTestVibration: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    if (!permissionsState.allPermissionsGranted) {
        PermissionScreen(permissionsState)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 20.dp,
                    start = 10.dp,
                    end = 10.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = "WearOS LSL",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Heart rate display */ }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Heart Rate",
                                style = MaterialTheme.typography.caption1
                            )
                            Text(
                                text = "$lastHeartRate BPM",
                                style = MaterialTheme.typography.title2,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
                
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onToggleLSL,
                        label = {
                            Text(
                                text = if (isLSLActive) "Stop LSL" else "Start LSL",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (isLSLActive) Color.Red else Color.Green
                        )
                    )
                }
                
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onToggleSensors,
                        label = {
                            Text(
                                text = if (isSensorActive) "Stop Sensors" else "Start Sensors",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (isSensorActive) Color.Red else Color.Blue
                        )
                    )
                }
                
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTestVibration,
                        label = {
                            Text(
                                text = "Test Vibration",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                
                item {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToSettings,
                        label = {
                            Text(
                                text = "Settings",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(permissionsState: MultiplePermissionsState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "This app needs sensor and network permissions to work",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
            
            Chip(
                onClick = {
                    permissionsState.launchMultiplePermissionRequest()
                },
                label = {
                    Text("Grant Permissions")
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 20.dp,
                start = 10.dp,
                end = 10.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { /* TODO: Implement settings */ }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Device Info",
                            style = MaterialTheme.typography.caption1
                        )
                        Text(
                            text = "OnePlus Watch 2R",
                            style = MaterialTheme.typography.body2
                        )
                        Text(
                            text = "WearOS 4.0",
                            style = MaterialTheme.typography.caption2
                        )
                    }
                }
            }
            
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBack,
                    label = {
                        Text("Back")
                    }
                )
            }
        }
    }
}