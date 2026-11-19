package com.alap.mesh.mesh

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.alap.mesh.data.AlapSettings
import com.alap.mesh.data.DataStoreManager
import com.alap.mesh.data.local.AlapDao
import com.alap.mesh.data.local.AlapDatabase
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.FileTransferEntity
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.data.local.MessageEntity
import com.alap.mesh.mesh.ble.BleConnectionState
import com.alap.mesh.mesh.ble.BleMeshEngine
import com.alap.mesh.mesh.ble.BleRadioState
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.mesh.routing.MeshRouter
import com.alap.mesh.service.AlapMeshService
import com.alap.mesh.util.BanglaFormatters
import com.alap.mesh.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface MeshRepository {
    val settingsFlow: Flow<AlapSettings>
    val conversationsFlow: Flow<List<ConversationEntity>>
    val fileTransfersFlow: Flow<List<FileTransferEntity>>
    val meshPeersFlow: Flow<List<MeshPeerEntity>>
    val radioStateFlow: StateFlow<BleRadioState>
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    suspend fun initialize()
    suspend fun startMeshEngine()
    suspend fun stopMeshEngine()
    suspend fun rescanPeers(): Int
    suspend fun sendMessage(conversationId: String, text: String, isSos: Boolean = false)
    suspend fun sendSosBroadcast(customNote: String = "")
    suspend fun sendMeshPing(): String
    suspend fun openOrCreatePeerConversation(peer: MeshPeerEntity): String
    suspend fun markConversationRead(conversationId: String)
    suspend fun sendFileFromUri(uri: Uri, targetPeerName: String, conversationId: String? = null): String
    suspend fun runDiagnosticsTest(): String
    suspend fun clearCacheDirectory(): String
    fun getPublicFingerprint(nodeId: String): String
    val dataStore: DataStoreManager
}

