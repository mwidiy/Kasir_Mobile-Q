package id.quacxel.mejapesan.utils

import io.socket.client.IO
import id.quacxel.mejapesan.BuildConfig
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    private lateinit var mSocket: Socket

    @Synchronized
    fun setSocket() {
        if (::mSocket.isInitialized) return // Prevent re-initialization
        try {
            val url = BuildConfig.API_BASE_URL.trim()
            android.util.Log.d("SocketHandler", "Initializing Socket for URL: $url")
            
            // Using the base URL from local.properties
            val options = IO.Options()
            options.transports = arrayOf("websocket", "polling") // Fallback if WebSocket fails
            options.reconnection = true
            options.reconnectionAttempts = Int.MAX_VALUE
            options.reconnectionDelay = 1000
            options.timeout = 20000 // 20 seconds
            
            mSocket = IO.socket(BuildConfig.API_BASE_URL, options)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun isInitialized(): Boolean = ::mSocket.isInitialized

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun getSocketOrNull(): Socket? {
        return if (::mSocket.isInitialized) mSocket else null
    }

    @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }
}
