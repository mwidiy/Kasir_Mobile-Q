package id.quacxel.mejapesan.utils

import android.content.Context
import android.media.MediaPlayer
import id.quacxel.mejapesan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

fun playOrderSound(context: Context) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            // 1. Check for custom sound from SessionManager (now a content:// URI)
            val customPath = SessionManager.getCustomSoundPath(context).first()
            if (!customPath.isNullOrEmpty()) {
                try {
                    val uri = android.net.Uri.parse(customPath)
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, uri)
                        prepare()
                        setOnCompletionListener { it.release() }
                    }
                    mediaPlayer.start()
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fall through to default sound
                }
            }

            // 2. Fallback to default sound_pesanan
            val soundId = context.resources.getIdentifier("sound_pesanan", "raw", context.packageName)
            if (soundId != 0) {
                val mediaPlayer = MediaPlayer.create(context, soundId)
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            } else {
                playCancellationSound(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun playCancellationSound(context: Context) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            // Use existing Ding.mp3 for cancellation
            val mediaPlayer = MediaPlayer.create(context, R.raw.ding)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
