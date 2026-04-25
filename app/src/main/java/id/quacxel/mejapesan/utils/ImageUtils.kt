package id.quacxel.mejapesan.utils

import id.quacxel.mejapesan.BuildConfig
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

        // BYPASS: Do not rewrite Cloudinary CDN URLs or Local Device URIs
        if (originalUrl.contains("cloudinary.com") || 
            originalUrl.startsWith("content://") || 
            originalUrl.startsWith("file://")) {
            return originalUrl
        }

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
     * Decodes a QR Code from a Uri securely without crashing OOM.
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            // SECURITY FIX: Calculate inSampleSize first to avoid OOM
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // Calculate ratios for ~1024px maximum bounds
            val reqWidth = 1024
            val reqHeight = 1024
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            // Decode actual bitmap with downsampling
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                 BitmapFactory.decodeStream(inputStream, null, options)
            }

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

    /**
     * Calculates optimal downsampling ratio to prevent OOM
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
