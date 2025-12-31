package com.example.kasir.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageSaver {
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        return try {
            val filename = "${title}_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KasirApp")
                }
                
                val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = context.contentResolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "KasirApp")
                if (!appDir.exists()) appDir.mkdirs()
                
                val imageFile = File(appDir, filename)
                fos = FileOutputStream(imageFile)
            }
            
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
