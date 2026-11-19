package com.alap.mesh.mesh.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.alap.mesh.mesh.PermissionHelper
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.mesh.protocol.FragmentCodec
import com.alap.mesh.mesh.protocol.FragmentReassembler
import com.alap.mesh.mesh.protocol.HandshakePayload
import com.alap.mesh.mesh.protocol.PacketType
import com.alap.mesh.mesh.routing.ConnectedPeerInfo
import com.alap.mesh.mesh.routing.IncomingPacketEvent
import com.alap.mesh.mesh.routing.MeshTransport
import com.alap.mesh.util.BanglaFormatters
import com.alap.mesh.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BleMeshEngine(
    private val context: Context,
    private val logger: DiagnosticLogger = DiagnosticLogger
) : MeshTransport {

    companion object {
        val ALAP_SERVICE_UUID: UUID = UUID.fromString("0000A1A9-0000-1000-8000-00805F9B34FB")
        val ALAP_CHAR_RX_UUID: UUID = UUID.fromString("0000A1AA-0000-1000-8000-00805F9B34FB")
        val ALAP_CHAR_TX_UUID: UUID = UUID.fromString("0000A1AB-0000-1000-8000-00805F9B34FB")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        const val ALAP_MANUFACTURER_ID = 0xA1A9
        const val MAX_ACTIVE_CONNECTIONS = 5
        const val STALE_PEER_TIMEOUT_MS = 45_000L
        const val WEAK_RSSI_THRESHOLD = -90
        private const val BASE_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 16_000L
    }

    private enum class LinkRole { CLIENT, SERVER }

    private data class PeerSession(
        val address: String,
        val role: LinkRole,
        var state: BleConnectionState = BleConnectionState.DISCONNECTED,
        var gatt: BluetoothGatt? = null,
        var serverDevice: BluetoothDevice? = null,
        var rxCharacteristic: BluetoothGattCharacteristic? = null,
        var maxChunkData: Int = FragmentCodec.DEFAULT_MAX_CHUNK_DATA,
        val writeQueue: GattWriteQueue = GattWriteQueue(),
        var info: ConnectedPeerInfo? = null,
        var rssi: Int = -70,
        var mtuTimeoutJob: Job? = null,
        var handshakeSent: Boolean = false,
        var notificationsEnabled: Boolean = false
    )

    private data class BackoffState(
        val attempts: Int = 0,
        val nextRetryAtMs: Long = 0L
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private var gattServer: BluetoothGattServer? = null
    private var txNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val sessions = ConcurrentHashMap<String, PeerSession>()
    private val retryBackoffs = ConcurrentHashMap<String, BackoffState>()
    private val reassembler = FragmentReassembler()

    private val _discoveredPeers = MutableStateFlow<Map<String, DiscoveredBlePeer>>(emptyMap())
    override val discoveredPeers: StateFlow<Map<String, DiscoveredBlePeer>> = _discoveredPeers.asStateFlow()

    private val _readyPeers = MutableStateFlow<Map<String, ConnectedPeerInfo>>(emptyMap())
    override val readyPeers: StateFlow<Map<String, ConnectedPeerInfo>> = _readyPeers.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<IncomingPacketEvent>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<IncomingPacketEvent> = _incomingPackets.asSharedFlow()

    private val _radioState = MutableStateFlow(BleRadioState())
    override val radioState: StateFlow<BleRadioState> = _radioState.asStateFlow()

    @Volatile private var localNodeId: String = "ALAP-0000-000"
    @Volatile private var localHash32: Int = 1
    @Volatile private var localShortId: Short = 1
    @Volatile private var localDisplayName: String = "ব্যবহারকারী"
    @Volatile private var localStealth: Boolean = false
    @Volatile private var currentScanMode: String = "Balanced"
    @Volatile private var isRunning: Boolean = false

    private var housekeepingJob: Job? = null
    private var scanDutyJob: Job? = null

    override fun isBluetoothReady(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun start(nodeId: String, displayName: String, stealthMode: Boolean, scanMode: String) {
        localNodeId = nodeId
        localHash32 = BanglaFormatters.nodeIdToHash32(nodeId)
        localShortId = BanglaFormatters.nodeIdToShortId(nodeId)
        localDisplayName = displayName
        localStealth = stealthMode
        currentScanMode = scanMode

        val btReady = isBluetoothReady()
        val hasPerms = PermissionHelper.hasBluetoothPermissions(context)
        _radioState.value = _radioState.value.copy(isBluetoothOn = btReady)

        if (!hasPerms || !btReady) {
            logger.log(
                "BLE",
                "Engine",
                if (!btReady) "ব্লুটুথ বন্ধ আছে; রেডিও চালু করা যায়নি" else "BLE পারমিশন নেই"
            )
            return
        }

        isRunning = true
        startGattServer()
        startAdvertising()
        startScanDutyCycle(scanMode)
        startHousekeeping()
        logger.log(
            "BLE",
            "Engine",
            "BLE মেশ চালু: node=$nodeId short=${BanglaFormatters.shortIdHex(localShortId)} mode=$scanMode"
        )
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        isRunning = false
        housekeepingJob?.cancel()
        scanDutyJob?.cancel()
        try {
            if (PermissionHelper.hasBluetoothPermissions(context)) {
                scanner?.stopScan(scanCallback)
                advertiser?.stopAdvertising(advertiseCallback)
                gattServer?.close()
            }
        } catch (e: Exception) {
            logger.log("ERROR", "BLE", "রেডিও বন্ধ করতে সমস্যা: ${e.message}")
        } finally {
            gattServer = null
            txNotifyCharacteristic = null
            sessions.keys.toList().forEach { closeSession(it, applyBackoff = false) }
            sessions.clear()
            _discoveredPeers.value = emptyMap()
            _readyPeers.value = emptyMap()
            _radioState.value = BleRadioState(isBluetoothOn = isBluetoothReady())
            logger.log("BLE", "Engine", "BLE মেশ বন্ধ করা হয়েছে")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        try {
            gattServer?.close()
            val server = bluetoothManager?.openGattServer(context, gattServerCallback) ?: run {
                logger.log("ERROR", "GATT-SRV", "openGattServer null ফেরত দিয়েছে")
                return
            }

            val service = BluetoothGattService(ALAP_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val rxChar = BluetoothGattCharacteristic(
                ALAP_CHAR_RX_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val txChar = BluetoothGattCharacteristic(
                ALAP_CHAR_TX_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val cccd = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            txChar.addDescriptor(cccd)

            service.addCharacteristic(rxChar)
            service.addCharacteristic(txChar)
            server.addService(service)

            txNotifyCharacteristic = txChar
            gattServer = server
            logger.log("BLE", "GATT-SRV", "GATT সার্ভার প্রস্তুত (RX Write + TX Notify)")
        } catch (e: Exception) {
            logger.log("ERROR", "GATT-SRV", "GATT সার্ভার ত্রুটি: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val adv = advertiser ?: run {
            _radioState.value = _radioState.value.copy(
                isAdvertising = false,
                advertiseError = "এই ডিভাইসে BLE Advertising সমর্থিত নয়"
            )
            logger.log("ERROR", "BLE-ADV", "BluetoothLeAdvertiser অনুপলব্ধ")
            return
        }

        try {
            adv.stopAdvertising(advertiseCallback)
            val advMode = when {
                currentScanMode.contains("Ultra", ignoreCase = true) -> AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
                currentScanMode.contains("Fast", ignoreCase = true) -> AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                else -> AdvertiseSettings.ADVERTISE_MODE_BALANCED
            }
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(advMode)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()

            // Strictly <= 31 bytes: Flags (3B) + 128-bit UUID (18B) + 4B ManufacturerData (8B) = 29 bytes
            val hashBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(localHash32).array()
            val primaryData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(ALAP_SERVICE_UUID))
                .addManufacturerData(ALAP_MANUFACTURER_ID, hashBytes)
                .build()

            // Scan response carries compact 8B binary node identity (NO Bangla display name)
            val scanRespBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(localHash32)
                putShort(localShortId)
                put(if (localStealth) 1.toByte() else 0.toByte())
                put(BinaryMeshPacket.PROTOCOL_VERSION)
            }.array()
            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addManufacturerData(ALAP_MANUFACTURER_ID, scanRespBytes)
                .build()

            adv.startAdvertising(settings, primaryData, scanResponse, advertiseCallback)
        } catch (e: Exception) {
            _radioState.value = _radioState.value.copy(
                isAdvertising = false,
                advertiseError = "বিজ্ঞাপন চালু হয়নি: ${e.message}"
            )
            logger.log("ERROR", "BLE-ADV", "startAdvertising ব্যর্থ: ${e.message}")
        }
    }

    private fun startScanDutyCycle(scanMode: String) {
        scanDutyJob?.cancel()
        scanDutyJob = scope.launch {
            while (isActive && isRunning) {
                val (onMs, offMs, androidMode) = when {
                    scanMode.contains("Ultra", ignoreCase = true) ->
                        Triple(10_000L, 15_000L, ScanSettings.SCAN_MODE_LOW_POWER)
                    scanMode.contains("Fast", ignoreCase = true) ->
                        Triple(30_000L, 0L, ScanSettings.SCAN_MODE_LOW_LATENCY)
                    else ->
                        Triple(20_000L, 5_000L, ScanSettings.SCAN_MODE_BALANCED)
                }

                startScanOnce(androidMode)
                delay(onMs)
                if (offMs > 0 && isActive && isRunning) {
                    stopScanOnce()
                    delay(offMs)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanOnce(scanMode: Int) {
        val bleScanner = scanner ?: return
        if (!PermissionHelper.hasBluetoothPermissions(context) || !isBluetoothReady()) return
        try {
            bleScanner.stopScan(scanCallback)
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ALAP_SERVICE_UUID))
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(scanMode)
                .build()
            bleScanner.startScan(listOf(filter), settings, scanCallback)
            _radioState.value = _radioState.value.copy(isScanning = true, scanError = null)
        } catch (e: Exception) {
            _radioState.value = _radioState.value.copy(isScanning = false, scanError = e.message)
            logger.log("ERROR", "BLE-SCAN", "স্ক্যান শুরু হয়নি: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanOnce() {
        try {
            if (PermissionHelper.hasBluetoothPermissions(context)) {
                scanner?.stopScan(scanCallback)
            }
        } catch (_: Exception) {
        } finally {
            _radioState.value = _radioState.value.copy(isScanning = false)
        }
    }

    private fun startHousekeeping() {
        housekeepingJob?.cancel()
        housekeepingJob = scope.launch {
            while (isActive && isRunning) {
                delay(5_000L)
                val now = System.currentTimeMillis()
                val current = _discoveredPeers.value
                val filtered = current.filterValues { peer ->
                    val session = sessions[peer.address]
                    val isAlive = session != null && session.state != BleConnectionState.DISCONNECTED
                    isAlive || (now - peer.lastSeen) <= STALE_PEER_TIMEOUT_MS
                }
                if (filtered.size != current.size) {
                    val removed = current.size - filtered.size
                    _discoveredPeers.value = filtered
                    logger.log("BLE", "Housekeep", "${removed}টি নিষ্ক্রিয় পিয়ার তালিকা থেকে সরানো হয়েছে (>৪৫সে)")
                }
                _radioState.value = _radioState.value.copy(
                    isBluetoothOn = isBluetoothReady(),
                    connectedCount = _readyPeers.value.size
                )
            }
        }
    }

    override suspend fun broadcastPacket(packetBytes: ByteArray, excludeAddress: String?): Int {
        if (!PermissionHelper.hasBluetoothPermissions(context) || !isBluetoothReady()) return 0
        val targets = sessions.values.filter {
            it.state == BleConnectionState.READY && it.address != excludeAddress
        }
        var successCount = 0
        for (session in targets) {
            if (sendPacketToPeer(session.address, packetBytes)) {
                successCount++
            }
        }
        return successCount
    }

    override suspend fun sendPacketToPeer(address: String, packetBytes: ByteArray): Boolean {
        val session = sessions[address] ?: return false
        if (session.state != BleConnectionState.READY) return false
        val decoded = BinaryMeshPacket.decode(packetBytes)
        val msgId = decoded?.messageId ?: BinaryMeshPacket.nextMessageId()
        return sendRawFrame(session, msgId, packetBytes)
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendRawFrame(session: PeerSession, messageId: Long, frameBytes: ByteArray): Boolean {
        if (!PermissionHelper.hasBluetoothPermissions(context) || !isBluetoothReady()) return false
        val chunks = FragmentCodec.fragment(messageId, frameBytes, session.maxChunkData)
        return when (session.role) {
            LinkRole.CLIENT -> {
                val gatt = session.gatt ?: return false
                val rxChar = session.rxCharacteristic ?: return false
                session.writeQueue.sendChunksSerially(chunks) { chunk ->
                    writeCharacteristicCompat(gatt, rxChar, chunk)
                }
            }
            LinkRole.SERVER -> {
                val server = gattServer ?: return false
                val txChar = txNotifyCharacteristic ?: return false
                val device = session.serverDevice ?: return false
                if (!session.notificationsEnabled) return false
                session.writeQueue.sendChunksSerially(chunks) { chunk ->
                    notifyCharacteristicCompat(server, device, txChar, chunk)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = gatt.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            res == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifyCharacteristicCompat(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = server.notifyCharacteristicChanged(device, characteristic, true, value)
            res == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            server.notifyCharacteristicChanged(device, characteristic, true)
        }
    }

    private fun parseAdvertisementMetadata(result: ScanResult): Triple<Int, Short, Boolean>? {
        val mfg = result.scanRecord?.getManufacturerSpecificData(ALAP_MANUFACTURER_ID) ?: return null
        return when {
            mfg.size >= 8 -> {
                val buf = ByteBuffer.wrap(mfg).order(ByteOrder.BIG_ENDIAN)
                val hash = buf.getInt()
                val shortId = buf.getShort()
                val stealth = buf.get().toInt() == 1
                Triple(hash, shortId, stealth)
            }
            mfg.size >= 4 -> {
                val hash = ByteBuffer.wrap(mfg, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt()
                val shortId = (hash and 0xFFFF).toShort().let { if (it.toInt() == 0) 1.toShort() else it }
                Triple(hash, shortId, false)
            }
            else -> null
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _radioState.value = _radioState.value.copy(isAdvertising = true, advertiseError = null)
            logger.log("BLE-ADV", "Advertise", "বিজ্ঞাপন চালু হয়েছে (<=৩১ বাইট)")
        }

        override fun onStartFailure(errorCode: Int) {
            val reason = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "ডেটা আকার ৩১ বাইটের বেশি (DATA_TOO_LARGE)"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "সিস্টেমে বেশি বিজ্ঞাপন চালু (TOO_MANY_ADVERTISERS)"
                ADVERTISE_FAILED_ALREADY_STARTED -> "ইতিমধ্যে চালু আছে (ALREADY_STARTED)"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "অভ্যন্তরীণ ব্লুটুথ ত্রুটি (INTERNAL_ERROR)"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE Advertising সমর্থিত নয়"
                else -> "অজানা ত্রুটি কোড $errorCode"
            }
            _radioState.value = _radioState.value.copy(isAdvertising = false, advertiseError = reason)
            logger.log("ERROR", "BLE-ADV", "Advertise ব্যর্থ: $reason")
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val meta = parseAdvertisementMetadata(result) ?: return
            val (remoteHash, remoteShortId, isStealth) = meta
            if (remoteHash == localHash32) return

            val now = System.currentTimeMillis()
            val existingSession = sessions[address]
            val existingPeer = _discoveredPeers.value[address]
            val shortHex = BanglaFormatters.shortIdHex(remoteShortId)
            val fallbackNodeId = existingSession?.info?.nodeId ?: existingPeer?.nodeId ?: "ALAP-$shortHex"
            val fallbackName = existingSession?.info?.displayName
                ?: existingPeer?.displayName
                ?: if (isStealth) "Stealth নোড ($shortHex)" else "নোড $shortHex"

            if (existingSession != null) {
                existingSession.rssi = result.rssi
            }

            val peer = DiscoveredBlePeer(
                address = address,
                nodeHash32 = remoteHash,
                shortId = remoteShortId,
                nodeId = fallbackNodeId,
                displayName = fallbackName,
                rssi = result.rssi,
                stealth = isStealth,
                connectionState = existingSession?.state ?: BleConnectionState.DISCONNECTED,
                lastSeen = now
            )
            _discoveredPeers.value = _discoveredPeers.value + (address to peer)

            maybeInitiateClientConnection(device, remoteHash, result.rssi)
        }

        override fun onScanFailed(errorCode: Int) {
            val reason = "BLE স্ক্যান ত্রুটি কোড $errorCode"
            _radioState.value = _radioState.value.copy(isScanning = false, scanError = reason)
            logger.log("ERROR", "BLE-SCAN", reason)
        }
    }

    @SuppressLint("MissingPermission")
    private fun maybeInitiateClientConnection(device: BluetoothDevice, remoteHash: Int, rssi: Int) {
        val address = device.address ?: return
        if (!isRunning || !PermissionHelper.hasBluetoothPermissions(context)) return

        // Avoid duplicate links to same peer (by address or by remoteHash)
        val alreadyLinked = sessions.values.any {
            it.address == address || (it.info?.nodeHash32 == remoteHash && it.state != BleConnectionState.DISCONNECTED)
        }
        if (alreadyLinked) return

        // Deterministic tie-breaker: higher unsigned 32-bit hash acts as GATT Client
        val shouldInitiate = localHash32.toUInt() > remoteHash.toUInt()
        if (!shouldInitiate) return

        val backoff = retryBackoffs[address]
        val now = System.currentTimeMillis()
        if (backoff != null && now < backoff.nextRetryAtMs) return

        if (rssi < WEAK_RSSI_THRESHOLD && sessions.size >= 2) return

        // Enforce max 5 connections; evict weakest if full and new RSSI is noticeably stronger
        if (sessions.size >= MAX_ACTIVE_CONNECTIONS) {
            val weakest = sessions.values.minByOrNull { it.rssi }
            if (weakest != null && weakest.rssi < WEAK_RSSI_THRESHOLD && rssi > weakest.rssi + 6) {
                logger.log("BLE", "Pool", "দুর্বল সংযোগ (${weakest.address}, ${weakest.rssi} dBm) সরিয়ে নতুন সংযোগ")
                closeSession(weakest.address, applyBackoff = false)
            } else {
                return
            }
        }

        val session = PeerSession(
            address = address,
            role = LinkRole.CLIENT,
            state = BleConnectionState.CONNECTING,
            rssi = rssi
        )
        sessions[address] = session
        updatePeerState(address, BleConnectionState.CONNECTING)
        logger.log("BLE", "Connect", "$address-এ GATT সংযোগ শুরু (RSSI=$rssi dBm)")

        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context, false, gattClientCallback)
        }
        session.gatt = gatt
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address ?: return
            val session = sessions[address]
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                if (session == null) {
                    gatt.close()
                    return
                }
                session.state = BleConnectionState.MTU_NEGOTIATION
                updatePeerState(address, BleConnectionState.MTU_NEGOTIATION)
                retryBackoffs.remove(address)
                logger.log("BLE", "GATT-CLI", "$address সংযুক্ত; MTU 247 অনুরোধ")

                val requested = gatt.requestMtu(247)
                session.mtuTimeoutJob?.cancel()
                session.mtuTimeoutJob = scope.launch {
                    delay(2_500L)
                    if (session.state == BleConnectionState.MTU_NEGOTIATION) {
                        session.state = BleConnectionState.DISCOVERING_SERVICES
                        updatePeerState(address, BleConnectionState.DISCOVERING_SERVICES)
                        gatt.discoverServices()
                    }
                }
                if (!requested) {
                    session.mtuTimeoutJob?.cancel()
                    session.state = BleConnectionState.DISCOVERING_SERVICES
                    updatePeerState(address, BleConnectionState.DISCOVERING_SERVICES)
                    gatt.discoverServices()
                }
            } else {
                logger.log("BLE", "GATT-CLI", "$address বিচ্ছিন্ন (status=$status state=$newState)")
                closeSession(address, applyBackoff = status != BluetoothGatt.GATT_SUCCESS)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val address = gatt.device.address ?: return
            val session = sessions[address] ?: return
            session.mtuTimeoutJob?.cancel()
            val negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23
            session.maxChunkData = (negotiatedMtu - 3 - FragmentCodec.FRAGMENT_HEADER_BYTES).coerceIn(20, 220)
            logger.log("BLE", "GATT-CLI", "$address MTU=$negotiatedMtu (chunk=${session.maxChunkData}B)")
            if (session.state == BleConnectionState.MTU_NEGOTIATION) {
                session.state = BleConnectionState.DISCOVERING_SERVICES
                updatePeerState(address, BleConnectionState.DISCOVERING_SERVICES)
                gatt.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val address = gatt.device.address ?: return
            val session = sessions[address] ?: return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logger.log("ERROR", "GATT-CLI", "$address সার্ভিস ডিসকভারি ব্যর্থ ($status)")
                closeSession(address, applyBackoff = true)
                return
            }
            val service = gatt.getService(ALAP_SERVICE_UUID)
            val rxChar = service?.getCharacteristic(ALAP_CHAR_RX_UUID)
            val txChar = service?.getCharacteristic(ALAP_CHAR_TX_UUID)
            if (service == null || rxChar == null || txChar == null) {
                logger.log("ERROR", "GATT-CLI", "$address-এ ALAP সার্ভিস পাওয়া যায়নি")
                closeSession(address, applyBackoff = true)
                return
            }

            session.rxCharacteristic = rxChar
            gatt.setCharacteristicNotification(txChar, true)
            val cccd = txChar.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                session.state = BleConnectionState.ENABLING_NOTIFICATIONS
                updatePeerState(address, BleConnectionState.ENABLING_NOTIFICATIONS)
                val wrote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothGatt.GATT_SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(cccd)
                }
                if (!wrote) {
                    logger.log("ERROR", "GATT-CLI", "$address CCCD লেখা ব্যর্থ")
                    closeSession(address, applyBackoff = true)
                }
            } else {
                beginClientHandshake(session)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val address = gatt.device.address ?: return
            val session = sessions[address] ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                beginClientHandshake(session)
            } else {
                logger.log("ERROR", "GATT-CLI", "$address CCCD callback ব্যর্থ ($status)")
                closeSession(address, applyBackoff = true)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val address = gatt.device.address ?: return
            sessions[address]?.writeQueue?.onWriteCallback(status == BluetoothGatt.GATT_SUCCESS)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                val address = gatt.device.address ?: return
                handleIncomingChunk(address, value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val address = gatt.device.address ?: return
            handleIncomingChunk(address, value)
        }
    }

    private fun beginClientHandshake(session: PeerSession) {
        session.state = BleConnectionState.HANDSHAKING
        updatePeerState(session.address, BleConnectionState.HANDSHAKING)
        scope.launch {
            sendHandshakePacket(session)
        }
    }

    private suspend fun sendHandshakePacket(session: PeerSession) {
        if (session.handshakeSent) return
        val hs = HandshakePayload(
            nodeId = localNodeId,
            shortId = localShortId,
            displayName = localDisplayName,
            stealth = localStealth
        )
        val pkt = BinaryMeshPacket(
            type = PacketType.HANDSHAKE,
            ttl = 1,
            hopCount = 0,
            senderHash = localHash32,
            recipientHash = BinaryMeshPacket.BROADCAST_HASH,
            payload = hs.encode()
        )
        val ok = sendRawFrame(session, pkt.messageId, pkt.encode())
        if (ok) {
            session.handshakeSent = true
            logger.log("TX", "Handshake", "${session.address}-এ হ্যান্ডশেক পাঠানো হয়েছে")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            val address = device.address ?: return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (sessions.size >= MAX_ACTIVE_CONNECTIONS && !sessions.containsKey(address)) {
                    logger.log("BLE", "GATT-SRV", "সর্বোচ্চ কানেকশন পূর্ণ, $address প্রত্যাখ্যান")
                    return
                }
                val existing = sessions[address]
                if (existing == null || existing.role == LinkRole.SERVER) {
                    val session = PeerSession(
                        address = address,
                        role = LinkRole.SERVER,
                        state = BleConnectionState.ENABLING_NOTIFICATIONS,
                        serverDevice = device
                    )
                    sessions[address] = session
                    logger.log("BLE", "GATT-SRV", "ক্লায়েন্ট $address সংযুক্ত হয়েছে")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val existing = sessions[address]
                if (existing?.role == LinkRole.SERVER) {
                    logger.log("BLE", "GATT-SRV", "ক্লায়েন্ট $address বিচ্ছিন্ন")
                    closeSession(address, applyBackoff = false)
                }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            val address = device.address ?: return
            sessions[address]?.let {
                it.maxChunkData = (mtu - 3 - FragmentCodec.FRAGMENT_HEADER_BYTES).coerceIn(20, 220)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
            val address = device.address ?: return
            val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            val session = sessions.getOrPut(address) {
                PeerSession(address = address, role = LinkRole.SERVER, serverDevice = device)
            }
            session.serverDevice = device
            session.notificationsEnabled = enabled
            if (enabled && session.state != BleConnectionState.READY) {
                session.state = BleConnectionState.HANDSHAKING
            }
            logger.log("BLE", "GATT-SRV", "$address নোটিফিকেশন সাবস্ক্রিপশন: $enabled")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
            val address = device.address ?: return
            val session = sessions.getOrPut(address) {
                PeerSession(address = address, role = LinkRole.SERVER, serverDevice = device)
            }
            if (session.serverDevice == null) session.serverDevice = device
            handleIncomingChunk(address, value)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            val address = device.address ?: return
            sessions[address]?.writeQueue?.onWriteCallback(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    private fun handleIncomingChunk(address: String, chunkBytes: ByteArray) {
        val frameBytes = reassembler.onFragmentReceived(address, chunkBytes) ?: return
        val packet = BinaryMeshPacket.decode(frameBytes) ?: run {
            logger.log("ERROR", "Decode", "$address থেকে অবৈধ প্যাকেট ফ্রেম")
            return
        }

        if (packet.type == PacketType.HANDSHAKE) {
            val hs = HandshakePayload.decode(packet.payload) ?: return
            val session = sessions[address] ?: return
            val peerInfo = ConnectedPeerInfo(
                address = address,
                nodeId = hs.nodeId,
                shortId = hs.shortId,
                nodeHash32 = packet.senderHash,
                displayName = if (hs.stealth) "Stealth (${BanglaFormatters.shortIdHex(hs.shortId)})" else hs.displayName,
                rssi = session.rssi,
                publicKeyBytes = hs.publicKeyBytes,
                isStealth = hs.stealth
            )
            session.info = peerInfo
            session.state = BleConnectionState.READY
            _readyPeers.value = _readyPeers.value + (address to peerInfo)

            val discovered = _discoveredPeers.value[address]
            if (discovered != null) {
                _discoveredPeers.value = _discoveredPeers.value + (address to discovered.copy(
                    nodeId = peerInfo.nodeId,
                    shortId = peerInfo.shortId,
                    displayName = peerInfo.displayName,
                    stealth = peerInfo.isStealth,
                    connectionState = BleConnectionState.READY,
                    lastSeen = System.currentTimeMillis()
                ))
            } else {
                _discoveredPeers.value = _discoveredPeers.value + (address to DiscoveredBlePeer(
                    address = address,
                    nodeHash32 = peerInfo.nodeHash32,
                    shortId = peerInfo.shortId,
                    nodeId = peerInfo.nodeId,
                    displayName = peerInfo.displayName,
                    rssi = session.rssi,
                    stealth = peerInfo.isStealth,
                    connectionState = BleConnectionState.READY,
                    lastSeen = System.currentTimeMillis()
                ))
            }

            _radioState.value = _radioState.value.copy(connectedCount = _readyPeers.value.size)
            logger.log("RX", "Handshake", "${peerInfo.displayName} (${peerInfo.nodeId}) READY হয়েছে")

            if (session.role == LinkRole.SERVER && !session.handshakeSent) {
                scope.launch { sendHandshakePacket(session) }
            }
            return
        }

        val rssi = sessions[address]?.rssi ?: -70
        _incomingPackets.tryEmit(IncomingPacketEvent(address, rssi, frameBytes))
    }

    private fun updatePeerState(address: String, state: BleConnectionState) {
        val current = _discoveredPeers.value[address] ?: return
        _discoveredPeers.value = _discoveredPeers.value + (address to current.copy(connectionState = state))
    }

    @SuppressLint("MissingPermission")
    private fun closeSession(address: String, applyBackoff: Boolean) {
        val session = sessions.remove(address)
        session?.mtuTimeoutJob?.cancel()
        session?.writeQueue?.cancelAll()
        try {
            if (PermissionHelper.hasBluetoothPermissions(context)) {
                session?.gatt?.disconnect()
                session?.gatt?.close()
            }
        } catch (_: Exception) {
        }
        reassembler.clearPeer(address)

        if (_readyPeers.value.containsKey(address)) {
            _readyPeers.value = _readyPeers.value - address
        }
        updatePeerState(address, BleConnectionState.DISCONNECTED)
        _radioState.value = _radioState.value.copy(connectedCount = _readyPeers.value.size)

        if (applyBackoff) {
            val prev = retryBackoffs[address]
            val nextAttempts = ((prev?.attempts ?: 0) + 1).coerceAtMost(4)
            val delayMs = (BASE_BACKOFF_MS shl (nextAttempts - 1)).coerceAtMost(MAX_BACKOFF_MS)
            retryBackoffs[address] = BackoffState(
                attempts = nextAttempts,
                nextRetryAtMs = System.currentTimeMillis() + delayMs
            )
        }
    }
}
