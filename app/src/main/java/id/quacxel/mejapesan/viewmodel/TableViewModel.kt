package id.quacxel.mejapesan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.Location
import id.quacxel.mejapesan.data.model.Table
import id.quacxel.mejapesan.data.model.TableRequest
import id.quacxel.mejapesan.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TableViewModel : ViewModel() {
    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations

    private val _tables = MutableStateFlow<List<Table>>(emptyList())
    val tables: StateFlow<List<Table>> = _tables

    private val _isStoreOpen = MutableStateFlow(false)
    val isStoreOpen: StateFlow<Boolean> = _isStoreOpen

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var selectedLocationName by mutableStateOf("Semua")

    // Anti-Spam Queue Mechanism
    val pendingTogglesCount = MutableStateFlow(0)
    private val toggleMutex = Mutex()

    init {
        refreshData()
    }

    fun refreshData(isSilent: Boolean = false) {
        viewModelScope.launch {
            // ANTI-BOUNCE: Block intermediate server fetches if the Mutex queue is still actively processing user toggles
            if (isSilent && pendingTogglesCount.value > 0) {
                return@launch
            }

            if (!isSilent) _isLoading.value = true
            try {
                // Optimasi Performa: Jalankan 3 panggilan API secara Paralel
                val storeDeferred = async { RetrofitClient.instance.getStore() }
                val tablesDeferred = async { RetrofitClient.instance.getTables() }
                val locsDeferred = async { RetrofitClient.instance.getLocations() }

                // SAFE AWAIT: Each call wrapped individually to prevent one failure from crashing all
                try {
                    val storeResponse = storeDeferred.await()
                    if (storeResponse.success && storeResponse.data != null) {
                        _isStoreOpen.value = storeResponse.data.isOpen
                    }
                } catch (e: Exception) {
                    if (!isSilent) _errorMessage.value = "Gagal memuat data toko"
                }

                try {
                    val tables = tablesDeferred.await()
                    _tables.value = tables
                } catch (e: Exception) {
                    // JSON parse error (server returned error object instead of array) or network error
                    if (!isSilent) _errorMessage.value = "Gagal memuat data meja"
                    _tables.value = emptyList()
                }

                try {
                    val locations = locsDeferred.await()
                    _locations.value = locations
                } catch (e: Exception) {
                    if (!isSilent) _errorMessage.value = "Gagal memuat lokasi"
                    _locations.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat ulang data: ${e.localizedMessage}"
            } finally {
                if (!isSilent) _isLoading.value = false
            }
        }
    }

    fun updateStoreStatus(isOpen: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            pendingTogglesCount.value++
            // ZERO-BOUNCE OPTIMISTIC UI: Update instantly outside the Mutex
            val originalStatus = _isStoreOpen.value
            val originalTables = _tables.value.toList() // Save for rollback
            
            _isStoreOpen.value = isOpen
            // Tahap 30: Cascade Optimistic Update ke list meja UI
            _tables.value = _tables.value.map { it.copy(isActive = isOpen) }
            
            toggleMutex.withLock {
                try {
                    val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(isOpen = isOpen))
                    if (response.success && response.data != null) {
                        // Success -> no state revert needed
                        refreshData(isSilent = true) // Target refresh tables quietly (safe now with Gatekeeper)
                    } else {
                        // Backend Error -> Full Server Sync (instead of volatile memory snapshot revert)
                        refreshData(isSilent = false)
                        _errorMessage.value = "Gagal update status toko"
                    }
                } catch(e: Exception) {
                     // Network Error -> Full Server Sync
                     refreshData(isSilent = false)
                     _errorMessage.value = "Error: ${e.localizedMessage}"
                } finally {
                    pendingTogglesCount.value-- // Unblock globale sockets 
                    onComplete()
                }
            }
        }
    }

    fun fetchLocations() {
        viewModelScope.launch {
            try {
                // Now returns List<Location> directly
                val response = RetrofitClient.instance.getLocations()
                _locations.value = response
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun addLocation(name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Returns LocationData directly. If successful, valid object returned.
                RetrofitClient.instance.addLocation(mapOf("name" to name))
                refreshData(isSilent = true) // Refresh list silently
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah lokasi: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun updateLocation(id: Int, name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.updateLocation(id, mapOf("name" to name))
                refreshData(isSilent = true)
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update lokasi: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun deleteLocation(id: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteLocation(id)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                    onComplete(true)
                } else {
                    _errorMessage.value = "Error: ${response.code()}"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus lokasi: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun addTable(name: String, locationId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val qrCode = "QR-${name}-${System.currentTimeMillis()}"
                val request = TableRequest(name, locationId, qrCode, true)
                // Returns Response<Table> now
                val response = RetrofitClient.instance.addTable(request)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                    onComplete(true)
                } else {
                     _errorMessage.value = "Gagal menambah meja: ${response.code()}"
                     onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah meja: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun deleteTable(id: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.deleteTable(id)
                refreshData(isSilent = true)
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus meja: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun updateTable(id: Int, name: String, locationId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Find existing qrCode or generate a new placeholder if missing
                val existingTable = _tables.value.find { it.id == id }
                val qrCode = existingTable?.qrCode ?: "QR-${name}-${System.currentTimeMillis()}"
                val request = TableRequest(name, locationId, qrCode, existingTable?.isActive ?: true)
                val response = RetrofitClient.instance.updateTable(id, request)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                    onComplete(true)
                } else {
                     _errorMessage.value = "Gagal update meja: ${response.code()}"
                     onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update meja: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun updateTableStatus(id: Int, isActive: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            pendingTogglesCount.value++
            // ZERO-BOUNCE OPTIMISTIC UI: Update instantly outside the Mutex
            _tables.value = _tables.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
            
            toggleMutex.withLock {
                try {
                    val response = RetrofitClient.instance.updateTableStatus(id, mapOf("isActive" to isActive))
                    if (response.isSuccessful && response.body() != null) {
                        // Success -> Backend data confirms
                        refreshData(isSilent = true) // Sync any other changes covertly (Gatekeeper protected)
                    } else {
                        // Backend Error -> Full Data Resync (Better Rollback than snapshotting history)
                        refreshData(isSilent = false)
                        _errorMessage.value = "Gagal memodifikasi status meja"
                    }
                } catch (e: Exception) {
                    // Network Error -> Full Data Resync
                    refreshData(isSilent = false)
                    _errorMessage.value = "Koneksi terputus: ${e.localizedMessage}"
                } finally {
                    pendingTogglesCount.value-- // Unblock global socket sync 
                    onComplete() // INFINITE LOCK RELAY -> Release UI ONLY when API strictly finishes
                }
            }
        }
    }
}
