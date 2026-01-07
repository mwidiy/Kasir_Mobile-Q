package com.example.kasir.data.model

data class Store(
    val id: Int,
    val name: String,
    val logo: String?,
    val qrisImage: String?,
    val ownerId: Int,
    val updatedAt: String?
)

data class StoreResponse(
    val success: Boolean,
    val data: Store?
)
