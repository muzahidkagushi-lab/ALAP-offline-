package com.alap.mesh.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlapDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAtMillis DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE peerNodeId = :peerNodeId LIMIT 1")
    suspend fun getConversationByPeerNodeId(peerNodeId: String): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: String)

    @Query("UPDATE conversations SET deliveryStatus = :deliveryStatus WHERE id = :conversationId")
    suspend fun updateConversationDeliveryStatus(conversationId: String, deliveryStatus: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query(
        "UPDATE messages SET deliveryStatus = :status, hopCount = :hopCount, hopMeta = :hopMeta, hopPathSerialized = :hopPathSerialized WHERE id = :messageId"
    )
    suspend fun updateMessageDelivery(
        messageId: String,
        status: String,
        hopCount: Int,
        hopMeta: String,
        hopPathSerialized: String
    )

    @Query("SELECT * FROM file_transfers ORDER BY timestampMillis DESC")
    fun observeFileTransfers(): Flow<List<FileTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFileTransfer(fileTransfer: FileTransferEntity)

    @Query("UPDATE file_transfers SET progressPercent = :progress, speedText = :speedText, status = :status WHERE id = :fileId")
    suspend fun updateFileProgress(fileId: String, progress: Int, speedText: String, status: String)

    @Query("SELECT * FROM mesh_peers ORDER BY isConnected DESC, hopCount ASC, rssiDbm DESC")
    fun observeMeshPeers(): Flow<List<MeshPeerEntity>>

    @Query("SELECT * FROM mesh_peers")
    suspend fun getAllPeersList(): List<MeshPeerEntity>

    @Query("SELECT * FROM mesh_peers WHERE nodeHash32 = :hash LIMIT 1")
    suspend fun getPeerByHash(hash: Int): MeshPeerEntity?

    @Query("SELECT * FROM mesh_peers WHERE shortId = :shortId LIMIT 1")
    suspend fun getPeerByShortId(shortId: Int): MeshPeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeshPeer(peer: MeshPeerEntity)

    @Query("DELETE FROM mesh_peers WHERE lastSeenMillis < :cutoffMillis AND isConnected = 0")
    suspend fun deleteStaleDisconnectedPeers(cutoffMillis: Long)

    @Query("UPDATE mesh_peers SET isConnected = 0")
    suspend fun markAllPeersDisconnected()

    @Query("DELETE FROM mesh_peers")
    suspend fun clearAllPeers()

    // Store-and-Forward Pending Packets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingPacket(packet: PendingPacketEntity)

    @Query("SELECT * FROM pending_packets WHERE expiresAtMillis > :nowMillis ORDER BY createdAtMillis ASC")
    suspend fun getValidPendingPackets(nowMillis: Long): List<PendingPacketEntity>

    @Query("SELECT COUNT(*) FROM pending_packets")
    suspend fun getPendingPacketCount(): Int

    @Query("DELETE FROM pending_packets WHERE messageIdHex = :messageIdHex")
    suspend fun deletePendingPacket(messageIdHex: String)

    @Query("DELETE FROM pending_packets WHERE expiresAtMillis <= :nowMillis")
    suspend fun deleteExpiredPendingPackets(nowMillis: Long): Int

    @Query(
        "DELETE FROM pending_packets WHERE messageIdHex IN (SELECT messageIdHex FROM pending_packets ORDER BY createdAtMillis ASC LIMIT :limit)"
    )
    suspend fun deleteOldestPendingPackets(limit: Int)

    @Query("UPDATE pending_packets SET retryCount = retryCount + 1 WHERE messageIdHex = :messageIdHex")
    suspend fun incrementPendingRetry(messageIdHex: String)

    // Deduplication Cache (Persistent)
    @Query("SELECT EXISTS(SELECT 1 FROM seen_packets WHERE messageIdHex = :messageIdHex)")
    suspend fun hasSeenPacket(messageIdHex: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeenPacket(packet: SeenPacketEntity)

    @Query("SELECT COUNT(*) FROM seen_packets")
    suspend fun getSeenPacketCount(): Int

    @Query(
        "DELETE FROM seen_packets WHERE messageIdHex IN (SELECT messageIdHex FROM seen_packets ORDER BY seenAtMillis ASC LIMIT :limit)"
    )
    suspend fun deleteOldestSeenPackets(limit: Int)
}
