package com.example.kasir.utils

import android.content.Context
import android.media.MediaPlayer
import com.example.kasir.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun playNotificationSound(context: Context) {
    CoroutineScope(Dispatchers.Main).launch {
        repeat(3) {
            try {
                val mediaPlayer = MediaPlayer.create(context, R.raw.ding)
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
                // Wait for duration approx 1-2 sec or just delay slightly more than ding length
                // "ding.mp3" is usually short.
                delay(1500) 
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