class AlapMeshRepository private constructor(
    private val context: Context,
    override val dataStore: DataStoreManager = DataStoreManager(context),
    private val dao: AlapDao = AlapDatabase.getInstance(context).alapDao(),
    val bleEngine: BleMeshEngine = BleMeshEngine(context)
) : MeshRepository {

    companion object {
        @Volatile
        private var INSTANCE: AlapMeshRepository? = null

        fun getInstance(context: Context): AlapMeshRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlapMeshRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val router = MeshRouter(
        transport = bleEngine,
        dao = dao,
        dataStore = dataStore,
        scope = scope
    )
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    @Volatile private var initialized = false

    override val settingsFlow: Flow<AlapSettings> = dataStore.settingsFlow
    override val conversationsFlow: Flow<List<ConversationEntity>> = dao.observeConversations()
    override val fileTransfersFlow: Flow<List<FileTransferEntity>> = dao.observeFileTransfers()
    override val meshPeersFlow: Flow<List<MeshPeerEntity>> = dao.observeMeshPeers()
    override val radioStateFlow: StateFlow<BleRadioState> = bleEngine.radioState

    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    override suspend fun initialize() {
        if (initialized) return
        initialized = true
        dataStore.ensureNodeIdInitialized()
        ensurePublicChannelExists()
        router.start()
        observeBleDiscoveredPeers()
        observeReadyPeerCount()
    }

    override suspend fun startMeshEngine() {
        initialize()
        val nodeId = dataStore.ensureNodeIdInitialized()
        val settings = settingsFlow.first()
        bleEngine.start(
            nodeId = nodeId,
            displayName = settings.displayName,
            stealthMode = settings.stealthModeEnabled,
            scanMode = settings.bleScanInterval
        )
    }

    override suspend fun stopMeshEngine() {
        bleEngine.stop()
        dao.markAllPeersDisconnected()
    }

    override suspend fun rescanPeers(): Int {
        startMeshEngine()
        router.flushStoreAndForwardQueue()
        return bleEngine.discoveredPeers.value.size
    }

    private fun observeBleDiscoveredPeers() {
        scope.launch {
            bleEngine.discoveredPeers.collect { peerMap ->
                val settings = settingsFlow.first()
                val localNodeId = settings.nodeId.ifBlank { "ALAP-ME" }
                val readySet = bleEngine.readyPeers.value
                val cutoff = System.currentTimeMillis() - BleMeshEngine.STALE_PEER_TIMEOUT_MS
                dao.deleteStaleDisconnectedPeers(cutoff)

                peerMap.values.forEach { blePeer ->
                    val readyInfo = readySet[blePeer.address]
                    val isConnected = blePeer.connectionState == BleConnectionState.READY || readyInfo != null
                    val signalText = when {
                        blePeer.rssi >= -60 -> "${blePeer.rssi} dBm (খুব ভালো)"
                        blePeer.rssi >= -75 -> "${blePeer.rssi} dBm (মাঝারি)"
                        else -> "${blePeer.rssi} dBm (দূরবর্তী)"
                    }
                    val statusLabel = when (blePeer.connectionState) {
                        BleConnectionState.READY -> "সংযুক্ত (READY)"
                        BleConnectionState.HANDSHAKING -> "হ্যান্ডশেক চলছে"
                        BleConnectionState.CONNECTING,
                        BleConnectionState.MTU_NEGOTIATION,
                        BleConnectionState.DISCOVERING_SERVICES,
                        BleConnectionState.ENABLING_NOTIFICATIONS -> "সংযুক্ত হচ্ছে..."
                        BleConnectionState.DISCONNECTED -> "স্ক্যানে পাওয়া গেছে"
                    }
                    dao.upsertMeshPeer(
                        MeshPeerEntity(
                            nodeId = readyInfo?.nodeId ?: blePeer.nodeId,
                            shortId = (readyInfo?.shortId ?: blePeer.shortId).toInt() and 0xFFFF,
                            nodeHash32 = readyInfo?.nodeHash32 ?: blePeer.nodeHash32,
                            name = readyInfo?.displayName ?: blePeer.displayName,
                            avatarShort = (readyInfo?.displayName ?: blePeer.displayName).take(2),
                            hopCount = 1,
                            hopLabel = "1-Hop",
                            linkType = "BLE Mesh",
                            rssiDbm = blePeer.rssi,
                            signalLabel = signalText,
                            statusText = statusLabel,
                            pathSerialized = "আপনি ($localNodeId)|${readyInfo?.displayName ?: blePeer.displayName} (1-Hop • BLE)",
                            colorKey = if (isConnected) "emerald" else "sky",
                            isConnected = isConnected,
                            lastSeenMillis = blePeer.lastSeen
                        )
                    )
                }
            }
        }
    }

    private fun observeReadyPeerCount() {
        scope.launch {
            bleEngine.readyPeers.collect { peers ->
                if (PermissionHelper.hasBluetoothPermissions(context) && bleEngine.isBluetoothReady()) {
                    AlapMeshService.updateNotificationIfRunning(context, peers.size)
                }
            }
        }
    }

    override suspend fun sendMessage(conversationId: String, text: String, isSos: Boolean) {
        router.sendTextMessage(conversationId, text, isSos)
    }

    override suspend fun sendSosBroadcast(customNote: String) {
        val nodeId = dataStore.ensureNodeIdInitialized()
        val sosText = if (customNote.isNotBlank()) {
            "SOS জরুরি বার্তা: $customNote"
        } else {
            "SOS জরুরি বার্তা: জরুরি সাহায্য প্রয়োজন ($nodeId)"
        }
        router.sendTextMessage("c_public", sosText, isSos = true)
    }

    override suspend fun sendMeshPing(): String {
        val ready = bleEngine.readyPeers.value
        if (ready.isEmpty()) {
            return "কোনো সংযুক্ত পিয়ার নেই"
        }
        val results = router.sendPingAndAwaitRtt()
        return if (results.isEmpty()) {
            "পিং পাঠানো হয়েছে, এখনো ACK পাওয়া যায়নি"
        } else {
            val best = results.first()
            "পিং সফল: ${best.fromNodeId} • ${BanglaFormatters.toBanglaDigits(best.rttMs.toInt())}ms (${BanglaFormatters.toBanglaDigits(best.hopCount)} হপ)"
        }
    }

    override suspend fun openOrCreatePeerConversation(peer: MeshPeerEntity): String {
        val existing = dao.getConversationByPeerNodeId(peer.nodeId)
        if (existing != null) {
            dao.markConversationRead(existing.id)
            return existing.id
        }
        val convId = "conv_${peer.nodeId}"
        val now = System.currentTimeMillis()
        dao.upsertConversation(
            ConversationEntity(
                id = convId,
                name = peer.name,
                avatarShort = peer.avatarShort,
                type = "direct",
                hopBadge = "${peer.hopLabel} • ${peer.linkType}",
                unreadCount = 0,
                lastMessage = "কথোপকথন শুরু করুন",
                lastMessageTime = timeFormat.format(Date(now)),
                deliveryStatus = if (peer.isConnected) "সংযুক্ত" else "কিউ মোড",
                hasFile = false,
                peerNodeId = peer.nodeId,
                updatedAtMillis = now
            )
        )
        return convId
    }

    override suspend fun markConversationRead(conversationId: String) {
        dao.markConversationRead(conversationId)
    }

    override suspend fun sendFileFromUri(uri: Uri, targetPeerName: String, conversationId: String?): String {
        // Real Nearby Connections file transfer will be implemented in Step 2.
        // Do NOT fake progress or speed in Step 1.
        val (displayName, _) = resolveUriMeta(uri)
        DiagnosticLogger.log("INFO", "File", "ফাইল ট্রান্সফার ($displayName) ধাপ ২-এ Nearby Connections দিয়ে যুক্ত হবে")
        return "$displayName (ফাইল ট্রান্সফার ধাপ ২-এ যুক্ত হবে)"
    }

    override suspend fun runDiagnosticsTest(): String {
        val nodeId = dataStore.ensureNodeIdInitialized()
        val hash = BanglaFormatters.nodeIdToHash32(nodeId)
        val packet = BinaryMeshPacket(
            type = com.alap.mesh.mesh.protocol.PacketType.DATA,
            ttl = 4,
            hopCount = 0,
            senderHash = hash,
            recipientHash = hash,
            payload = "আলাপ ডায়াগনস্টিক\nলাইন২|টেস্ট".toByteArray(Charsets.UTF_8)
        )
        val decoded = BinaryMeshPacket.decode(packet.encode())
        val packetOk = decoded != null && String(decoded.payload, Charsets.UTF_8) == "আলাপ ডায়াগনস্টিক\nলাইন২|টেস্ট"
        val bleReady = bleEngine.isBluetoothReady()
        val peerCount = bleEngine.readyPeers.value.size
        return when {
            !packetOk -> "বাইনারি প্যাকেট লুপব্যাক টেস্টে ত্রুটি"
            !bleReady -> "প্যাকেট এনকোড/ডিকোড সফল! রেডিও টেস্টের জন্য Bluetooth চালু করুন"
            peerCount == 0 -> "BLE রেডিও ও বাইনারি প্রোটোকল স্বাভাবিক, এখনো কোনো পিয়ার সংযুক্ত নয়"
            else -> "BLE মেশ স্বাভাবিক: ${BanglaFormatters.toBanglaDigits(peerCount)}টি পিয়ার সংযুক্ত"
        }
    }

    override suspend fun clearCacheDirectory(): String {
        val cacheDir = context.cacheDir
        var freedBytes = 0L
        cacheDir.listFiles()?.forEach { f ->
            freedBytes += f.length()
            f.deleteRecursively()
        }
        return "অফলাইন ক্যাশ পরিষ্কার (${formatFileSize(freedBytes)} মুক্ত)"
    }

    override fun getPublicFingerprint(nodeId: String): String {
        if (nodeId.isBlank()) return "কী এখনো তৈরি হয়নি"
        return "ধাপ ২-এ X25519 কী-পেয়ার যুক্ত হলে আসল ফিঙ্গারপ্রিন্ট দেখাবে"
    }

    private suspend fun ensurePublicChannelExists() {
        val existing = dao.getConversationById("c_public")
        if (existing == null) {
            dao.upsertConversation(
                ConversationEntity(
                    id = "c_public",
                    name = "পাবলিক এরিয়া চ্যানেল",
                    avatarShort = "পা",
                    type = "channel",
                    hopBadge = "Public Mesh • Multi-Hop",
                    unreadCount = 0,
                    lastMessage = "কাছাকাছি সবার জন্য খোলা অফলাইন চ্যানেল ও SOS ফিড",
                    lastMessageTime = "--",
                    deliveryStatus = "Broadcast Channel",
                    hasFile = false,
                    peerNodeId = "*",
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private fun resolveUriMeta(uri: Uri): Pair<String, Long> {
        var name = "ALAP_File"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
