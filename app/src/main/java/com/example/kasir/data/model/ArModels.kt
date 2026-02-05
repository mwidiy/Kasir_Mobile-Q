package com.example.kasir.data.model

data class ArAsset(
    val id: Int? = null,
    val name: String,
    val url: String,
    val isDefault: Boolean = false
)

data class ArAssetResponse(
    val success: Boolean,
    val message: String?,
    val data: List<ArAsset>
)

data class SingleArAssetResponse(
    val success: Boolean,
    val message: String?,
    val data: ArAsset?
)
