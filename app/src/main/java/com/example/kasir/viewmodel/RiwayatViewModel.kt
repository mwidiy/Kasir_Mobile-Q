package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RiwayatViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance

    private val _allOrders = MutableStateFlow<List<OrderResponse>>(emptyList())
    private val _displayedOrders = MutableStateFlow<List<OrderResponse>>(emptyList())
    val displayedOrders: StateFlow<List<OrderResponse>> = _displayedOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Analysis State
    data class AnalysisData(
        val totalIncome: String = "Rp 0",
        val transactionCount: String = "0",
        val avgIncome: String = "Rp 0"
    )
    private val _analysis = MutableStateFlow(AnalysisData())
    val analysis: StateFlow<AnalysisData> = _analysis.asStateFlow()

    // Filter State
    var currentFilterTab = 0 // 0: Hari Ini, 1: Minggu Ini, 2: Bulan Ini
    var currentQuery = ""
    var statusFilter = "All" // All, Completed, Cancelled
    var typeFilter = "All" // All, dinein, takeaway

    init {
        initSocket()
        fetchHistory()
    }

    private fun initSocket() {
        try {
            com.example.kasir.utils.SocketHandler.setSocket()
            com.example.kasir.utils.SocketHandler.establishConnection()
            val socket = com.example.kasir.utils.SocketHandler.getSocket()

            // Listen for any order update
            socket.on("order_status_updated") { args ->
                // Refresh if the updated order is Completed or Cancelled
                // For simplicity, we just refetch or checking the status arg could be optimized
                fetchHistory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getOrders("Completed,Cancelled") 
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val orders = response.body()?.data ?: emptyList()
                    _allOrders.value = orders
                    // Re-apply current filters
                    applyFilters()
                } else {
                    _error.value = "Gagal memuat riwayat: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTabFilter(tabIndex: Int) {
        currentFilterTab = tabIndex
        applyFilters()
    }

    fun search(query: String) {
        currentQuery = query
        applyFilters()
    }

    fun setAdvancedFilter(status: String, type: String) {
        statusFilter = status
        typeFilter = type
        applyFilters()
    }

    private fun applyFilters() {
        val now = Date()
        var result = _allOrders.value

        // 1. Date Tab Filter (Apply First)
        result = when (currentFilterTab) {
            0 -> result.filter { isSameDay(it.createdAt, now) }
            1 -> result.filter { isSameMonth(it.createdAt, now) }
            2 -> result // Semua (All History)
            else -> result
        }

        // 2. Search Filter (Apply Second - AND Logic)
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase()
            result = result.filter { order ->
                order.transactionCode.lowercase().contains(q) ||
                order.customerName.lowercase().contains(q) ||
                order.items.any { it.product.name.lowercase().contains(q) }
            }
        }
        
        // 3. Status Filter

        // 3. Status Filter
        if (statusFilter != "All") {
            result = result.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        // 4. Type Filter
        if (typeFilter != "All") {
            // Backend types: "dinein", "takeaway"
            result = result.filter { it.orderType.equals(typeFilter, ignoreCase = true) }
        }
        
        _displayedOrders.value = result
        calculateAnalysis(result)
    }

    fun applyFilter(index: Int) {
        setTabFilter(index)
    }

    private fun calculateAnalysis(orders: List<OrderResponse>) {
        val total = orders.sumOf { it.totalAmount.toLong() }
        val count = orders.size
        val avg = if (count > 0) total / count else 0

        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        _analysis.value = AnalysisData(
            totalIncome = fmt.format(total).replace("Rp", "Rp ").replace(",00", ""),
            transactionCount = count.toString(),
            avgIncome = fmt.format(avg).replace("Rp", "Rp ").replace(",00", "")
        )
    }

    private fun isSameDay(dateStr: String, now: Date): Boolean {
        return try {
             val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
             val date = parser.parse(dateStr) ?: return false
             val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
             fmt.format(date) == fmt.format(now)
        } catch (e: Exception) { false }
    }

    private fun isThisWeek(dateStr: String, now: Date): Boolean {
        return try {
             val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
             val date = parser.parse(dateStr) ?: return false
             val diff = now.time - date.time
             val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
             days in 0..6
        } catch (e: Exception) { false }
    }

    private fun isSameMonth(dateStr: String, now: Date): Boolean {
        return try {
             val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
             val date = parser.parse(dateStr) ?: return false
             val fmt = SimpleDateFormat("yyyyMM", Locale.getDefault())
             fmt.format(date) == fmt.format(now)
        } catch (e: Exception) { false }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Optional: socket.off if needed, but SocketHandler usually manages singleton
    }
}
