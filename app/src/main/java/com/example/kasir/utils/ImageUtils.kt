package com.example.kasir.utils

import com.example.kasir.BuildConfig
import android.net.Uri

object ImageUtils {
    /**
     * Transforms a potentially stale absolute URL (e.g. from old IP) into a correct URL
     * using the current BuildConfig.API_BASE_URL.
     * 
     * @param originalUrl The raw URL from database (e.g. "http://192.168.1.5:3000/uploads/img.jpg")
     * @return The corrected URL (e.g. "http://192.168.1.10:3000/uploads/img.jpg")
     */
    fun getDynamicImageUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrBlank()) return null

        // If it starts with http, we assume it's a full URL that might have wrong IP
        if (originalUrl.startsWith("http")) {
            return try {
                val uri = Uri.parse(originalUrl)
                // Extract "uploads/filename.jpg"
                val path = uri.path?.trimStart('/') ?: return originalUrl
                
                // Remove "uploads/" from path if it exists to avoid duplication if we re-add it
                // Actually, typically the path is "/uploads/filename.jpg"
                // So uri.path is "/uploads/filename.jpg"
                
                // Current Config Base URL ends with "/" (as ensured in build.gradle)
                // e.g. "http://192.168.1.10:3000/"
                
                // Combine
                // If path starts with slash, remove it to concat cleanly
                val cleanPath = if (path.startsWith("/")) path.substring(1) else path
                
                // Reconstruct using current Base URL
                "${BuildConfig.API_BASE_URL}$cleanPath"
            } catch (e: Exception) {
                originalUrl // Fallback
            }
        }
        
        // If it's just a filename (e.g. "171000.jpg"), prepend Base URL + uploads/
        // Assuming all images are in "uploads/"
        return "${BuildConfig.API_BASE_URL}uploads/$originalUrl"
    }
}
