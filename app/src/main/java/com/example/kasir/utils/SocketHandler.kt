package com.example.kasir.utils

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    private lateinit var mSocket: Socket

    @Synchronized
    fun setSocket() {
        try {
            // Using the base URL as requested
            mSocket = IO.socket("http://192.168.1.6:3000/")
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
