package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Location
import com.example.kasir.data.model.Table
import com.example.kasir.data.model.TableRequest
import com.example.kasir.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class TableViewModel : ViewModel() {
    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations

    private val _tables = MutableStateFlow<List<Table>>(emptyList())
    val tables: StateFlow<List<Table>> = _tables

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var selectedLocationName by mutableStateOf("Semua")

    init {
        fetchLocations()
        fetchTables()
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

    fun fetchTables() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // ApiService returns List<Table> directly
                val response = RetrofitClient.instance.getTables()
                _tables.value = response
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat meja: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addLocation(name: String) {
        viewModelScope.launch {
            try {
                // Returns LocationData directly. If successful, valid object returned.
                RetrofitClient.instance.addLocation(mapOf("name" to name))
                fetchLocations() // Refresh list immediately
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun updateLocation(id: Int, name: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.updateLocation(id, mapOf("name" to name))
                fetchLocations()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteLocation(id)
                if (response.isSuccessful) {
                    fetchLocations()
                    // Logic reset location selection if needed
                } else {
                    _errorMessage.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun addTable(name: String, locationId: Int) {
        viewModelScope.launch {
            try {
                val qrCode = "QR-${name}-${System.currentTimeMillis()}"
                val request = TableRequest(name, locationId, qrCode, true)
                // Returns Response<Table> now
                val response = RetrofitClient.instance.addTable(request)
                if (response.isSuccessful) {
                    fetchTables()
                } else {
                     _errorMessage.value = "Gagal menambah meja: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah meja: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTable(id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.deleteTable(id)
                fetchTables()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus meja: ${e.localizedMessage}"
            }
        }
    }
}
