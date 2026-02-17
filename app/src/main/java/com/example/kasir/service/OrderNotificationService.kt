package com.example.kasir.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kasir.MainActivity
import com.example.kasir.R
import com.example.kasir.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import io.socket.client.Socket
import org.json.JSONObject
import androidx.compose.animation.ExperimentalAnimationApi

@ExperimentalAnimationApi
class OrderNotificationService : Service() {

    private var mSocket: Socket? = null
    private val CHANNEL_ID = "order_notifications_v2" // Changed ID to force update sound
    private val SERVICE_CHANNEL_ID = "order_service_channel"
    private val NOTIFICATION_ID = 1
    private val SERVICE_NOTIFICATION_ID = 999
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        Log.d("OrderService", "Service Created")
        createNotificationChannel()
        try {
            startForeground(SERVICE_NOTIFICATION_ID, createForegroundNotification())
            Log.d("OrderService", "Service Started Foreground")
        } catch (e: Exception) {
             Log.e("OrderService", "Failed to start foreground", e)
        }
        
        // Setup Socket only if logged in
        serviceScope.launch {
            SessionManager.loadSession(applicationContext)
            if (SessionManager.isLoggedIn()) {
                setupSocket()
            } else {
                Log.d("OrderService", "User not logged in, socket setup skipped")
                stopSelf()
            }
        }
    }

    private fun setupSocket() {
        try {
            // BUG FIX 1: Use independent socket connection, NOT Singleton
            // This ensures Service stays connected even if ViewModel disconnects
            val options = io.socket.client.IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                forceNew = true // Force new connection
            }
            
            mSocket = io.socket.client.IO.socket(com.example.kasir.BuildConfig.API_BASE_URL, options)

            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d("OrderService", "Socket Connected: ${mSocket?.id()}")
            }

            mSocket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("OrderService", "Socket Disconnected")
            }

            mSocket?.on("new_order") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as? JSONObject
                        // Fallback parsing if JSON isn't directly castable (sometimes happens with different socket versions)
                        val finalData = if (data == null && args[0] is String) {
                             JSONObject(args[0] as String)
                        } else {
                             data
                        }

                        val transactionCode = finalData?.optString("transactionCode") ?: "Pesanan Baru"
                        val customerName = finalData?.optString("customerName") ?: "Pelanggan"
                        
                        Log.d("OrderService", "New Order Received: $transactionCode")
                        showOrderNotification(transactionCode, customerName)
                    } catch (e: Exception) {
                        Log.e("OrderService", "Error parsing new_order", e)
                    }
                }
            }

            mSocket?.connect()

        } catch (e: Exception) {
            Log.e("OrderService", "Socket Setup Failed", e)
        }
    }

    private fun showOrderNotification(code: String, name: String) {
        // --- LOGIC BARU: Cek Foreground ---
        if (com.example.kasir.utils.AppLifecycleObserver.isAppInForeground) {
            Log.d("OrderService", "App in foreground, skipping notification")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        // Custom Sound Logic
        // We use the default notification sound uri constructed pointing to the resource
        // UPDATED: Use sound_pesanan
        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + R.raw.sound_pesanan)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Use app icon or custom small icon
            .setContentTitle("📦 Pesanan Baru Masuk!")
            .setContentText("$name - $code")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // ID should be unique per order if we want stacking, or fixed if we want to replace
        // Use hashcode of code to stack? Or just fixed ID for simplicity?
        // Let's use System.currentTimeMillis to stack them so user doesn't miss multiple orders
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createForegroundNotification(): Notification {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SERVICE_CHANNEL_ID
        } else {
            ""
        }
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Kasir Service Aktif")
            .setContentText("Menunggu pesanan masuk...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Service Channel (Silent, Low Importance)
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Kasir Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            
            // 2. Order Notification Channel (High Importance, Sound)
            val orderChannel = NotificationChannel(
                CHANNEL_ID, // V2
                "Notifikasi Pesanan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat ada pesanan baru masuk"
                enableVibration(true)
                
                // Custom Sound Setup for Channel (Android 8+)
                // UPDATED: Use sound_pesanan
                val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + R.raw.sound_pesanan)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(orderChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If service killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mSocket?.disconnect() // Clean up
        mSocket?.off("new_order")
    }
}
