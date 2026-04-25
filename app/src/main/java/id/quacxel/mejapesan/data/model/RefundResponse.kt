package id.quacxel.mejapesan.data.model

data class RefundResponse(
    val success: Boolean,
    val message: String,
    val amount: Int?
)
