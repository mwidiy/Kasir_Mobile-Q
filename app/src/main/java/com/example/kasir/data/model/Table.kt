package com.example.kasir.data.model

data class Table(
    val id: Int,
    val name: String,
    val location: LocationData?,
    val qrCode: String?,
    val isActive: Boolean = true
)

// Keeping these for compatibility if needed, though ApiService might return List<Table> directly now.
data class TableResponse(
    val success: Boolean,
    val message: String,
    val data: List<Table>
)

data class SingleTableResponse(
    val success: Boolean,
    val message: String,
    val data: Table?
)
