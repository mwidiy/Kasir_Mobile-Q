package id.quacxel.mejapesan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.OrderResponse
import id.quacxel.mejapesan.data.model.OrderStatusRequest
import id.quacxel.mejapesan.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance

    private val _scannedOrder = MutableStateFlow<OrderResponse?>(null)
    val scannedOrder: StateFlow<OrderResponse?> = _scannedOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _paymentSuccess = MutableStateFlow(false)
    val paymentSuccess: StateFlow<Boolean> = _paymentSuccess.asStateFlow()

    // Refund State
    private val _refundOrder = MutableStateFlow<OrderResponse?>(null)
    val refundOrder: StateFlow<OrderResponse?> = _refundOrder.asStateFlow()

    private val _refundSuccess = MutableStateFlow(false)
    val refundSuccess: StateFlow<Boolean> = _refundSuccess.asStateFlow()

    fun fetchOrderByCode(rawPayload: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _scannedOrder.value = null 
            try {
                var codeToFetch = rawPayload

                // --- SECURITY FIX: CROSS-STORE QR VALIDATION ---
                // Format: STORE:12|TRX-2024ABCD...
                if (rawPayload.startsWith("STORE:")) {
                    val parts = rawPayload.substringAfter("STORE:").split("|", limit = 2)
                    if (parts.size == 2) {
                        val qrStoreId = parts[0].toIntOrNull()
                        val myStoreId = id.quacxel.mejapesan.utils.SessionManager.currentUser?.store?.id
                        
                        if (qrStoreId != null && myStoreId != null && qrStoreId != myStoreId) {
                            _error.value = "⛔ Barcode Bukan Milik Toko Anda!"
                            _isLoading.value = false
                            return@launch
                        }
                        codeToFetch = parts[1]
                    }
                }

                val response = apiService.getOrderByCode(codeToFetch)
                if (response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()?.data
                    if (order != null) {
                        // Check if this is a Refund Case
                        // 1. Handle Valid Refund Request (Prioritas: User minta refund)
                        if (order.status == "Cancelled" && order.paymentStatus == "Paid" && order.refundStatus != "Refunded") {
                            _refundOrder.value = order
                        } 
                        // 2. Reject Already Refunded (Exploit Fix)
                        else if (order.refundStatus == "Refunded") {
                            _error.value = "⛔ Pesanan ini SUDAH direfund!"
                        }
                        // 3. Reject Cancelled & Unpaid (Exploit Fix)
                        else if (order.status == "Cancelled") {
                             _error.value = "⛔ Pesanan ini sudah DIBATALKAN!"
                        }
                        // 4. Normal Flow (Payment Confirmation or other processing)
                        else {
                            _scannedOrder.value = order
                        }
                    }
                } else {
                    _error.value = "Pesanan tidak ditemukan atau error: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Gagal memuat pesanan: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmPayment(orderId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fix: Only update paymentStatus to "Paid", do NOT change status to "Processing"
                // The status should remain as it was (e.g. Pending) until Kitchen manually starts processing
                val response = apiService.updateOrderStatus(orderId, OrderStatusRequest(null, "Paid"))
                if (response.isSuccessful) {
                    _paymentSuccess.value = true
                } else {
                    _error.value = "Gagal konfirmasi pembayaran"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processRefund(transactionCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.verifyRefund(mapOf("transactionCode" to transactionCode))
                if (response.isSuccessful && response.body()?.success == true) {
                    _refundSuccess.value = true
                } else {
                    _error.value = response.body()?.message ?: "Gagal memproses refund"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetState() {
        _scannedOrder.value = null
        _refundOrder.value = null
        _paymentSuccess.value = false
        _refundSuccess.value = false
        _error.value = null
    }
}
