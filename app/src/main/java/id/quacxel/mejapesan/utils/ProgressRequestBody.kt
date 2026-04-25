package id.quacxel.mejapesan.utils

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val onProgress: (Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentType
    }

    override fun contentLength(): Long {
        return file.length()
    }

    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val inputStream = FileInputStream(file)
        var uploaded: Long = 0
        val handler = Handler(Looper.getMainLooper())

        try {
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                // write to sink
                sink.write(buffer, 0, read)
                uploaded += read
                
                // Update progress on Main Thread
                val progress = (uploaded.toFloat() / fileLength.toFloat())
                handler.post {
                    onProgress(progress)
                }
            }
        } finally {
            inputStream.close()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}
