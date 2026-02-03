package com.example.kasir.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object QRCodeHelper {
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            if (content.isBlank()) return null
            
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun QRCodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(content) { mutableStateOf<android.graphics.Bitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(content) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val generated = QRCodeHelper.generateQrBitmap(content)
            bitmap = generated
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "QR Code: $content",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        // Placeholder or Loading state if needed, transparent for now to prevent layout jump
        androidx.compose.foundation.layout.Box(modifier = modifier)
    }
}
