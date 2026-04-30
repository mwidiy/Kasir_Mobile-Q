package id.quacxel.mejapesan.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    /**
     * Converts a UTC ISO date string to a formatted WIB string
     * Input Example: 2024-04-28T10:00:00.000Z
     * Output Example: 28 Apr 2024, 17:00 WIB
     */
    fun formatToWIB(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "-"
        
        return try {
            // 1. Parse from UTC
            // Format can vary, handling standard ISO
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Clean up if there's a dot for milliseconds
            val cleanedDate = isoDate.split(".")[0].replace("Z", "")
            val date = inputFormat.parse(cleanedDate) ?: return isoDate
            
            // 2. Format to WIB (GMT+7)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("GMT+7")
            }
            
            outputFormat.format(date) + " WIB"
        } catch (e: Exception) {
            isoDate // Fallback to original
        }
    }

    /**
     * Simple time only formatter for smaller spaces
     * Output: 17:00
     */
    fun formatTimeOnly(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "-"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanedDate = isoDate.split(".")[0].replace("Z", "")
            val date = inputFormat.parse(cleanedDate) ?: return isoDate
            
            val outputFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT+7")
            }
            outputFormat.format(date)
        } catch (e: Exception) {
            "-"
        }
    }
}
