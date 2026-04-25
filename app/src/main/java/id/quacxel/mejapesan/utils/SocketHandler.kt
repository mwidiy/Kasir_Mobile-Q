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

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
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
