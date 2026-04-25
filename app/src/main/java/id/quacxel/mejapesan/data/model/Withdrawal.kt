package id.quacxel.mejapesan.data.model

data class WithdrawalRequest(
    val amount: Int,
    val method: String // "DANA" or "Bank Transfer"
)

data class WithdrawalResponse(
    val success: Boolean,
    val data: Withdrawal?
)

data class Withdrawal(
    val id: Int,
    val amount: Int,
    val status: String,
    val method: String,
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val storeId: Int? = null
)

data class BalanceResponse(
    val success: Boolean,
    val balance: Int, // Deprecated: Total/Available mix depending on legacy
    val availableBalance: Int = 0, // Hot/Cold Split: Cold
    val pendingSettlement: Int = 0 // Hot/Cold Split: Hot
)

data class WithdrawalHistoryResponse(
    val success: Boolean,
    val data: List<Withdrawal>
)
