package com.example.kasir.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kasir.MainActivity
import com.example.kasir.R
import com.example.kasir.utils.AppLifecycleObserver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@androidx.compose.animation.ExperimentalAnimationApi
class FCMNotificationService : FirebaseMessagingService() {

    private val CHANNEL_ID = "order_notifications_v2"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM Token obtained: $token")
        // TODO: Send this new token to your backend API to register this device
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Extract data payload sent by the server
        val transactionCode = message.data["transactionCode"] ?: message.notification?.title ?: "Pesanan Baru"
        val customerName = message.data["customerName"] ?: message.notification?.body ?: "Pelanggan"

        Log.d("FCMService", "New Order Received via FCM: $transactionCode")
        
        showOrderNotification(transactionCode, customerName)
    }

    private fun showOrderNotification(code: String, name: String) {
        // --- LOGIC: Cek Foreground ---
        if (AppLifecycleObserver.isAppInForeground) {
            Log.d("FCMService", "App in foreground, skipping notification")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        // Custom Sound Logic reuse
        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + R.raw.sound_pesanan)

        createNotificationChannel(soundUri)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) 
            .setContentTitle("📦 Pesanan Baru Masuk!")
            .setContentText("$name - $code")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel(soundUri: android.net.Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val orderChannel = NotificationChannel(
                CHANNEL_ID,
                "Notifikasi Pesanan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat ada pesanan baru masuk"
                enableVibration(true)
                
                // Custom Sound Setup for Channel
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(orderChannel)
        }
    }
}
