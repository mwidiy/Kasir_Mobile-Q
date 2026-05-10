package id.quacxel.mejapesan.data.model

data class WhatsAppStatusResponse(
    val success: Boolean,
    val status: String, // "connected" or "disconnected"
    val isDbActive: Boolean
)
