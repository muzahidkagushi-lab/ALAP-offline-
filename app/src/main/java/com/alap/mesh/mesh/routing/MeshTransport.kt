package com.alap.mesh.mesh.routing

import com.alap.mesh.mesh.ble.BleRadioState
import com.alap.mesh.mesh.ble.DiscoveredBlePeer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class ConnectedPeerInfo(
    val address: String,
    val nodeId: String,
    val shortId: Short,
    val nodeHash32: Int,
    val displayName: String,
    val rssi: Int,
    val publicKeyBytes: ByteArray = ByteArray(0),
    val isStealth: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConnectedPeerInfo) return false
        return address == other.address &&
            nodeId == other.nodeId &&
            shortId == other.shortId &&
            nodeHash32 == other.nodeHash32 &&
            displayName == other.displayName
    }

    override fun hashCode(): Int = nodeId.hashCode()
}

data class IncomingPacketEvent(
    val fromAddress: String,
    val rssi: Int,
    val packetBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncomingPacketEvent) return false
        return fromAddress == other.fromAddress && packetBytes.contentEquals(other.packetBytes)
    }

    override fun hashCode(): Int = fromAddress.hashCode()
}

interface MeshTransport {
    val readyPeers: StateFlow<Map<String, ConnectedPeerInfo>>
    val discoveredPeers: StateFlow<Map<String, DiscoveredBlePeer>>
    val incomingPackets: SharedFlow<IncomingPacketEvent>
    val radioState: StateFlow<BleRadioState>

    suspend fun sendPacketToPeer(address: String, packetBytes: ByteArray): Boolean
    suspend fun broadcastPacket(packetBytes: ByteArray, excludeAddress: String? = null): Int
    fun start(nodeId: String, displayName: String, stealthMode: Boolean, scanMode: String)
    fun stop()
    fun isBluetoothReady(): Boolean
}
