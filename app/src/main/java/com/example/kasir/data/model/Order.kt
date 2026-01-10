package com.example.kasir.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    val id: Int,
    val transactionCode: String?,
    val customerName: String?,
    @SerializedName("orderType") val orderType: String?, // "dinein", "takeaway", "delivery"
    val totalAmount: Double,
    val status: String,
    val paymentStatus: String,
    val globalNote: String?,
    val createdAt: String?,
    val table: OrderTable?,        // Nested Object Level 1
    val items: List<OrderItem>?
)

data class OrderTable(
    val id: Int,
    val name: String,         // Contoh: "12"
    val location: OrderLocation?   // Nested Object Level 2
)

data class OrderLocation(
    val id: Int,
    val name: String          // Contoh: "UKMI"
)

data class OrderItem(
    val id: Int,
    val quantity: Int,
    val note: String?,
    val priceSnapshot: Double,
    val product: Product
)

data class OrderStatusRequest(
    val status: String?,
    val paymentStatus: String? = null
)
