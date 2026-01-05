package com.example.kasir.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderStatusRequest
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.SocketHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance

    // State Management
    private val _orders = MutableStateFlow<List<OrderResponse>>(emptyList())
    val orders: StateFlow<List<OrderResponse>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initSocket()
        fetchOrders()
    }

    private fun initSocket() {
        try {
            SocketHandler.setSocket()
            SocketHandler.establishConnection()
            val socket = SocketHandler.getSocket()

            // Listen for "new_order" event from backend
            socket.on("new_order") {
                Log.d("DashboardViewModel", "Socket event received: new_order")
                fetchOrders()
            }
            
            // Listen for update status event if consistent with backend
            socket.on("order_status_updated") {
                Log.d("DashboardViewModel", "Socket event received: order_status_updated")
                fetchOrders()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = "Socket Error: ${e.message}"
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch all orders regardless of status
                val response = apiService.getOrders(null)
                if (response.isSuccessful) {
                    val orderListResponse = response.body()
                    // DEBUGGING: Log raw response data
                    android.util.Log.d("DashboardViewModel", "Raw Data: $orderListResponse")
                    if (orderListResponse != null && orderListResponse.success) {
                        _orders.value = orderListResponse.data
                        android.util.Log.d("DashboardViewModel", "Orders assigned: ${_orders.value.size}")
                        // Check first order detail
                        if (_orders.value.isNotEmpty()) {
                            val first = _orders.value[0]
                            android.util.Log.d("DashboardViewModel", "Order[0] Table: ${first.table}, TableID: ${first.table?.id}, Loc: ${first.table?.location}")
                        }
                    } else {
                        _error.value = "Gagal memuat data: ${orderListResponse?.message}"
                    }
                } else {
                    val msg = "Failed to fetch orders: ${response.message()}"
                    _error.value = msg
                    Log.e("DashboardViewModel", msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = "Error: ${e.localizedMessage}"
                _error.value = msg
                Log.e("DashboardViewModel", "Gagal fetch data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.updateOrderStatus(orderId, OrderStatusRequest(newStatus))
                if (response.isSuccessful) {
                    fetchOrders() // Refresh list on success
                } else {
                     _error.value = "Failed to update status: ${response.message()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Error update: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val socket = SocketHandler.getSocket()
            if (socket.connected()) {
                 socket.off("new_order")
                 socket.off("order_status_updated")
                 SocketHandler.closeConnection()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
