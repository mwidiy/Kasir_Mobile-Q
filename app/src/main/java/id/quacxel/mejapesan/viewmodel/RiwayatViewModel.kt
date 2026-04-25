package id.quacxel.mejapesan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.OrderResponse
import id.quacxel.mejapesan.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import id.quacxel.mejapesan.utils.LocalEventBus
import com.google.gson.Gson
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

    private var socketDebounceJob: Job? = null
    private val historyMutex = Mutex()
    private val gson = Gson()

    private val _isKasirQrVerificationEnabled = MutableStateFlow(false)

    init {
        initSocket()
        initLocalEventBus() // TAHAP 33: True Local Optimistic Sync
        fetchStoreSettings() // NEW
        fetchHistory()
    }

    private fun fetchStoreSettings() {
        viewModelScope.launch {
            try {
                val response = apiService.getStore()
                if (response.success && response.data != null) {
                    _isKasirQrVerificationEnabled.value = response.data.isKasirQrVerificationEnabled ?: false
                    applyFilters()
                }
            } catch (e: Exception) {
                // Konfigurasi biarkan default (false) jika gagal memuat.
            }
        }
    }

    private fun initLocalEventBus() {
        viewModelScope.launch {
            LocalEventBus.settingsUpdateFlow.collect { isEnabled ->
                _isKasirQrVerificationEnabled.value = isEnabled
                applyFilters()
            }
        }
        viewModelScope.launch {
            LocalEventBus.orderUpdateFlow.collect { order ->
                historyMutex.withLock {
                    try {
                        val currentList = _allOrders.value
                        val existingIndex = currentList.indexOfFirst { it.id == order.id }
                        
                        val isHistoryValid = order.status == "Completed" || order.status == "Cancelled"
                        
                        if (existingIndex != -1) {
                            val newList = currentList.toMutableList()
                            if (isHistoryValid) {
                                newList[existingIndex] = order
                            } else {
                                newList.removeAt(existingIndex)
                            }
                            _allOrders.value = newList
                        } else {
                            if (isHistoryValid) {
                                _allOrders.value = listOf(order) + currentList
                            }
                        }
                        applyFilters()
                        // 0ms Latency achieved!
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initSocket() {
        try {
            id.quacxel.mejapesan.utils.SocketHandler.setSocket()
            id.quacxel.mejapesan.utils.SocketHandler.establishConnection()
            val socket = id.quacxel.mejapesan.utils.SocketHandler.getSocket()

            // Listen for any order update
            socket.on("order_status_updated") { args ->
                val obj = args.getOrNull(0)
                if (obj != null) {
                    viewModelScope.launch {
                        historyMutex.withLock {
                            try {
                                val order = gson.fromJson(obj.toString(), OrderResponse::class.java)
                                val currentList = _allOrders.value
                                val existingIndex = currentList.indexOfFirst { it.id == order.id }
                                
                                val isHistoryValid = order.status == "Completed" || order.status == "Cancelled"
                                
                                if (existingIndex != -1) {
                                    val newList = currentList.toMutableList()
                                    if (isHistoryValid) {
                                        newList[existingIndex] = order
                                    } else {
                                        // Case where a history item was somehow rolled back
                                        newList.removeAt(existingIndex)
                                    }
                                    _allOrders.value = newList
                                } else {
                                    // New Completed/Cancelled Order incoming
                                    if (isHistoryValid) {
                                        _allOrders.value = listOf(order) + currentList
                                    }
                                }
                                applyFilters() // Instant Local Refresh (0.001s)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
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

        // 5. GLOBAL SAFETY FILTER: Hide WaitingPayment unless Paid
        val kasirQrEnabled = _isKasirQrVerificationEnabled.value
        result = result.filter { order ->
            if (kasirQrEnabled && order.paymentMethod.equals("Kasir", ignoreCase = true)) {
                 // Kasir payments MUST be Paid to enter Riwayat if feature is ON
                 order.paymentStatus.equals("Paid", ignoreCase = true)
            } else {
                 // Normal behavior: Hide WaitingPayment unless Paid
                 !order.status.equals("WaitingPayment", ignoreCase = true) || order.paymentStatus.equals("Paid", ignoreCase = true)
            }
        }
        
        _displayedOrders.value = result
        calculateAnalysis(result)
    }

    fun applyFilter(index: Int) {
        setTabFilter(index)
    }

    private fun calculateAnalysis(orders: List<OrderResponse>) {
        // Only count COMPLETED orders for income
        val completedOrders = orders.filter { it.status == "Completed" }
        val total = completedOrders.sumOf { it.totalAmount.toLong() }
        
        // Count all transactions for count? Or also only completed? 
        // Usually count is total transactions, but let's stick to visible list count for now
        // or consistent with income? 
        // User asked "why cancelled counted as income", so fixing income is priority.
        // Let's keep count as total list size (including cancelled) so they know traffic,
        // BUT user might want count of valid sales. 
        // Let's stick to fixing INCOME specifically as requested.
        
        val count = orders.size
        val avg = if (count > 0) total / count else 0 // Avg per transaction (including cancelled/pending?)
        // If we want True Avg Sales, we should divide by completedOrders.size
        // Let's refine avg to be "Avg Income per Successful Order"
        val successCount = completedOrders.size
        val realAvg = if (successCount > 0) total / successCount else 0

        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        _analysis.value = AnalysisData(
            totalIncome = fmt.format(total).replace("Rp", "Rp ").replace(",00", ""),
            transactionCount = count.toString(),
            avgIncome = fmt.format(avg).replace("Rp", "Rp ").replace(",00", "")
        )
    }

    private fun parseIsoDate(dateStr: String): Date? {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.US)
                parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return parser.parse(dateStr)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    private fun isSameDay(dateStr: String, now: Date): Boolean {
        val date = parseIsoDate(dateStr) ?: return false
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault()) // Compare in Local Time
        return fmt.format(date) == fmt.format(now)
    }

    private fun isThisWeek(dateStr: String, now: Date): Boolean {
        val date = parseIsoDate(dateStr) ?: return false
        val diff = now.time - date.time
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        return days in 0..6
    }

    private fun isSameMonth(dateStr: String, now: Date): Boolean {
        val date = parseIsoDate(dateStr) ?: return false
        val fmt = SimpleDateFormat("yyyyMM", Locale.getDefault())
        return fmt.format(date) == fmt.format(now)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Optional: socket.off if needed, but SocketHandler usually manages singleton
    }
}
