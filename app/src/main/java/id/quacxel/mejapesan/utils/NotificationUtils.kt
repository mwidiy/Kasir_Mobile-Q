package id.quacxel.mejapesan.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import id.quacxel.mejapesan.R

object NotificationUtils {
    
    fun createOrderChannel(context: Context, customSoundUri: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Determine channel ID and sound URI
            val channelId: String
            val soundUri: Uri
            
            if (!customSoundUri.isNullOrEmpty()) {
                // Custom sound: use dynamic channel ID based on URI hash
                channelId = "pesanan_baru_custom_${customSoundUri.hashCode()}"
                soundUri = Uri.parse(customSoundUri)
            } else {
                // Default sound: use a FRESH channel ID (not the old locked "pesanan_baru")
                channelId = "pesanan_baru_default_v2"
                soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.sound_pesanan}")
            }

            // AGGRESSIVE CLEANUP: Delete ALL old channels that we don't need anymore
            notificationManager.notificationChannels.forEach { oldChannel ->
                val oldId = oldChannel.id
                if (oldId.startsWith("pesanan_baru") && oldId != channelId) {
                    notificationManager.deleteNotificationChannel(oldId)
                }
            }

            // Create the channel with the correct sound
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(channelId, "Pesanan Baru", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi untuk pesanan masuk"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            notificationManager.createNotificationChannel(channel)

            // Save active channel ID to SharedPreferences for FCM service
            val prefs = context.getSharedPreferences("mejapesan_sound", Context.MODE_PRIVATE)
            prefs.edit().putString("active_channel_id", channelId).apply()
        }
    }

    // Get the active channel ID synchronously (for FCM service)
    fun getActiveChannelId(context: Context): String {
        val prefs = context.getSharedPreferences("mejapesan_sound", Context.MODE_PRIVATE)
        return prefs.getString("active_channel_id", "pesanan_baru_default_v2") ?: "pesanan_baru_default_v2"
    }

    fun playOrderSound(context: Context, customSoundUri: String?) {
        val soundUri = if (!customSoundUri.isNullOrEmpty()) {
            Uri.parse(customSoundUri)
        } else {
            Uri.parse("android.resource://${context.packageName}/${R.raw.sound_pesanan}")
        }
        
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, soundUri)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
