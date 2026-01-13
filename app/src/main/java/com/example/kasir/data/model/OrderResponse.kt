package com.example.kasir.data.model

import com.google.gson.annotations.SerializedName

// Wrapper Class untuk respon API yang membungkus list
data class OrderListResponse(
    val success: Boolean,
    val message: String,
    val data: List<OrderResponse>
)

data class OrderResponse(
    val id: Int,
    val transactionCode: String,
    val customerName: String,
    @SerializedName("orderType") val orderType: String, // "dinein", "takeaway"
    val paymentStatus: String, // "Paid", "Unpaid"
    val status: String, // "Pending", "Processing", "Completed"
    val totalAmount: Int,
    val table: OrderTableResponse?, // Nullable
    val items: List<OrderItemResponse>,
    val createdAt: String,
    val note: String?, // Dulu globalNote
    val deliveryAddress: String?, // New field for address
    val cancellationStatus: String? = null,
    val cancellationReason: String? = null,
    val refundStatus: String? = null,
    val queueNumber: Int? = null,
    val paymentMethod: String? = null
)

data class OrderTableResponse(
    val id: Int,
    val name: String,
    val qrCode: String?, // Added QR Code field
    val location: OrderLocationResponse? // Nullable agar aman jika backend belum kirim
)

data class OrderLocationResponse(
    val name: String
)

data class OrderItemResponse(
    val id: Int,
    val quantity: Int,
    val note: String?,
    @SerializedName("priceSnapshot") val priceSnapshot: Int?, // Added to capture historical price
    val product: OrderProductResponse
)

data class OrderProductResponse(
    val name: String,
    val price: Int,
    val image: String?
)
