package com.alap.mesh

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.alap.mesh.data.DataStoreManager
import com.alap.mesh.data.local.AlapDatabase
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.DeliveryStatus
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.mesh.ble.BleRadioState
import com.alap.mesh.mesh.ble.DiscoveredBlePeer
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.mesh.protocol.PacketType
import com.alap.mesh.mesh.routing.ConnectedPeerInfo
import com.alap.mesh.mesh.routing.IncomingPacketEvent
import com.alap.mesh.mesh.routing.MeshRouter
import com.alap.mesh.mesh.routing.MeshTransport
import com.alap.mesh.util.BanglaFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MeshRouterSimulatorTest {

    private class FakeMeshTransport(
        val selfAddress: String,
        private val network: FakeMeshNetwork,
        private val scope: CoroutineScope
    ) : MeshTransport {

        private val _readyPeers = MutableStateFlow<Map<String, ConnectedPeerInfo>>(emptyMap())
        override val readyPeers: StateFlow<Map<String, ConnectedPeerInfo>> = _readyPeers.asStateFlow()

        private val _discoveredPeers = MutableStateFlow<Map<String, DiscoveredBlePeer>>(emptyMap())
        override val discoveredPeers: StateFlow<Map<String, DiscoveredBlePeer>> = _discoveredPeers.asStateFlow()

        private val _incomingPackets = MutableSharedFlow<IncomingPacketEvent>(extraBufferCapacity = 64)
        override val incomingPackets: SharedFlow<IncomingPacketEvent> = _incomingPackets.asSharedFlow()

        private val _radioState = MutableStateFlow(BleRadioState(isAdvertising = true, isScanning = true))
        override val radioState: StateFlow<BleRadioState> = _radioState.asStateFlow()

        val sentPackets = CopyOnWriteArrayList<Pair<String, ByteArray>>()
        var nodeInfo: ConnectedPeerInfo? = null

        override fun start(nodeId: String, displayName: String, stealthMode: Boolean, scanMode: String) {
            nodeInfo = ConnectedPeerInfo(
                address = selfAddress,
                nodeId = nodeId,
                shortId = BanglaFormatters.nodeIdToShortId(nodeId),
                nodeHash32 = BanglaFormatters.nodeIdToHash32(nodeId),
                displayName = displayName,
                rssi = -55,
                isStealth = stealthMode
            )
            network.register(this)
        }

        override fun stop() {
            _readyPeers.value = emptyMap()
        }

        override fun isBluetoothReady(): Boolean = true

        fun updateConnectedNeighbors(neighbors: List<FakeMeshTransport>) {
            val map = LinkedHashMap<String, ConnectedPeerInfo>()
            neighbors.forEach { n ->
                val info = n.nodeInfo ?: return@forEach
                map[n.selfAddress] = info
            }
            _readyPeers.value = map
        }

        suspend fun deliver(fromAddress: String, packetBytes: ByteArray) {
            _incomingPackets.emit(IncomingPacketEvent(fromAddress, -55, packetBytes))
        }

        override suspend fun sendPacketToPeer(address: String, packetBytes: ByteArray): Boolean {
            val neighbor = _readyPeers.value[address] ?: return false
            sentPackets.add(address to packetBytes)
            val target = network.nodes[neighbor.address] ?: return false
            scope.launch {
                delay(5)
                target.deliver(selfAddress, packetBytes)
            }
            return true
        }

        override suspend fun broadcastPacket(packetBytes: ByteArray, excludeAddress: String?): Int {
            val targets = _readyPeers.value.keys.filter { it != excludeAddress }
            if (targets.isEmpty()) return 0
            targets.forEach { addr ->
                sentPackets.add(addr to packetBytes)
                val target = network.nodes[addr]
                if (target != null) {
                    scope.launch {
                        delay(5)
                        target.deliver(selfAddress, packetBytes)
                    }
                }
            }
            return targets.size
        }
    }

    private class FakeMeshNetwork {
        val nodes = LinkedHashMap<String, FakeMeshTransport>()
        private val edges = LinkedHashMap<String, MutableSet<String>>()

        fun register(transport: FakeMeshTransport) {
            nodes[transport.selfAddress] = transport
            edges.putIfAbsent(transport.selfAddress, linkedSetOf())
        }

        fun connect(a: FakeMeshTransport, b: FakeMeshTransport) {
            edges.getOrPut(a.selfAddress) { linkedSetOf() }.add(b.selfAddress)
            edges.getOrPut(b.selfAddress) { linkedSetOf() }.add(a.selfAddress)
            syncNeighbors()
        }

        fun disconnectAll(a: FakeMeshTransport) {
            edges[a.selfAddress]?.clear()
            edges.values.forEach { it.remove(a.selfAddress) }
            syncNeighbors()
        }

        private fun syncNeighbors() {
            nodes.forEach { (addr, transport) ->
                val neighborTransports = edges[addr].orEmpty().mapNotNull { nodes[it] }
                transport.updateConnectedNeighbors(neighborTransports)
            }
        }
    }

    private class SimulatedNode(
        val name: String,
        val nodeId: String,
        val address: String,
        context: Context,
        network: FakeMeshNetwork,
        tempDir: File,
        jitterRangeMs: LongRange = 10L..20L,
        clock: () -> Long = { System.currentTimeMillis() }
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val db = Room.inMemoryDatabaseBuilder(context, AlapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.alapDao()
        val dataStore = DataStoreManager(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(tempDir, "prefs_$address.preferences_pb")
            }
        )
        val transport = FakeMeshTransport(address, network, scope)
        val router = MeshRouter(
            transport = transport,
            dao = dao,
            dataStore = dataStore,
            scope = scope,
            jitterRangeMs = jitterRangeMs,
            clock = clock
        )

        suspend fun boot() {
            dataStore.setNodeIdForTest(nodeId)
            dataStore.updateDisplayName(name)
            transport.start(nodeId, name, stealthMode = false, scanMode = "Fast")
            router.start()
        }

        suspend fun registerPeer(other: SimulatedNode, hops: Int = 1) {
            dao.upsertMeshPeer(
                MeshPeerEntity(
                    nodeId = other.nodeId,
                    shortId = BanglaFormatters.nodeIdToShortId(other.nodeId).toInt() and 0xFFFF,
                    nodeHash32 = BanglaFormatters.nodeIdToHash32(other.nodeId),
                    name = other.name,
                    avatarShort = other.name.take(2),
                    hopCount = hops,
                    hopLabel = "$hops-Hop",
                    linkType = "BLE Mesh",
                    rssiDbm = -55,
                    signalLabel = "-55 dBm",
                    statusText = "READY",
                    pathSerialized = "",
                    isConnected = hops == 1
                )
            )
        }

        fun shutdown() {
            router.stop()
            transport.stop()
            scope.cancel()
            db.close()
        }
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000L, predicate: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (predicate()) return
            delay(25)
        }
        assertTrue("Condition timed out after ${timeoutMs}ms", predicate())
    }

    private fun newTempDir(): File = File.createTempFile("alap_sim_", "_dir").apply {
        delete()
        mkdirs()
    }

    @Test
    fun `three node line A-B-C relays message and returns end-to-end ACK`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = newTempDir()
        val net = FakeMeshNetwork()
        val nodeA = SimulatedNode("নিলয়", "ALAP-AAAA-111", "00:00:00:00:00:01", context, net, tempDir)
        val nodeB = SimulatedNode("রিলে-বি", "ALAP-BBBB-222", "00:00:00:00:00:02", context, net, tempDir)
        val nodeC = SimulatedNode("সাদিয়া", "ALAP-CCCC-333", "00:00:00:00:00:03", context, net, tempDir)

        try {
            nodeA.boot(); nodeB.boot(); nodeC.boot()
            nodeA.registerPeer(nodeC, hops = 2)
            nodeA.registerPeer(nodeB, hops = 1)
            nodeC.registerPeer(nodeA, hops = 2)
            nodeC.registerPeer(nodeB, hops = 1)

            // Line topology: A <-> B <-> C (A and C cannot reach directly)
            net.connect(nodeA.transport, nodeB.transport)
            net.connect(nodeB.transport, nodeC.transport)

            nodeA.dao.upsertConversation(
                ConversationEntity(
                    id = "conv_${nodeC.nodeId}",
                    name = nodeC.name,
                    avatarShort = "সা",
                    type = "direct",
                    hopBadge = "BLE Mesh",
                    lastMessage = "",
                    lastMessageTime = "--",
                    deliveryStatus = "",
                    peerNodeId = nodeC.nodeId
                )
            )

            val msgId = nodeA.router.sendTextMessage("conv_${nodeC.nodeId}", "হ্যালো সাদিয়া, মেশ দিয়ে!")
            assertNotNull(msgId)

            // C must receive the message at 2 hops
            awaitUntil {
                nodeC.dao.getMessageById(msgId!!) != null
            }
            val receivedAtC = nodeC.dao.getMessageById(msgId!!)!!
            assertEquals("হ্যালো সাদিয়া, মেশ দিয়ে!", receivedAtC.text)
            assertEquals(2, receivedAtC.hopCount)
            assertEquals(DeliveryStatus.RECEIVED, receivedAtC.deliveryStatus)

            // A must receive end-to-end ACK back through B and mark message DELIVERED
            awaitUntil {
                nodeA.dao.getMessageById(msgId)?.deliveryStatus == DeliveryStatus.DELIVERED
            }
            val updatedAtA = nodeA.dao.getMessageById(msgId)!!
            assertEquals(DeliveryStatus.DELIVERED, updatedAtA.deliveryStatus)
            assertEquals(2, updatedAtA.hopCount)
            assertTrue(nodeB.dataStore.settingsFlow.first().relayedMessageCount >= 1)
        } finally {
            nodeA.shutdown(); nodeB.shutdown(); nodeC.shutdown()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `four node diamond delivers once without routing loop`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = newTempDir()
        val net = FakeMeshNetwork()
        val nodeA = SimulatedNode("A", "ALAP-1111-AAA", "00:00:00:00:01:01", context, net, tempDir)
        val nodeB = SimulatedNode("B", "ALAP-2222-BBB", "00:00:00:00:01:02", context, net, tempDir)
        val nodeC = SimulatedNode("C", "ALAP-3333-CCC", "00:00:00:00:01:03", context, net, tempDir)
        val nodeD = SimulatedNode("D", "ALAP-4444-DDD", "00:00:00:00:01:04", context, net, tempDir)

        try {
            nodeA.boot(); nodeB.boot(); nodeC.boot(); nodeD.boot()
            // Diamond: A-B, A-C, B-D, C-D
            net.connect(nodeA.transport, nodeB.transport)
            net.connect(nodeA.transport, nodeC.transport)
            net.connect(nodeB.transport, nodeD.transport)
            net.connect(nodeC.transport, nodeD.transport)

            val msgId = nodeA.router.sendTextMessage("c_public", "পাবলিক চ্যানেল ব্রডকাস্ট")
            assertNotNull(msgId)

            awaitUntil {
                nodeD.dao.getMessageById(msgId!!) != null
            }
            // Give any potential duplicate relays a chance to arrive
            delay(200)
            val msgsAtD = nodeD.dao.observeMessages("c_public").first()
            assertEquals(1, msgsAtD.count { it.id == msgId })
        } finally {
            nodeA.shutdown(); nodeB.shutdown(); nodeC.shutdown(); nodeD.shutdown()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `ttl equals 1 stops forwarding at direct neighbor`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = newTempDir()
        val net = FakeMeshNetwork()
        val nodeA = SimulatedNode("A", "ALAP-1111-111", "00:00:00:00:02:01", context, net, tempDir)
        val nodeB = SimulatedNode("B", "ALAP-2222-222", "00:00:00:00:02:02", context, net, tempDir)
        val nodeC = SimulatedNode("C", "ALAP-3333-333", "00:00:00:00:02:03", context, net, tempDir)

        try {
            nodeA.boot(); nodeB.boot(); nodeC.boot()
            nodeA.dataStore.setMaxHopLimit(1)
            net.connect(nodeA.transport, nodeB.transport)
            net.connect(nodeB.transport, nodeC.transport)

            val msgId = nodeA.router.sendTextMessage("c_public", "TTL=1 সীমিত মেসেজ")
            assertNotNull(msgId)

            awaitUntil { nodeB.dao.getMessageById(msgId!!) != null }
            delay(180)
            assertEquals(null, nodeC.dao.getMessageById(msgId!!))
        } finally {
            nodeA.shutdown(); nodeB.shutdown(); nodeC.shutdown()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `jitter storm suppression cancels second neighbor relay when first relays earlier`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = newTempDir()
        val net = FakeMeshNetwork()
        val nodeA = SimulatedNode("A", "ALAP-1000-001", "00:00:00:00:03:01", context, net, tempDir, jitterRangeMs = 5L..5L)
        val fastRelay = SimulatedNode("Fast", "ALAP-2000-002", "00:00:00:00:03:02", context, net, tempDir, jitterRangeMs = 15L..15L)
        val slowRelay = SimulatedNode("Slow", "ALAP-3000-003", "00:00:00:00:03:03", context, net, tempDir, jitterRangeMs = 300L..300L)

        try {
            nodeA.boot(); fastRelay.boot(); slowRelay.boot()
            // Triangle: A-Fast, A-Slow, Fast-Slow
            net.connect(nodeA.transport, fastRelay.transport)
            net.connect(nodeA.transport, slowRelay.transport)
            net.connect(fastRelay.transport, slowRelay.transport)

            val msgId = nodeA.router.sendTextMessage("c_public", "স্টর্ম সাপ্রেশন টেস্ট")
            assertNotNull(msgId)

            // Wait past both jitter windows
            delay(450)
            // FastRelay forwarded it to SlowRelay at ~15ms; SlowRelay saw the duplicate from FastRelay
            // and cancelled its 300ms relay job -> SlowRelay sent 0 packets!
            assertEquals(1, fastRelay.transport.sentPackets.size)
            assertEquals(0, slowRelay.transport.sentPackets.size)
        } finally {
            nodeA.shutdown(); fastRelay.shutdown(); slowRelay.shutdown()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `store and forward queues offline message and flushes when peer connects unless expired`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = newTempDir()
        val net = FakeMeshNetwork()
        val fakeClock = AtomicLong(1_750_000_000_000L)
        val nodeA = SimulatedNode(
            "A", "ALAP-5000-001", "00:00:00:00:04:01",
            context, net, tempDir, clock = { fakeClock.get() }
        )
        val nodeB = SimulatedNode(
            "B", "ALAP-6000-002", "00:00:00:00:04:02",
            context, net, tempDir, clock = { fakeClock.get() }
        )

        try {
            nodeA.boot(); nodeB.boot()

            // Send with 0 peers -> QUEUED
            val validMsgId = nodeA.router.sendTextMessage("c_public", "অফলাইন কিউ মেসেজ")!!
            assertEquals(DeliveryStatus.QUEUED, nodeA.dao.getMessageById(validMsgId)?.deliveryStatus)
            assertEquals(1, nodeA.dao.getPendingPacketCount())

            // Connect B -> automatic readyPeers flush delivers & updates status
            net.connect(nodeA.transport, nodeB.transport)
            awaitUntil {
                nodeB.dao.getMessageById(validMsgId) != null &&
                    nodeA.dao.getPendingPacketCount() == 0
            }
            assertEquals(DeliveryStatus.SENT_TO_NEIGHBOR, nodeA.dao.getMessageById(validMsgId)?.deliveryStatus)

            // Disconnect B, queue another message, advance clock > 24h, reconnect B -> expired packet dropped
            net.disconnectAll(nodeB.transport)
            val expiredMsgId = nodeA.router.sendTextMessage("c_public", "পুরোনো মেসেজ")!!
            assertEquals(1, nodeA.dao.getPendingPacketCount())

            fakeClock.addAndGet(24L * 60L * 60L * 1000L + 10_000L)
            net.connect(nodeA.transport, nodeB.transport)
            awaitUntil { nodeA.dao.getPendingPacketCount() == 0 }
            delay(100)
            assertEquals(null, nodeB.dao.getMessageById(expiredMsgId))
        } finally {
            nodeA.shutdown(); nodeB.shutdown()
            tempDir.deleteRecursively()
        }
    }
}
