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
    val isDeliveryActive: Boolean? = true,
    val isWaOrderNotificationActive: Boolean? = false,
    val isAiEnabled: Boolean? = false,
    val isAutoReplyEnabled: Boolean? = false
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
    val isDeliveryActive: Boolean? = null,
    val isWaOrderNotificationActive: Boolean? = null,
    val isAiEnabled: Boolean? = null,
    val isAutoReplyEnabled: Boolean? = null
)

data class PromotionStats(
    val loyalCount: Int,
    val churningCount: Int
)

data class PromotionStatsResponse(
    val success: Boolean,
    val data: PromotionStats?
)

data class PromotionResponse(
    val success: Boolean,
    val message: String,
    val targetCount: Int? = 0
)
