BLE-ESP32-Controller
A lightweight Kotlin Android app for wireless communication with ESP32 via BLE, enabling simple IoT control and data interaction.
Overview
This app provides core BLE functionalities to connect Android devices with ESP32 microcontrollers—perfect for quick prototyping, hobby projects, or embedded system testing.
Features
Scan & filter ESP32 BLE devices
Stable connection & auto-retry
Bidirectional data transfer (send commands/receive sensor data)
Minimalist, easy-to-use UI
Low power consumption (BLE-optimized)
Prerequisites
Android device (API 21+, Android 5.0+)
ESP32 microcontroller with BLE service enabled
ESP32 BLE service/characteristic UUIDs (configure in app code)
Power on ESP32 and enable its BLE service
Grant Bluetooth/BLE permissions to the app
Scan for devices, select your ESP32 from the list
Send control commands or view received data in real-time
Language: Kotlin
Framework: Android SDK
Protocol: BLE (Bluetooth Low Energy)
Target Hardware: ESP32
Notes
Ensure ESP32 and Android device are within BLE range (≤10m)
Check app permissions (Location/Bluetooth) on Android 12+
Modify data parsing logic in BluetoothLeService.kt to match your ESP32's data format
