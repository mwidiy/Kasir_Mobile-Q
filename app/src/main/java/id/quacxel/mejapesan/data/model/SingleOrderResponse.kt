package id.quacxel.mejapesan.data.model

data class SingleOrderResponse(
    val success: Boolean,
    val message: String?,
    val data: OrderResponse
)
