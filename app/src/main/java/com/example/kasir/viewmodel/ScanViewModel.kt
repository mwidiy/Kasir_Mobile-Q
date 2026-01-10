package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderStatusRequest
import com.example.kasir.data.network.RetrofitClient
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

    fun fetchOrderByCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _scannedOrder.value = null 
            try {
                val response = apiService.getOrderByCode(code)
                if (response.isSuccessful && response.body()?.success == true) {
                    _scannedOrder.value = response.body()?.data
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
                // Update to 'Processing' or 'Paid' explicitly if backend supports it.
                // Assuming 'Processing' as per previous logic.
                // Wait, orderController updateOrderStatus logic handles "Paid" status string carefully.
                // "Processing" is just a status. To mark paid, maybe send "Paid" status?
                // But typically Kitchen workflow: Pending -> Processing -> Completed.
                // Payment is separate.
                // User said "konfirmasi aja".
                // I will send "Processing" to move it forward.
                val response = apiService.updateOrderStatus(orderId, OrderStatusRequest("Processing", "Paid"))
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

    fun resetState() {
        _scannedOrder.value = null
        _paymentSuccess.value = false
        _error.value = null
    }
}
