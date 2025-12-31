package com.example.kasir.data.model

data class TableRequest(
    val name: String,
    val locationId: Int,
    val qrCode: String,
    val isActive: Boolean = true
)
