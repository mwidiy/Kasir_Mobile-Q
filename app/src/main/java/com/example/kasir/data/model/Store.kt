package com.example.kasir.data.model

data class Store(
    val id: Int,
    val name: String,
    val logo: String?,
    val qrisImage: String?,
    val ownerId: Int,
    val updatedAt: String?,
    val isOpen: Boolean = true,
    val bankName: String?,
    val bankNumber: String?,
    val bankHolder: String?,
    val ewalletType: String?,
    val ewalletNumber: String?,
    val ewalletName: String?
)

data class StoreResponse(
    val success: Boolean,
    val data: Store?
)

data class StoreUpdateRequest(
    val name: String? = null,
    val isOpen: Boolean? = null,
    val bankName: String? = null,
    val bankNumber: String? = null,
    val bankHolder: String? = null,
    val ewalletType: String? = null,
    val ewalletNumber: String? = null,
    val ewalletName: String? = null
)
