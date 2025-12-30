package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Location
import com.example.kasir.data.model.Table
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

    var selectedLocationId by mutableStateOf(0) // 0 means "All"

    init {
        fetchLocations()
        fetchTables()
    }

    fun fetchLocations() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getLocations()
                if (response.success) {
                    _locations.value = response.data
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun fetchTables() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.getTables()
                if (response.success) {
                    _tables.value = response.data
                }
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
                val response = RetrofitClient.instance.addLocation(mapOf("name" to name))
                if (response.success) {
                    fetchLocations() // Refresh list immediately
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun updateLocation(id: Int, name: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateLocation(id, mapOf("name" to name))
                if (response.success) fetchLocations()
                else _errorMessage.value = response.message
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteLocation(id)
                if (response.success) {
                    fetchLocations()
                    if (selectedLocationId == id) selectedLocationId = 0
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus lokasi: ${e.localizedMessage}"
            }
        }
    }

    fun addTable(name: String, locationId: Int) {
        viewModelScope.launch {
            try {
                val body = mapOf("name" to name, "locationId" to locationId)
                val response = RetrofitClient.instance.addTable(body)
                if (response.success) {
                    fetchTables()
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah meja: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTable(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteTable(id)
                if (response.success) fetchTables()
                else _errorMessage.value = response.message
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus meja: ${e.localizedMessage}"
            }
        }
    }
}
