package com.example.kasir.data.model

data class ProductResponse(
    val success: Boolean,
    val message: String,
    val data: List<Product>
)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val image: String?,
    val description: String?,
    val isActive: Boolean
)
