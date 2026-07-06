package id.quacxel.mejapesan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.Location
import id.quacxel.mejapesan.data.model.LocationData
import id.quacxel.mejapesan.data.model.StoreUpdateRequest
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
                val storeDeferred = async { 
                    try { RetrofitClient.instance.getStore() } catch (e: Exception) { null }
                }
                val tablesDeferred = async { 
                    try { RetrofitClient.instance.getTables() } catch (e: Exception) { null }
                }
                val locsDeferred = async { 
                    try { RetrofitClient.instance.getLocations() } catch (e: Exception) { null }
                }

                // SAFE AWAIT: Handle nulls returned from catch blocks
                val storeResponse = storeDeferred.await()
                if (storeResponse != null && storeResponse.success && storeResponse.data != null) {
                    _isStoreOpen.value = storeResponse.data.isOpen
                } else if (storeResponse == null) {
                    if (!isSilent) _errorMessage.value = "Gagal memuat data toko"
                }

                val tables = tablesDeferred.await()
                if (tables != null) {
                    _tables.value = tables
                } else {
                    if (!isSilent) _errorMessage.value = "Gagal memuat data meja"
                    _tables.value = emptyList()
                }

                val locations = locsDeferred.await()
                if (locations != null) {
                    _locations.value = locations
                } else {
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
                    val response = RetrofitClient.instance.updateStore(StoreUpdateRequest(isOpen = isOpen))
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
            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val dummyId = -(System.currentTimeMillis().toInt())
            val optimisticLoc = Location(id = dummyId, name = name)
            _locations.value = _locations.value + optimisticLoc
            onComplete(true)

            try {
                RetrofitClient.instance.addLocation(mapOf("name" to name))
                refreshData(isSilent = true) // Refresh list silently
            } catch (e: Exception) {
                _locations.value = _locations.value.filter { it.id != dummyId }
                _errorMessage.value = "Gagal menambah lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun updateLocation(id: Int, name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val originalLocations = _locations.value.toList()
            val originalTables = _tables.value.toList()
            
            _locations.value = _locations.value.map { if (it.id == id) it.copy(name = name) else it }
            _tables.value = _tables.value.map {
                if (it.location?.id == id) it.copy(location = LocationData(id = id, name = name)) else it
            }
            onComplete(true)

            try {
                RetrofitClient.instance.updateLocation(id, mapOf("name" to name))
                refreshData(isSilent = true)
            } catch (e: Exception) {
                _locations.value = originalLocations
                _tables.value = originalTables
                _errorMessage.value = "Gagal update lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun deleteLocation(id: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // Local Validation: Do not allow deletion if any table uses this location
            val hasTables = _tables.value.any { it.location?.id == id }
            if (hasTables) {
                _errorMessage.value = "Mohon maaf lokasi ini belum bisa di hapus karena masih ada meja yang memakai lokasi ini"
                onComplete(false)
                return@launch
            }

            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val originalLocations = _locations.value.toList()
            _locations.value = _locations.value.filter { it.id != id }
            onComplete(true)

            try {
                val response = RetrofitClient.instance.deleteLocation(id)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                } else {
                    _locations.value = originalLocations
                    _errorMessage.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _locations.value = originalLocations
                _errorMessage.value = "Gagal menghapus lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun addTable(name: String, locationId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val dummyId = -(System.currentTimeMillis().toInt())
            val qrCode = "QR-${name}-${System.currentTimeMillis()}"
            val locObj = _locations.value.find { it.id == locationId }
            val locData = if (locObj != null) LocationData(locObj.id, locObj.name) else null
            val optimisticTable = Table(id = dummyId, name = name, location = locData, qrCode = qrCode, isActive = true)
            
            _tables.value = _tables.value + optimisticTable
            onComplete(true)

            try {
                val request = TableRequest(name, locationId, qrCode, true)
                val response = RetrofitClient.instance.addTable(request)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                } else {
                    _tables.value = _tables.value.filter { it.id != dummyId }
                    _errorMessage.value = "Gagal menambah meja: ${response.code()}"
                }
            } catch (e: Exception) {
                _tables.value = _tables.value.filter { it.id != dummyId }
                _errorMessage.value = "Gagal menambah meja: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTable(id: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val originalTables = _tables.value.toList()
            _tables.value = _tables.value.filter { it.id != id }
            onComplete(true)

            try {
                RetrofitClient.instance.deleteTable(id)
                refreshData(isSilent = true)
            } catch (e: Exception) {
                _tables.value = originalTables
                _errorMessage.value = "Gagal menghapus meja: ${e.localizedMessage}"
            }
        }
    }

    fun updateTable(id: Int, name: String, locationId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // ZERO-LATENCY GOJEK STYLE: Close modal in 0ms & update RAM state!
            val existingTable = _tables.value.find { it.id == id }
            val qrCode = existingTable?.qrCode ?: "QR-${name}-${System.currentTimeMillis()}"
            val locObj = _locations.value.find { it.id == locationId }
            val locData = if (locObj != null) LocationData(locObj.id, locObj.name) else existingTable?.location
            
            val originalTables = _tables.value.toList()
            _tables.value = _tables.value.map {
                if (it.id == id) it.copy(name = name, location = locData, qrCode = qrCode) else it
            }
            onComplete(true)

            try {
                val request = TableRequest(name, locationId, qrCode, existingTable?.isActive ?: true)
                val response = RetrofitClient.instance.updateTable(id, request)
                if (response.isSuccessful) {
                    refreshData(isSilent = true)
                } else {
                    _tables.value = originalTables
                    _errorMessage.value = "Gagal update meja: ${response.code()}"
                }
            } catch (e: Exception) {
                _tables.value = originalTables
                _errorMessage.value = "Gagal update meja: ${e.localizedMessage}"
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
