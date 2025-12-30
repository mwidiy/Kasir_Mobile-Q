package com.example.kasir.data.model

data class ProductResponse(
    val success: Boolean,
    val message: String,
    val data: List<Product>
)

data class Product(
    val id: Int,
    val name: String,
    val category: String?, // Backend might still send this or we map it
    val categoryId: Int?, // New field
    val price: Int,
    val image: String?,
    val description: String?,
    val isActive: Boolean
)

data class SingleProductResponse(
    val success: Boolean,
    val message: String,
    val data: Product?
)

