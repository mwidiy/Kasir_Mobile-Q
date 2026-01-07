package com.example.kasir.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Store
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ProfileViewModel : ViewModel() {
    private val _storeState = MutableStateFlow<Store?>(null)
    val storeState: StateFlow<Store?> = _storeState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchStore()
    }

    fun fetchStore() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.getStore()
                if (response.success) {
                    _storeState.value = response.data
                }
            } catch (e: Exception) {
                // If 404 or auth fails, it might throw or return error
                // For now, silent fail or log
                e.printStackTrace()
                _errorMessage.value = "Gagal memuat profil: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                 val response = RetrofitClient.instance.updateStore(mapOf("name" to newName))
                 if (response.success && response.data != null) {
                     _storeState.value = response.data
                 }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update nama: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadLogo(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = FileUtils.getFileFromUri(context, uri)
                if (file != null) {
                    val contentResolver = context.contentResolver
                    val type = contentResolver.getType(uri) ?: "image/jpeg"
                    val requestFile = RequestBody.create(type.toMediaTypeOrNull(), file)
                    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    
                    val response = RetrofitClient.instance.uploadLogo(body)
                    if (response.success && response.data != null) {
                        _storeState.value = response.data
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal upload logo: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadQris(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = FileUtils.getFileFromUri(context, uri)
                if (file != null) {
                    val contentResolver = context.contentResolver
                    val type = contentResolver.getType(uri) ?: "image/jpeg"
                    val requestFile = RequestBody.create(type.toMediaTypeOrNull(), file)
                    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

                    val response = RetrofitClient.instance.uploadQris(body)
                    if (response.success && response.data != null) {
                        _storeState.value = response.data
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal upload QRIS: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
