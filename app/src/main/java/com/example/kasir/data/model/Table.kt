package com.example.kasir.data.model

data class Table(
    val id: Int,
    val name: String,
    val locationId: Int,
    val location: Location?,
    val isActive: Boolean
)

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
