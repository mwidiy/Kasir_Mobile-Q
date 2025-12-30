package com.example.kasir.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Banner
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import com.example.kasir.utils.SocketHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class BannerViewModel : ViewModel() {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners = _banners.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var selectedImageUri by mutableStateOf<Uri?>(null)

    init {
        try {
            // Check if socket is initialized, if not initialize it
            try {
                SocketHandler.getSocket()
            } catch (e: Exception) {
                SocketHandler.setSocket()
            }
            SocketHandler.establishConnection()
            
            SocketHandler.getSocket()?.on("banners_updated") {
                // Handle event on background thread, fetchBanners handles scope
                fetchBanners(isSilent = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fetchBanners()
    }

    fun fetchBanners(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _isLoading.value = true
            }
            _errorMessage.value = null
            try {
                val response = RetrofitClient.instance.getBanners()
                // Response is Response<ApiResponse<List<Banner>>>
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success) {
                        _banners.value = apiResponse.data
                    } else {
                        _errorMessage.value = apiResponse.message
                    }
                } else {
                     _errorMessage.value = "Error: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat banner: ${e.localizedMessage}"
            } finally {
                if (!isSilent) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun createPartFromString(stringData: String): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), stringData)
    }

    fun saveBanner(
        context: Context,
        id: Int? = null,
        title: String,
        subtitle: String?,
        highlightText: String?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val titlePart = createPartFromString(title)
                val subtitlePart = if (subtitle != null) createPartFromString(subtitle) else null
                val highlightPart = if (highlightText != null) createPartFromString(highlightText) else null
                val isActivePart = createPartFromString(isActive.toString())

                var imagePart: MultipartBody.Part? = null
                if (selectedImageUri != null) {
                    val file = FileUtils.getFileFromUri(context, selectedImageUri!!)
                    if (file != null) {
                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(selectedImageUri!!) ?: "image/jpeg"
                        val requestFile = RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                if (id == null) {
                    // Add Banner
                    if (imagePart == null) {
                        _errorMessage.value = "Gambar wajib diisi untuk banner baru"
                        return@launch
                    }
                    val response = RetrofitClient.instance.addBanner(
                        titlePart, subtitlePart, highlightPart, imagePart, isActivePart
                    )
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        selectedImageUri = null
                        fetchBanners()
                    } else {
                        _errorMessage.value = response.body()?.message ?: "Gagal menambah banner"
                    }
                } else {
                    // Update Banner
                    // imagePart is nullable here, which matches ApiService signature
                    val response = RetrofitClient.instance.updateBanner(
                        id, titlePart, subtitlePart, highlightPart, imagePart, isActivePart
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        selectedImageUri = null
                        fetchBanners()
                    } else {
                        _errorMessage.value = response.body()?.message ?: "Gagal update banner"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan banner: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBanner(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.instance.deleteBanner(id)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchBanners()
                } else {
                     _errorMessage.value = response.body()?.message ?: "Gagal menghapus banner"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus banner: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
