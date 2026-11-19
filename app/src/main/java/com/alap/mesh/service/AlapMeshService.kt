package com.alap.mesh.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alap.mesh.MainActivity
import com.alap.mesh.R
import com.alap.mesh.mesh.AlapMeshRepository
import com.alap.mesh.util.BanglaFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlapMeshService : Service() {

    companion object {
        const val CHANNEL_ID = "alap_mesh_service_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_PEER_COUNT = "extra_peer_count"
        const val ACTION_STOP_MESH = "com.alap.mesh.ACTION_STOP_MESH"

        @Volatile
        private var isServiceRunning = false

        fun startOrUpdate(context: Context, peerCount: Int = 0) {
            val intent = Intent(context, AlapMeshService::class.java).apply {
                putExtra(EXTRA_PEER_COUNT, peerCount.coerceAtLeast(0))
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun updateNotificationIfRunning(context: Context, peerCount: Int) {
            if (!isServiceRunning) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.notify(NOTIFICATION_ID, buildNotificationStatic(context, peerCount.coerceAtLeast(0)))
        }

        private fun buildNotificationStatic(context: Context, peerCount: Int): Notification {
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val stopIntent = Intent(context, AlapMeshService::class.java).apply {
                action = ACTION_STOP_MESH
            }
            val stopPending = PendingIntent.getService(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val statusText = if (peerCount <= 0) {
                "কোনো ডিভাইস সংযুক্ত নেই"
            } else {
                "আলাপ মেশ সক্রিয় • ${BanglaFormatters.toBanglaDigits(peerCount)}টি ডিভাইস সংযুক্ত"
            }

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.mesh_service_notification_title))
                .setContentText(statusText)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(0, "মেশ বন্ধ করুন", stopPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MESH) {
            serviceScope.launch {
                AlapMeshRepository.getInstance(applicationContext).stopMeshEngine()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val peerCount = (intent?.getIntExtra(EXTRA_PEER_COUNT, 0) ?: 0).coerceAtLeast(0)
        val notification = buildNotificationStatic(this, peerCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.mesh_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "অফলাইন ব্লুটুথ ও ওয়াই-ফাই মেশ ব্যাকগ্রাউন্ড সার্ভিস"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }
}
