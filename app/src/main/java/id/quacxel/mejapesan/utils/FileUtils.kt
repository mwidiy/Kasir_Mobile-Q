package id.quacxel.mejapesan.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            
            // 1. Get Real Filename
            var fileName = "temp_file_${System.currentTimeMillis()}"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
            
            // 2. Create Temp File with Real Name
            // Clean filename to match backend expectation just in case
            val cleanName = fileName.replace("[^a-zA-Z0-9.\\-_]".toRegex(), "_")
            val file = File(context.cacheDir, cleanName)
            
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            android.util.Log.d("FileUtils", "Created temp file: ${file.name} from $uri")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
