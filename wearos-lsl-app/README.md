# WearLSLApp

A standalone Wear OS 4 application for the OnePlus Watch 2R that bridges **Lab Streaming Layer (LSL)** with on-device sensors.

Features:

* Subscribes to an LSL stream of type `VibrationPattern`. Each sample is expected to be a string like `010101` which is translated into a 100 ms on/off waveform on the watch's haptic motor.
* Publishes an `IMU` stream (`wear_imu`) containing accelerometer and gyroscope values (`6 × float32`) at the fastest attainable rate.
* Publishes a `HeartRate` stream (`wear_hr`) containing BPM (`1 × float32`).

## Building

1. Open the `wearos-lsl-app` directory in Android Studio Hedgehog or later.
2. Make sure you have the latest **Wear OS 4 SDK (API 34)** and **Android 13** build-tools installed.
3. Click *Run* with your OnePlus Watch 2R connected (or use the emulator).

## Runtime permissions

The app requests `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`, and `VIBRATE` on first launch. Because there is no UI, it terminates immediately after requesting permissions and runs as a foreground service in the background after you grant them.

## LSL expectations

* **Inbound pattern stream** — *name arbitrary*, `type="VibrationPattern"`, channel 1, format `string`.
* **Outbound IMU stream** — name `wear_imu`, `type="IMU"`, channel 6, `float32`.
* **Outbound HR stream** — name `wear_hr`, `type="HeartRate"`, channel 1, `float32`.

Adjust the identifiers in `LSLService.kt` if your experiment uses different metadata.