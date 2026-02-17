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

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.instance

    // State Management
    private val _orders = MutableStateFlow<List<OrderResponse>>(emptyList())
    val orders: StateFlow<List<OrderResponse>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Global UI State (Always On Display)
    // Global UI State (Always On Display)
    private val _isAlwaysOn = MutableStateFlow(false)
    val isAlwaysOn: StateFlow<Boolean> = _isAlwaysOn.asStateFlow()

    // Global Sound State
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    init {
        initSocket()
        fetchOrders()
        // Observe Session Persistence
        viewModelScope.launch {
            com.example.kasir.utils.SessionManager.getAlwaysOn(getApplication()).collect {
                _isAlwaysOn.value = it
            }
        }
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
            // Silent Refresh: Only show full loader if list is empty
            if (_orders.value.isEmpty()) {
                _isLoading.value = true
            }
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
        // Optimistic Update
        val currentList = _orders.value
        val oldOrderIndex = currentList.indexOfFirst { it.id == orderId }
        
        if (oldOrderIndex != -1) {
            val oldOrder = currentList[oldOrderIndex]
            val updatedOrder = oldOrder.copy(status = newStatus)
            val newList = currentList.toMutableList()
            newList[oldOrderIndex] = updatedOrder
            _orders.value = newList
        }

        viewModelScope.launch {
            // No blocking loading
            try {
                val response = apiService.updateOrderStatus(orderId, OrderStatusRequest(newStatus))
                if (response.isSuccessful) {
                    fetchOrders() // Sync with server ensure consistency using Silent Refresh
                } else {
                    // Revert
                    _orders.value = currentList
                    _error.value = "Failed to update status: ${response.message()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert
                _orders.value = currentList
                _error.value = "Error update: ${e.localizedMessage}"
            }
        }
    }

    fun approveCancellation(orderId: Int) {
         // Optimistic Update: Remove from list or set status to Cancelled depending on filter?
         // Usually approve cancel means it becomes "Cancelled" (History)
         val currentList = _orders.value
         val oldOrderIndex = currentList.indexOfFirst { it.id == orderId }

         if (oldOrderIndex != -1) {
             val oldOrder = currentList[oldOrderIndex]
             val updatedOrder = oldOrder.copy(status = "Cancelled")
             val newList = currentList.toMutableList()
             newList[oldOrderIndex] = updatedOrder
             _orders.value = newList
         }

        viewModelScope.launch {
             // No blocking loading
            try {
                val response = apiService.approveCancel(orderId)
                if (response.isSuccessful) {
                    fetchOrders()
                } else {
                    // Revert
                    _orders.value = currentList
                    _error.value = "Gagal menyetujui pembatalan"
                }
            } catch (e: Exception) {
                 // Revert
                _orders.value = currentList
                _error.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun rejectCancellation(orderId: Int, reason: String? = null) {
        // Optimistic Update: Revert to previous status (e.g. "Pending")? 
        // Or remains "Pending"/"Processing"? Usually rejection means it goes back to active.
        // Assuming "Pending" or keeping it as is but removing formatted "Cancellation Requested" flag if exists.
        // Since we don't track "Requested" state explicitly in OrderResponse (it might be in status or separate flag),
        // Simplest optimistic is to assume it goes back to "Pending" or stay same but we refresh.
        // If we strictly follow "Anti-Blink", we should probably guess the next state.
        // Let's assume rejection keeps it "Pending" or "Processing".
        // WITHOUT specific state logic, Silent Refresh via fetchOrders is safest for complexity.
        // BUT user wants Anti-Kedip.
        // Let's rely on Silent Refresh for this one since logic is complex (Reject -> Back to what?),
        // OR simply don't show loading spinner.

        viewModelScope.launch {
            // No blocking loading, just silent refresh
            try {
                // Construct body
                val body = if (reason != null) mapOf("reason" to reason) else emptyMap()
                
                val response = apiService.rejectCancel(orderId, body)
                if (response.isSuccessful) {
                    fetchOrders()
                } else {
                    _error.value = "Gagal menolak pembatalan"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun toggleAlwaysOn(enabled: Boolean) {
        _isAlwaysOn.value = enabled
        viewModelScope.launch {
            com.example.kasir.utils.SessionManager.setAlwaysOn(getApplication(), enabled)
        }
    }

    fun toggleSound(enabled: Boolean) {
        _isSoundEnabled.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val socket = SocketHandler.getSocket()
            if (socket.connected()) {
                 socket.off("new_order")
                 socket.off("order_status_updated")
                 // SocketHandler.closeConnection() // BUG FIX: Don't kill connection! Service might use it (if shared) but now Service is independent.
                 // However, keeping connection alive when app is killed is not ViewModel's job.
                 // Service handles background. ViewModel just detaches listeners.
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
