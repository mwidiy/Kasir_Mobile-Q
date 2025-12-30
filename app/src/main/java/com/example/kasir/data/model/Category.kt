package com.example.kasir.data.model

data class Category(
    val id: Int,
    val name: String
)

data class CategoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<Category>
)

data class SingleCategoryResponse(
    val success: Boolean,
    val message: String,
    val data: Category?
)
