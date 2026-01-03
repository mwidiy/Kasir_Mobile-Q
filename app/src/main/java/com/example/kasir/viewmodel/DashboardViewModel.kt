package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Order
import com.example.kasir.data.model.OrderStatusRequest
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.SocketHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    init {
        initializeSocket()
        fetchOrders()
    }

    private fun initializeSocket() {
        try {
            SocketHandler.setSocket()
            SocketHandler.establishConnection()
            val socket = SocketHandler.getSocket()

            socket.on("new_order") {
                fetchOrders()
            }

            socket.on("order_status_updated") {
                fetchOrders()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            try {
                // Fetch all orders regardless of status, or we can filter in the backend if API supports it
                // Calling getOrders(null) to get all
                val response = apiService.getOrders(null)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    // Assuming ApiResponse has a 'data' field. Use safe call/elvis.
                    val orderList = apiResponse?.data ?: emptyList()
                    // Sort by createdAt descending (newest first)
                    // createdAt is String, maybe ISO format?
                    // For now, reverse list or rely on backend sort. 
                    // Let's reverse to show newest if backend returns oldest first.
                    // Or parsing string date.
                    // Just updating state.
                    _orders.value = orderList.reversed() 
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                val response = apiService.updateOrderStatus(orderId, OrderStatusRequest(newStatus))
                if (response.isSuccessful) {
                    fetchOrders()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            SocketHandler.closeConnection()
            SocketHandler.getSocket().off("new_order")
            SocketHandler.getSocket().off("order_status_updated")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
