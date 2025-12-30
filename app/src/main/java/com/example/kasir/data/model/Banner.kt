package com.example.kasir.data.model

data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String?,
    val highlightText: String?,
    val image: String,
    val isActive: Boolean
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T
)
