package com.alap.mesh.mesh.routing

import com.alap.mesh.data.DataStoreManager
import com.alap.mesh.data.local.AlapDao
import com.alap.mesh.data.local.ConversationEntity
import com.alap.mesh.data.local.DeliveryStatus
import com.alap.mesh.data.local.MeshPeerEntity
import com.alap.mesh.data.local.MessageEntity
import com.alap.mesh.data.local.PendingPacketEntity
import com.alap.mesh.data.local.SeenPacketEntity
import com.alap.mesh.mesh.protocol.AckPayload
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.mesh.protocol.PacketType
import com.alap.mesh.util.BanglaFormatters
import com.alap.mesh.util.DiagnosticLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class MeshRouter(
    private val transport: MeshTransport,
    private val dao: AlapDao,
    private val dataStore: DataStoreManager,
    private val scope: CoroutineScope,
    private val logger: DiagnosticLogger = DiagnosticLogger,
    private val jitterRangeMs: LongRange = 50L..300L,
    private val maxRelaysPerSecond: Int = 20,
    private val storeAndForwardMaxQueue: Int = 200,
    private val storeAndForwardTtlMs: Long = 24L * 60L * 60L * 1000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val lruLock = Any()
    private val seenLru = object : LinkedHashMap<String, Long>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 512
    }

    private val pendingRelays = ConcurrentHashMap<String, RelayJobEntry>()
    private val pendingPings = ConcurrentHashMap<Long, CompletableDeferred<PingAckResult>>()
    private val relayTimestampsMs = ArrayDeque<Long>()

    private var collectJob: Job? = null
    private var readyPeerFlushJob: Job? = null
    private var periodicFlushJob: Job? = null
    private var lastReadyAddresses: Set<String> = emptySet()

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    data class RelayJobEntry(
        val originFromAddress: String,
        val job: Job
    )

    data class PingAckResult(
        val fromNodeId: String,
        val hopCount: Int,
        val rttMs: Long
    )

    fun start() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch {
            transport.incomingPackets.collect { event ->
                handleIncomingPacket(event)
            }
        }
        readyPeerFlushJob = scope.launch {
            transport.readyPeers.collect { peers ->
                val currentAddrs = peers.keys
                val newlyReady = currentAddrs - lastReadyAddresses
                lastReadyAddresses = currentAddrs
                if (peers.isNotEmpty() && newlyReady.isNotEmpty()) {
                    flushStoreAndForwardQueue()
                }
            }
        }
        periodicFlushJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                flushStoreAndForwardQueue()
            }
        }
    }

    fun stop() {
        collectJob?.cancel()
        readyPeerFlushJob?.cancel()
        periodicFlushJob?.cancel()
        pendingRelays.values.forEach { it.job.cancel() }
        pendingRelays.clear()
    }

    suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        isSos: Boolean = false
    ): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val bounded = if (clean.length > BinaryMeshPacket.MAX_TEXT_CHARS) {
            clean.take(BinaryMeshPacket.MAX_TEXT_CHARS)
        } else {
            clean
        }

        val settings = dataStore.settingsFlow.first()
        val localNodeId = settings.nodeId.ifBlank { dataStore.ensureNodeIdInitialized() }
        val localHash = BanglaFormatters.nodeIdToHash32(localNodeId)
        val now = clock()
        val timeLabel = timeFormat.format(Date(now))

        val conv = dao.getConversationById(conversationId)
        val peerNodeId = conv?.peerNodeId.orEmpty()
        val isBroadcastChannel = peerNodeId.isBlank() || peerNodeId == "*"
        val recipientHash = if (isBroadcastChannel) {
            BinaryMeshPacket.BROADCAST_HASH
        } else {
            BanglaFormatters.nodeIdToHash32(peerNodeId)
        }

        val packetType = when {
            isSos -> PacketType.SOS
            isBroadcastChannel -> PacketType.PUBLIC
            else -> PacketType.DATA
        }
        val flags = if (isSos) BinaryMeshPacket.FLAG_SOS.toByte() else 0.toByte()

        val packet = BinaryMeshPacket(
            type = packetType,
            flags = flags,
            ttl = settings.maxHopLimit.coerceIn(1, 6),
            hopCount = 0,
            senderHash = localHash,
            recipientHash = recipientHash,
            timestampSeconds = (now / 1000L).toInt(),
            hopShortIds = emptyList(),
            payload = bounded.toByteArray(Charsets.UTF_8)
        )
        val msgIdHex = packet.messageIdHex()
        markSeen(msgIdHex, now)

        dao.insertMessage(
            MessageEntity(
                id = msgIdHex,
                conversationId = conversationId,
                senderId = localNodeId,
                senderName = settings.displayName,
                isMe = true,
                text = bounded,
                timeLabel = timeLabel,
                timestampMillis = now,
                deliveryStatus = DeliveryStatus.SENDING,
                hopCount = 0,
                hopMeta = DeliveryStatus.toBanglaLabel(DeliveryStatus.SENDING),
                hopPathSerialized = "আপনি ($localNodeId)",
                isSos = isSos
            )
        )

        val encoded = packet.encode()
        val sentCount = transport.broadcastPacket(encoded)
        val finalStatus = if (sentCount > 0) {
            dataStore.incrementTxPacket(sentCount)
            logger.log("TX", "Router", "মেসেজ $msgIdHex পাঠানো হয়েছে ($sentCount প্রতিবেশী)")
            DeliveryStatus.SENT_TO_NEIGHBOR
        } else {
            enqueuePendingPacket(msgIdHex, recipientHash, encoded, now)
            logger.log("INFO", "StoreFwd", "কোনো পিয়ার সংযুক্ত নেই; $msgIdHex কিউতে রাখা হলো")
            DeliveryStatus.QUEUED
        }

        val statusLabel = DeliveryStatus.toBanglaLabel(finalStatus)
        dao.updateMessageDelivery(
            messageId = msgIdHex,
            status = finalStatus,
            hopCount = 0,
            hopMeta = statusLabel,
            hopPathSerialized = "আপনি ($localNodeId)"
        )

        val convTitle = conv?.name ?: if (conversationId == "c_public") "পাবলিক এরিয়া চ্যানেল" else "মেশ চ্যানেল"
        dao.upsertConversation(
            ConversationEntity(
                id = conversationId,
                name = convTitle,
                avatarShort = conv?.avatarShort ?: convTitle.take(2),
                type = conv?.type ?: if (isBroadcastChannel) "channel" else "direct",
                hopBadge = conv?.hopBadge ?: "BLE Mesh",
                unreadCount = 0,
                lastMessage = bounded,
                lastMessageTime = timeLabel,
                deliveryStatus = statusLabel,
                hasFile = conv?.hasFile ?: false,
                peerNodeId = if (isBroadcastChannel) "*" else peerNodeId,
                updatedAtMillis = now
            )
        )

        return msgIdHex
    }

    suspend fun sendPingAndAwaitRtt(timeoutMs: Long = 5_000L): List<PingAckResult> {
        val ready = transport.readyPeers.value
        if (ready.isEmpty()) return emptyList()

        val settings = dataStore.settingsFlow.first()
        val localNodeId = settings.nodeId.ifBlank { dataStore.ensureNodeIdInitialized() }
        val localHash = BanglaFormatters.nodeIdToHash32(localNodeId)
        val sentAt = clock()

        val ping = BinaryMeshPacket(
            type = PacketType.PING,
            ttl = settings.maxHopLimit.coerceIn(1, 6),
            hopCount = 0,
            senderHash = localHash,
            recipientHash = BinaryMeshPacket.BROADCAST_HASH,
            timestampSeconds = (sentAt / 1000L).toInt(),
            payload = localNodeId.toByteArray(Charsets.UTF_8)
        )
        markSeen(ping.messageIdHex(), sentAt)

        val deferred = CompletableDeferred<PingAckResult>()
        pendingPings[ping.messageId] = deferred
        val sentCount = transport.broadcastPacket(ping.encode())
        if (sentCount == 0) {
            pendingPings.remove(ping.messageId)
            return emptyList()
        }
        dataStore.incrementTxPacket(sentCount)
        logger.log("TX", "Ping", "মেশ পিং পাঠানো হয়েছে ($sentCount প্রতিবেশী)")

        val result = withTimeoutOrNull(timeoutMs) { deferred.await() }
        pendingPings.remove(ping.messageId)
        return if (result != null) listOf(result.copy(rttMs = (clock() - sentAt).coerceAtLeast(1L))) else emptyList()
    }

    suspend fun flushStoreAndForwardQueue() {
        val now = clock()
        val expiredCount = dao.deleteExpiredPendingPackets(now)
        if (expiredCount > 0) {
            logger.log("INFO", "StoreFwd", "${expiredCount}টি ২৪ঘণ্টা পুরোনো কিউ প্যাকেট মুছে ফেলা হয়েছে")
        }
        val valid = dao.getValidPendingPackets(now)
        if (valid.isEmpty() || transport.readyPeers.value.isEmpty()) return

        for (pending in valid) {
            val sent = transport.broadcastPacket(pending.packetBytes)
            if (sent > 0) {
                dao.deletePendingPacket(pending.messageIdHex)
                dataStore.incrementTxPacket(sent)
                val msg = dao.getMessageById(pending.messageIdHex)
                if (msg != null && msg.deliveryStatus == DeliveryStatus.QUEUED) {
                    val label = DeliveryStatus.toBanglaLabel(DeliveryStatus.SENT_TO_NEIGHBOR)
                    dao.updateMessageDelivery(
                        messageId = msg.id,
                        status = DeliveryStatus.SENT_TO_NEIGHBOR,
                        hopCount = 0,
                        hopMeta = label,
                        hopPathSerialized = msg.hopPathSerialized
                    )
                    dao.updateConversationDeliveryStatus(msg.conversationId, label)
                }
                logger.log("TX", "StoreFwd", "কিউ প্যাকেট ${pending.messageIdHex} পাঠানো হয়েছে ($sent পিয়ার)")
            } else {
                dao.incrementPendingRetry(pending.messageIdHex)
            }
        }
    }

    internal suspend fun handleIncomingPacket(event: IncomingPacketEvent) {
        val packet = BinaryMeshPacket.decode(event.packetBytes) ?: return
        val now = clock()
        if (!packet.isTimestampWithinSkew((now / 1000L).toInt())) {
            logger.log("INFO", "Router", "সময়ের ব্যবধান >১০মিনিট; প্যাকেট ${packet.messageIdHex()} বাতিল")
            return
        }

        val msgIdHex = packet.messageIdHex()
        val alreadySeen = isAlreadySeen(msgIdHex)
        if (alreadySeen) {
            // Broadcast storm suppression: cancel pending relay if another neighbor already relayed it
            val pending = pendingRelays[msgIdHex]
            if (pending != null && pending.originFromAddress != event.fromAddress) {
                pending.job.cancel()
                pendingRelays.remove(msgIdHex)
                logger.log("RELAY", "StormCtrl", "প্রতিবেশী ইতিমধ্যে রিলে করেছে, ${msgIdHex}-এর রিলে বাতিল")
            }
            return
        }

        markSeen(msgIdHex, now)
        dataStore.incrementRxPacket()

        val settings = dataStore.settingsFlow.first()
        val localNodeId = settings.nodeId.ifBlank { dataStore.ensureNodeIdInitialized() }
        val localHash = BanglaFormatters.nodeIdToHash32(localNodeId)
        val localShortId = BanglaFormatters.nodeIdToShortId(localNodeId)

        val isForMe = packet.recipientHash == localHash
        val isBroadcast = packet.recipientHash == BinaryMeshPacket.BROADCAST_HASH

        if (isForMe || isBroadcast) {
            processLocalDelivery(packet, event.fromAddress, localNodeId, localHash, localShortId, now, settings.maxHopLimit)
        }

        // Schedule controlled relay if not addressed solely to us, relay is enabled, and TTL > 1
        if (!isForMe && settings.relayModeEnabled && packet.ttl > 1) {
            scheduleJitteredRelay(packet, event.fromAddress, localShortId)
        }
    }

    private suspend fun processLocalDelivery(
        packet: BinaryMeshPacket,
        fromAddress: String,
        localNodeId: String,
        localHash: Int,
        localShortId: Short,
        now: Long,
        maxHopLimit: Int
    ) {
        val senderPeer = dao.getPeerByHash(packet.senderHash)
            ?: transport.readyPeers.value.values.firstOrNull { it.nodeHash32 == packet.senderHash }?.let {
                MeshPeerEntity(
                    nodeId = it.nodeId,
                    shortId = it.shortId.toInt() and 0xFFFF,
                    nodeHash32 = it.nodeHash32,
                    name = it.displayName,
                    avatarShort = it.displayName.take(2),
                    hopCount = packet.hopCount.coerceAtLeast(1),
                    hopLabel = "${packet.hopCount.coerceAtLeast(1)}-Hop",
                    linkType = "BLE Mesh",
                    rssiDbm = it.rssi,
                    signalLabel = "${it.rssi} dBm",
                    statusText = "Online",
                    pathSerialized = ""
                )
            }

        val senderId = senderPeer?.nodeId ?: "ALAP-${BanglaFormatters.hash32Hex(packet.senderHash).take(8)}"
        val senderName = senderPeer?.name ?: "নোড ${BanglaFormatters.hash32Hex(packet.senderHash).take(4)}"

        when (packet.type) {
            PacketType.ACK -> {
                val ack = AckPayload.decode(packet.payload) ?: return
                val ackedHex = "%016X".format(ack.ackedMessageId)

                // Check if this is a PING ACK
                pendingPings[ack.ackedMessageId]?.let { pingDeferred ->
                    pingDeferred.complete(
                        PingAckResult(
                            fromNodeId = senderId,
                            hopCount = ack.reachedHopCount.coerceAtLeast(1),
                            rttMs = 0L
                        )
                    )
                    logger.log("ACK", "Ping", "$senderId থেকে পিয়ার ACK (${ack.reachedHopCount} হপ)")
                    return
                }

                val existingMsg = dao.getMessageById(ackedHex) ?: return
                val allPeers = dao.getAllPeersList()
                val routeNames = buildRouteNames(localNodeId, ack.routeShortIds, allPeers)
                val hops = ack.reachedHopCount.coerceAtLeast(1)
                val label = DeliveryStatus.toBanglaLabel(DeliveryStatus.DELIVERED, hops)
                dao.updateMessageDelivery(
                    messageId = ackedHex,
                    status = DeliveryStatus.DELIVERED,
                    hopCount = hops,
                    hopMeta = label,
                    hopPathSerialized = routeNames.joinToString("|")
                )
                dao.updateConversationDeliveryStatus(existingMsg.conversationId, label)
                logger.log("ACK", "Delivery", "মেসেজ $ackedHex পৌঁছেছে ($hops হপ)")
            }

            PacketType.PING -> {
                if (packet.senderHash != localHash) {
                    sendAckBack(
                        targetHash = packet.senderHash,
                        ackedMessageId = packet.messageId,
                        hops = packet.hopCount.coerceAtLeast(1),
                        routeWithSelf = packet.hopShortIds + localShortId,
                        ttl = maxHopLimit,
                        localHash = localHash,
                        preferredAddress = fromAddress
                    )
                }
            }

            PacketType.DATA, PacketType.PUBLIC, PacketType.SOS -> {
                if (packet.senderHash == localHash) return
                val text = String(packet.payload, Charsets.UTF_8)
                val isChannel = packet.recipientHash == BinaryMeshPacket.BROADCAST_HASH
                val convId = if (isChannel) "c_public" else "conv_$senderId"
                val existingConv = dao.getConversationById(convId)
                val timeLabel = timeFormat.format(Date(now))
                val hops = (packet.hopCount + 1).coerceAtLeast(1)
                val allPeers = dao.getAllPeersList()
                val routeNames = buildIncomingRouteNames(senderName, packet.hopShortIds, localNodeId, allPeers)

                dao.upsertConversation(
                    ConversationEntity(
                        id = convId,
                        name = if (isChannel) "পাবলিক এরিয়া চ্যানেল" else (existingConv?.name ?: senderName),
                        avatarShort = if (isChannel) "পা" else (existingConv?.avatarShort ?: senderName.take(2)),
                        type = if (isChannel) "channel" else "direct",
                        hopBadge = "${BanglaFormatters.toBanglaDigits(hops)}-Hop • BLE Mesh",
                        unreadCount = (existingConv?.unreadCount ?: 0) + 1,
                        lastMessage = text,
                        lastMessageTime = timeLabel,
                        deliveryStatus = if (packet.isSos) "SOS Alert" else DeliveryStatus.toBanglaLabel(DeliveryStatus.RECEIVED, hops),
                        hasFile = existingConv?.hasFile ?: false,
                        peerNodeId = if (isChannel) "*" else senderId,
                        updatedAtMillis = now
                    )
                )

                dao.insertMessage(
                    MessageEntity(
                        id = packet.messageIdHex(),
                        conversationId = convId,
                        senderId = senderId,
                        senderName = senderName,
                        isMe = false,
                        text = text,
                        timeLabel = timeLabel,
                        timestampMillis = now,
                        deliveryStatus = DeliveryStatus.RECEIVED,
                        hopCount = hops,
                        hopMeta = DeliveryStatus.toBanglaLabel(DeliveryStatus.RECEIVED, hops),
                        hopPathSerialized = routeNames.joinToString("|"),
                        isSos = packet.isSos
                    )
                )
                logger.log("RX", "Router", "${senderName} থেকে মেসেজ ${packet.messageIdHex()} ($hops হপ)")

                if (packet.type == PacketType.DATA) {
                    sendAckBack(
                        targetHash = packet.senderHash,
                        ackedMessageId = packet.messageId,
                        hops = hops,
                        routeWithSelf = packet.hopShortIds + localShortId,
                        ttl = maxHopLimit,
                        localHash = localHash,
                        preferredAddress = fromAddress
                    )
                }
            }

            PacketType.HANDSHAKE -> Unit
        }
    }

    private suspend fun sendAckBack(
        targetHash: Int,
        ackedMessageId: Long,
        hops: Int,
        routeWithSelf: List<Short>,
        ttl: Int,
        localHash: Int,
        preferredAddress: String
    ) {
        val now = clock()
        val ackPayload = AckPayload(ackedMessageId, hops, routeWithSelf)
        val ackPacket = BinaryMeshPacket(
            type = PacketType.ACK,
            ttl = ttl.coerceIn(1, 6),
            hopCount = 0,
            senderHash = localHash,
            recipientHash = targetHash,
            timestampSeconds = (now / 1000L).toInt(),
            payload = ackPayload.encode()
        )
        markSeen(ackPacket.messageIdHex(), now)
        val encoded = ackPacket.encode()
        val sentDirect = transport.sendPacketToPeer(preferredAddress, encoded)
        if (!sentDirect) {
            transport.broadcastPacket(encoded)
        }
        dataStore.incrementTxPacket()
    }

    private fun scheduleJitteredRelay(
        packet: BinaryMeshPacket,
        fromAddress: String,
        localShortId: Short
    ) {
        val msgIdHex = packet.messageIdHex()
        val forwarded = packet.forwardCopy(localShortId) ?: return
        val jitterMs = if (jitterRangeMs.first == jitterRangeMs.last) {
            jitterRangeMs.first
        } else {
            Random.nextLong(jitterRangeMs.first, jitterRangeMs.last + 1)
        }

        val job = scope.launch {
            if (jitterMs > 0) delay(jitterMs)
            pendingRelays.remove(msgIdHex)
            if (!canRelayNow()) {
                logger.log("RELAY", "RateLimit", "রিলে সীমা পূর্ণ; $msgIdHex বাদ দেওয়া হলো")
                return@launch
            }
            val sent = transport.broadcastPacket(forwarded.encode(), excludeAddress = fromAddress)
            if (sent > 0) {
                dataStore.incrementRelayedCount()
                dataStore.incrementTxPacket(sent)
                logger.log("RELAY", "Router", "প্যাকেট $msgIdHex রিলে (${forwarded.hopCount} হপ, $sent প্রতিবেশী)")
            }
        }
        pendingRelays[msgIdHex] = RelayJobEntry(fromAddress, job)
    }

    private fun canRelayNow(): Boolean {
        val now = clock()
        synchronized(relayTimestampsMs) {
            while (relayTimestampsMs.isNotEmpty() && now - relayTimestampsMs.first() >= 1_000L) {
                relayTimestampsMs.removeFirst()
            }
            if (relayTimestampsMs.size >= maxRelaysPerSecond) return false
            relayTimestampsMs.addLast(now)
            return true
        }
    }

    private suspend fun isAlreadySeen(msgIdHex: String): Boolean {
        synchronized(lruLock) {
            if (seenLru.containsKey(msgIdHex)) return true
        }
        val inDb = dao.hasSeenPacket(msgIdHex)
        if (inDb) {
            synchronized(lruLock) { seenLru[msgIdHex] = clock() }
        }
        return inDb
    }

    private suspend fun markSeen(msgIdHex: String, now: Long) {
        synchronized(lruLock) {
            seenLru[msgIdHex] = now
        }
        dao.insertSeenPacket(SeenPacketEntity(msgIdHex, now))
        if (dao.getSeenPacketCount() > 1000) {
            dao.deleteOldestSeenPackets(200)
        }
    }

    private suspend fun enqueuePendingPacket(
        msgIdHex: String,
        recipientHash: Int,
        packetBytes: ByteArray,
        now: Long
    ) {
        val count = dao.getPendingPacketCount()
        if (count >= storeAndForwardMaxQueue) {
            dao.deleteOldestPendingPackets((count - storeAndForwardMaxQueue) + 1)
        }
        dao.insertPendingPacket(
            PendingPacketEntity(
                messageIdHex = msgIdHex,
                recipientHash = recipientHash,
                packetBytes = packetBytes,
                createdAtMillis = now,
                expiresAtMillis = now + storeAndForwardTtlMs,
                retryCount = 0
            )
        )
    }

    private fun buildRouteNames(
        localNodeId: String,
        shortIds: List<Short>,
        peers: List<MeshPeerEntity>
    ): List<String> {
        val names = mutableListOf("আপনি ($localNodeId)")
        shortIds.forEachIndexed { idx, sId ->
            val uShort = sId.toInt() and 0xFFFF
            val match = peers.firstOrNull { it.shortId == uShort }
            val label = match?.name ?: "রিলে ${BanglaFormatters.shortIdHex(sId)}"
            names.add("$label (হপ ${BanglaFormatters.toBanglaDigits(idx + 1)})")
        }
        return names
    }

    private fun buildIncomingRouteNames(
        senderName: String,
        relayShorts: List<Short>,
        localNodeId: String,
        peers: List<MeshPeerEntity>
    ): List<String> {
        val names = mutableListOf("$senderName (উৎস)")
        relayShorts.forEachIndexed { idx, sId ->
            val uShort = sId.toInt() and 0xFFFF
            val match = peers.firstOrNull { it.shortId == uShort }
            val label = match?.name ?: "রিলে ${BanglaFormatters.shortIdHex(sId)}"
            names.add("$label (হপ ${BanglaFormatters.toBanglaDigits(idx + 1)})")
        }
        names.add("আপনি ($localNodeId)")
        return names
    }
}
