package id.quacxel.mejapesan.data.model

data class ShippingZone(
    val id: Int,
    val storeId: Int,
    val name: String,
    val fee: Int,
    val isActive: Boolean
)

data class ShippingZoneResponse(
    val success: Boolean,
    val data: List<ShippingZone>,
    val error: String? = null
)

data class SingleShippingZoneResponse(
    val success: Boolean,
    val data: ShippingZone?,
    val error: String? = null
)

data class ShippingZoneRequest(
    val name: String,
    val fee: Int,
    val isActive: Boolean = true
)
