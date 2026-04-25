package id.quacxel.mejapesan.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import id.quacxel.mejapesan.MainActivity
import id.quacxel.mejapesan.R
import id.quacxel.mejapesan.utils.SessionManager
import id.quacxel.mejapesan.data.network.FcmTokenRequest
import id.quacxel.mejapesan.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@androidx.compose.animation.ExperimentalAnimationApi
class FCMNotificationService : FirebaseMessagingService() {

    private val CHANNEL_ID = "pesanan_baru"

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

        // Custom Sound URI (channel already created in MainActivity with this sound)
        val soundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sound_pesanan)

        // Build notification using the channel created in MainActivity
        // Channel 'pesanan_baru' already has custom sound configured
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

