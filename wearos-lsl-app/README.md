# WearOS LSL App for OnePlus Watch 2R

A comprehensive Android app for WearOS 4 that integrates with Lab Streaming Layer (LSL) to:
- Stream IMU sensor data (accelerometer, gyroscope, magnetometer) and heart rate
- Receive vibration patterns from LSL streams (e.g., "010101")
- Provide real-time sensor data streaming for research experiments

## Features

### 🔄 LSL Integration
- **Outgoing Streams**: IMU data and heart rate streaming to LSL network
- **Incoming Streams**: Vibration pattern commands from LSL
- **Real-time Data**: 50Hz sensor data, 1Hz heart rate monitoring
- **Network Discovery**: Automatic LSL stream discovery and connection

### 📱 Sensor Data Collection
- **IMU Sensors**: 3-axis accelerometer, gyroscope, magnetometer
- **Heart Rate**: Real-time BPM monitoring using Health Services
- **High Frequency**: Up to 50Hz sampling rate for motion sensors
- **Background Collection**: Continues data collection when screen is off

### 🔧 Vibration Feedback
- **Pattern Support**: Binary patterns like "010101", "110011", etc.
- **Customizable**: Adjustable duration and intensity
- **Real-time Response**: Immediate vibration when LSL commands received

### 🎯 WearOS 4 Optimized
- **Health Services**: Uses latest WearOS Health Services API
- **Compose UI**: Modern Wear Compose interface
- **Battery Efficient**: Optimized for extended use
- **Permissions**: Proper handling of sensitive permissions

## Requirements

### Hardware
- OnePlus Watch 2R (or compatible WearOS device)
- WearOS 4.0 or higher (API level 30+)
- Accelerometer, Gyroscope, Magnetometer, Heart Rate sensor

### Software
- Android Studio Giraffe or newer
- Kotlin 1.9.22+
- Gradle 8.3+

### Network
- WiFi connection for LSL communication
- LSL-compatible receiving application on the network

## Installation

### 1. Clone Repository
```bash
git clone <repository-url>
cd wearos-lsl-app
```

### 2. Open in Android Studio
- Import the project in Android Studio
- Sync Gradle files
- Connect your OnePlus Watch 2R via ADB

### 3. Deploy to Watch
```bash
./gradlew installDebug
```

## Usage

### Basic Operation

1. **Grant Permissions**: On first launch, grant all required permissions
2. **Start Sensors**: Tap "Start Sensors" to begin data collection
3. **Start LSL**: Tap "Start LSL" to begin streaming data
4. **Monitor Status**: Check heart rate and status messages on screen

### LSL Streams Created

#### IMU Data Stream
- **Name**: `OnePlusWatch2R_IMU`
- **Type**: `IMU`
- **Channels**: 9 (ax, ay, az, gx, gy, gz, mx, my, mz)
- **Sample Rate**: 50 Hz
- **Format**: float32

#### Heart Rate Stream
- **Name**: `OnePlusWatch2R_HeartRate`
- **Type**: `HR`
- **Channels**: 1 (heart rate in BPM)
- **Sample Rate**: 1 Hz
- **Format**: float32

### Receiving Vibration Commands

The app listens for LSL markers containing vibration patterns:
- Send LSL marker with pattern like `{"pattern": "010101"}`
- Watch will vibrate according to binary pattern
- 1 = vibrate, 0 = pause
- Default duration: 100ms per pulse

## Architecture

### Services
- **SensorDataService**: Collects sensor data using Health Services
- **LSLService**: Manages LSL stream connections
- **VibrationService**: Handles haptic feedback patterns

### Data Flow
```
Sensors → SensorDataService → LSLService → LSL Network
LSL Network → LSLClient → VibrationService → Haptic Motor
```

### Key Components
- **LSLClient**: Core LSL integration and network communication
- **MainActivity**: WearOS Compose UI with circular navigation
- **Health Services**: Heart rate and activity monitoring
- **Traditional Sensors**: Direct sensor manager for IMU data

## Configuration

### Sampling Rates
- **IMU Data**: 50 Hz (configurable in SensorDataService)
- **Heart Rate**: 1 Hz (Health Services managed)
- **Data Emission**: 20ms intervals

### Network Settings
- **LSL Multicast**: 239.255.172.215:16571
- **Discovery**: Automatic LSL stream discovery
- **Timeout**: 5 second connection timeout

### Vibration Settings
- **Default Duration**: 100ms per pulse
- **Intensity**: 0-255 (default 255)
- **Pattern Format**: Binary string (e.g., "010101")

## Development

### Key Files Structure
```
app/src/main/java/com/example/wearoslsl/
├── data/
│   └── SensorData.kt          # Data models
├── lsl/
│   └── LSLClient.kt           # LSL integration
├── service/
│   ├── SensorDataService.kt   # Sensor collection
│   ├── LSLService.kt          # LSL streaming
│   └── VibrationService.kt    # Haptic feedback
└── presentation/
    └── MainActivity.kt        # WearOS UI
```

### Adding New Sensors
1. Add sensor type to `SensorDataService`
2. Update `SensorData` data class
3. Modify LSL stream configuration
4. Update UI to display new data

### Extending LSL Commands
1. Modify `parseVibrationCommand()` in `LSLClient`
2. Add new command types to `VibrationPattern`
3. Implement command handlers in `VibrationService`

## Testing

### Testing Vibration Patterns
```bash
# Send test pattern via LSL (example using Python)
from pylsl import StreamOutlet, StreamInfo
info = StreamInfo('VibrationCommands', 'Markers', 1, 0, 'string')
outlet = StreamOutlet(info)
outlet.push_sample(['{"pattern": "010101"}'])
```

### Monitoring LSL Streams
Use LSL Lab Recorder or similar tools to monitor:
- `OnePlusWatch2R_IMU` stream
- `OnePlusWatch2R_HeartRate` stream

### Debug Logging
Check Android logs for detailed operation:
```bash
adb logcat | grep -E "(LSLClient|SensorDataService|VibrationService)"
```

## Troubleshooting

### Common Issues

**Permissions Denied**
- Ensure all permissions granted in Settings
- Check BODY_SENSORS permission specifically

**No Heart Rate Data**
- Ensure watch is worn properly
- Check Health Services availability
- Verify sensor permissions

**LSL Connection Issues**
- Verify WiFi connectivity
- Check network firewall settings
- Ensure LSL receiver is running

**High Battery Drain**
- Normal for continuous sensor monitoring
- Use power-optimized settings if needed
- Monitor wake lock usage

### Performance Optimization
- Adjust sampling rates if needed
- Use batch processing for network efficiency
- Implement data buffering for network interruptions

## Contributing

1. Fork the repository
2. Create feature branch
3. Follow Kotlin coding conventions
4. Add tests for new functionality
5. Submit pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Lab Streaming Layer (LSL) project
- Android Health Services team
- WearOS development community
- OnePlus Watch 2R hardware specifications

## Research Applications

This app is designed for research scenarios including:
- Motion analysis experiments
- Physiological monitoring studies
- Human-computer interaction research
- Real-time feedback systems
- Multi-modal data collection

For research use, ensure compliance with your institution's ethics guidelines and data protection requirements.