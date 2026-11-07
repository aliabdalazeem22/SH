package com.example.swiftshare.domain

/**
 * Observable snapshot of the runtime capabilities SwiftShare needs.
 *
 * Keeping this as a StateFlow lets the Compose UI refresh immediately after the
 * user grants permissions or flips Bluetooth/Wi-Fi, instead of only on restart.
 */
data class DeviceCapabilities(
    val permissionsGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val wifiEnabled: Boolean = false
) {
    val readyForNearby: Boolean get() = permissionsGranted && bluetoothEnabled
}
