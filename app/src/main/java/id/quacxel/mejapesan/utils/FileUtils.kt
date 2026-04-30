package id.quacxel.mejapesan.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment

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

    fun saveCustomAudio(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            
            // 1. Validation: Size (Max 1MB)
            val size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0
            if (size > 1 * 1024 * 1024) {
                throw Exception("Ukuran file terlalu besar (Maks 1MB)")
            }

            // 2. Validation: Format
            val type = contentResolver.getType(uri) ?: "audio/mpeg"
            val allowedTypes = listOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/x-m4a", "audio/mp3", "audio/aac")
            if (type !in allowedTypes && !type.startsWith("audio/")) {
                throw Exception("Format file tidak didukung. Gunakan MP3, WAV, atau OGG.")
            }

            // 3. Insert to MediaStore
            val fileName = "MejaPesan_Order_Sound_${System.currentTimeMillis()}.mp3"
            
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, type)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
                put(MediaStore.Audio.Media.IS_ALARM, 0)
                put(MediaStore.Audio.Media.IS_RINGTONE, 0)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_NOTIFICATIONS + "/MejaPesan")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = contentResolver.insert(collection, values) 
                ?: throw Exception("Gagal mendaftarkan file ke sistem Android (MediaStore)")

            contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, values, null, null)
            }

            // Return the Content URI string
            itemUri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
