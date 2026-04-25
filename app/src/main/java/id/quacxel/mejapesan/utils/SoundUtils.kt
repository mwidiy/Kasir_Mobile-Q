package id.quacxel.mejapesan.utils

import android.content.Context
import android.media.MediaPlayer
import id.quacxel.mejapesan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun playOrderSound(context: Context) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            // Updated to use the new Sound_Pesanan.mp3
            val soundId = context.resources.getIdentifier("sound_pesanan", "raw", context.packageName)
            if (soundId != 0) {
                val mediaPlayer = MediaPlayer.create(context, soundId)
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            } else {
                // Fallback if sound_pesanan not found (start with ding)
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
