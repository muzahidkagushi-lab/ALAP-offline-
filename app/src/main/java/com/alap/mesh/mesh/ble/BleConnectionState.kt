package com.alap.mesh.mesh.ble

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    MTU_NEGOTIATION,
    DISCOVERING_SERVICES,
    ENABLING_NOTIFICATIONS,
    HANDSHAKING,
    READY
}

data class DiscoveredBlePeer(
    val address: String,
    val nodeHash32: Int,
    val shortId: Short,
    val nodeId: String,
    val displayName: String,
    val rssi: Int,
    val stealth: Boolean = false,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val lastSeen: Long = System.currentTimeMillis()
)

data class BleRadioState(
    val isBluetoothOn: Boolean = false,
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false,
    val advertiseError: String? = null,
    val scanError: String? = null,
    val connectedCount: Int = 0
)
