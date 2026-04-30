package id.quacxel.mejapesan.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import id.quacxel.mejapesan.MainActivity
import id.quacxel.mejapesan.R
import id.quacxel.mejapesan.utils.SessionManager
import id.quacxel.mejapesan.utils.NotificationUtils
import id.quacxel.mejapesan.data.network.FcmTokenRequest
import id.quacxel.mejapesan.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@androidx.compose.animation.ExperimentalAnimationApi
class FCMNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM Token obtained: $token")
        // Auto-send refreshed token to backend
        if (SessionManager.isLoggedIn()) {
            val userId = SessionManager.currentUser?.id?.toString()
            if (userId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = FcmTokenRequest(userId, token)
                        val response = RetrofitClient.instance.updateFcmToken(request)
                        if (response.isSuccessful) {
                            Log.d("FCMService", "Refreshed FCM token sent to backend successfully.")
                        } else {
                            Log.e("FCMService", "Failed to send refreshed token. Code: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FCMService", "Exception sending refreshed FCM token", e)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Data-only message: always triggers onMessageReceived (foreground + background)
        val data = message.data
        val title = data["title"] ?: "Pesanan Baru"
        val body = data["body"] ?: "Ada pesanan masuk!"
        val transactionCode = data["transactionCode"] ?: ""

        Log.d("FCMService", "FCM Data Message Received: $title | $body | code=$transactionCode")

        // ALWAYS show notification with sound (foreground & background)
        showOrderNotification(title, body)
    }

    private fun showOrderNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Read custom sound URI from SharedPreferences (SYNCHRONOUS - works even when app is killed)
        val customSoundUri = SessionManager.getCustomSoundPathSync(this)
        Log.d("FCMService", "Custom sound URI from SharedPrefs: ${customSoundUri ?: "NULL (using default)"}")
        
        // 2. Ensure channel exists with the correct sound
        NotificationUtils.createOrderChannel(this, customSoundUri)
        
        // 3. Get the active channel ID (saved by NotificationUtils to SharedPreferences)
        val channelId = NotificationUtils.getActiveChannelId(this)
        Log.d("FCMService", "Using channel ID: $channelId")

        // 4. Build notification (on Android 8+, sound is determined by Channel, not Builder)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
