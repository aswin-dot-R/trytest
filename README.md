# WearOS LSL App for OnePlus Watch 2R

A comprehensive Android app for WearOS 4 that integrates with Lab Streaming Layer (LSL) for research experiments.

## 🎯 Features

- **LSL Streaming**: Real-time IMU and heart rate data streaming via Lab Streaming Layer
- **Vibration Feedback**: Receives and executes vibration patterns (e.g., "010101") from LSL commands
- **Sensor Collection**: 50Hz IMU data (accelerometer, gyroscope, magnetometer) + 1Hz heart rate
- **WearOS Optimized**: Built specifically for WearOS 4 using Health Services API

## 📱 Target Device

- **OnePlus Watch 2R** (WearOS 4)
- Compatible with other WearOS 4+ devices with required sensors

## 🚀 Quick Start

### Prerequisites

- Android Studio Giraffe or newer
- OnePlus Watch 2R with WearOS 4
- ADB debugging enabled on watch
- LSL-compatible receiver on network

### Building & Installation

1. **Clone the repository**
   ```bash
   git clone <this-repo>
   cd wearos-lsl-app
   ```

2. **Open in Android Studio**
   - Import the project
   - Let Gradle sync complete

3. **Connect your watch**
   - Enable Developer Options on watch
   - Enable ADB debugging
   - Connect via USB or Wi-Fi

4. **Build and deploy**
   ```bash
   ./gradlew installDebug
   ```

### Usage

1. Launch the app on your watch
2. Grant all required permissions (sensors, network, etc.)
3. Tap "Start Sensors" to begin data collection
4. Tap "Start LSL" to begin streaming
5. Send vibration commands from your LSL application

## 📊 LSL Streams

### Outgoing Streams

#### IMU Data Stream
- **Name**: `OnePlusWatch2R_IMU`
- **Type**: `IMU`
- **Channels**: 9 (ax, ay, az, gx, gy, gz, mx, my, mz)
- **Sample Rate**: 50 Hz

#### Heart Rate Stream
- **Name**: `OnePlusWatch2R_HeartRate`
- **Type**: `HR`
- **Channels**: 1 (BPM)
- **Sample Rate**: 1 Hz

### Incoming Commands

Send LSL markers with vibration patterns:
```json
{"pattern": "010101"}
```
- 1 = vibrate (100ms default)
- 0 = pause (100ms default)

## 🔧 Architecture

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────┐
│   Sensors       │───▶│ Service      │───▶│ LSL Client  │
│ • Accelerometer │    │ • Data       │    │ • Network   │
│ • Gyroscope     │    │   Collection │    │   Streaming │
│ • Magnetometer  │    │ • Processing │    │ • Discovery │
│ • Heart Rate    │    │              │    │             │
└─────────────────┘    └──────────────┘    └─────────────┘
                                                   │
┌─────────────────┐    ┌──────────────┐           │
│ Vibration       │◀───│ Command      │◀──────────┘
│ • Pattern Exec  │    │ Processing   │
│ • Haptic Motor  │    │ • Parse LSL  │
│                 │    │   Commands   │
└─────────────────┘    └──────────────┘
```

## 🛠️ Key Components

- **SensorDataService**: Collects sensor data using Health Services
- **LSLClient**: Manages LSL network communication
- **VibrationService**: Handles haptic feedback patterns
- **MainActivity**: WearOS Compose UI

## 📋 Project Structure

```
wearos-lsl-app/
├── app/
│   ├── src/main/java/com/example/wearoslsl/
│   │   ├── data/           # Data models
│   │   ├── lsl/            # LSL integration
│   │   ├── service/        # Background services
│   │   └── presentation/   # UI components
│   ├── src/main/res/       # Resources
│   └── build.gradle        # App dependencies
├── build.gradle            # Project configuration
├── settings.gradle         # Module settings
└── README.md              # This file
```

## 🔬 Research Applications

Perfect for:
- Motion analysis experiments
- Physiological monitoring studies
- Human-computer interaction research
- Real-time feedback systems
- Multi-modal data collection

## 🐛 Troubleshooting

**Permissions Denied**
- Check Settings > Apps > WearOS LSL > Permissions
- Ensure BODY_SENSORS permission is granted

**No Heart Rate Data**
- Ensure watch is worn properly on wrist
- Check that sensors are clean and making contact

**LSL Connection Issues**
- Verify Wi-Fi connectivity on watch
- Check network firewall settings
- Ensure LSL receiver is running on network

## 📄 License

MIT License - see LICENSE file for details.

## 🙏 Acknowledgments

- Lab Streaming Layer (LSL) project
- WearOS Health Services team
- Android development community

---

**Note**: This app is designed for research purposes. Ensure compliance with your institution's ethics guidelines and data protection requirements.
