package com.alap.mesh.mesh.ble

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Serializes BLE GATT characteristic writes or server notifications for a single connection
 * so that the next chunk never starts before onCharacteristicWrite / onNotificationSent completes.
 */
class GattWriteQueue(
    private val timeoutMs: Long = 4_000L
) {
    private val sendMutex = Mutex()
    private val completionLock = Any()
    private var activeAck: CompletableDeferred<Boolean>? = null

    suspend fun sendChunksSerially(
        chunks: List<ByteArray>,
        dispatchChunk: (ByteArray) -> Boolean
    ): Boolean = sendMutex.withLock {
        if (chunks.isEmpty()) return@withLock true
        for (chunk in chunks) {
            val deferred = CompletableDeferred<Boolean>()
            synchronized(completionLock) {
                activeAck?.complete(false)
                activeAck = deferred
            }

            val initiated = try {
                dispatchChunk(chunk)
            } catch (_: Exception) {
                false
            }

            if (!initiated) {
                synchronized(completionLock) {
                    if (activeAck === deferred) activeAck = null
                }
                return@withLock false
            }

            val callbackSuccess = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            } ?: false

            synchronized(completionLock) {
                if (activeAck === deferred) activeAck = null
            }

            if (!callbackSuccess) {
                return@withLock false
            }
        }
        true
    }

    fun onWriteCallback(success: Boolean) {
        synchronized(completionLock) {
            activeAck?.complete(success)
            activeAck = null
        }
    }

    fun cancelAll() {
        synchronized(completionLock) {
            activeAck?.complete(false)
            activeAck = null
        }
    }
}
