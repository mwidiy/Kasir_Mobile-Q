package com.example.kasir.data.model

data class RefundResponse(
    val success: Boolean,
    val message: String,
    val amount: Int?
)
