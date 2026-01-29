package com.example.kasir.utils

import com.example.kasir.BuildConfig
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

object ImageUtils {
    /**
     * Transforms a potentially stale absolute URL (e.g. from old IP) into a correct URL
     * using the current BuildConfig.API_BASE_URL.
     */
    fun getDynamicImageUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrBlank()) return null

        if (originalUrl.startsWith("http")) {
            return try {
                val uri = Uri.parse(originalUrl)
                val path = uri.path?.trimStart('/') ?: return originalUrl
                val cleanPath = if (path.startsWith("/")) path.substring(1) else path
                "${BuildConfig.API_BASE_URL}$cleanPath"
            } catch (e: Exception) {
                originalUrl 
            }
        }
        
        return "${BuildConfig.API_BASE_URL}uploads/$originalUrl"
    }

    /**
     * Decodes a QR Code from a Uri.
     * Returns the decoded text string, or null if no QR code found or error.
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
