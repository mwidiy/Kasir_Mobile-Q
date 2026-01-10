package com.example.kasir.data.model

data class SingleOrderResponse(
    val success: Boolean,
    val message: String?,
    val data: OrderResponse
)
