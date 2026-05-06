package id.quacxel.mejapesan.data.model

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
    val ewalletName: String?,
    val whatsappNumber: String?, // NEW
    val isKasirQrVerificationEnabled: Boolean? = false, // NEW
    val cashPaymentMode: String? = "post", // NEW: "post" or "pre"
    val isCashActive: Boolean? = true,
    val isDineInActive: Boolean? = true,
    val isTakeawayActive: Boolean? = true,
    val isDeliveryActive: Boolean? = true
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
    val ewalletName: String? = null,
    val whatsappNumber: String? = null,
    val isKasirQrVerificationEnabled: Boolean? = null,
    val cashPaymentMode: String? = null,
    val isCashActive: Boolean? = null,
    val isDineInActive: Boolean? = null,
    val isTakeawayActive: Boolean? = null,
    val isDeliveryActive: Boolean? = null
)
