package com.example.kasir.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.kasir.data.model.Banner
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import com.example.kasir.utils.SocketHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    private var socketDebounceJob: Job? = null
    private val toggleJobs = mutableMapOf<Int, Job>()
    
    // SERIAL QUEUE: Prevent bombarding the single-threaded backend
    private val toggleMutex = Mutex()
    
    // Track pending api requests to prevent socket overwrites during queued toggles
    private val pendingTogglesCount = java.util.concurrent.atomic.AtomicInteger(0)

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
                // Trigger fetch in silent mode with 1-second debounce (DoS Protection)
                socketDebounceJob?.cancel()
                socketDebounceJob = viewModelScope.launch {
                    delay(1000L)
                    fetchBanners(isSilent = true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fetchBanners()
    }

    fun fetchBanners(isSilent: Boolean = false) {
        viewModelScope.launch {
            // Anti-Socket Overwrite: If we have queued toggles, ignore background socket updates
            if (isSilent && pendingTogglesCount.get() > 0) {
                return@launch
            }

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
        isActive: Boolean,
        onSuccess: () -> Unit // Callback added
    ) {
        viewModelScope.launch {
            // Remove full-screen loading for Optimistic UX
            _errorMessage.value = null
            var tempFile: java.io.File? = null // TRACK CACHE FOR DELETION
            
            // OPTIMISTIC UI PREPARATION
            val originalBanners = _banners.value.toList()
            if (id == null) {
                // SKELETON UI: Fake Add
                val dummyId = -(System.currentTimeMillis().toInt())
                val optimisticBanner = Banner(dummyId, title, subtitle, highlightText, "uploading", isActive)
                _banners.value = listOf(optimisticBanner) + _banners.value
            } else {
                // SKELETON UI: Fake Update
                _banners.value = _banners.value.map {
                    if (it.id == id) Banner(id, title, subtitle, highlightText, it.image, isActive) else it
                }
            }

            try {
                val titlePart = createPartFromString(title)
                val subtitlePart = if (subtitle != null) createPartFromString(subtitle) else null
                val highlightPart = if (highlightText != null) createPartFromString(highlightText) else null
                val isActivePart = createPartFromString(isActive.toString())

                var imagePart: MultipartBody.Part? = null
                if (selectedImageUri != null) {
                    val file = FileUtils.getFileFromUri(context, selectedImageUri!!)
                    tempFile = file // TRACK THIS IMAGE FILE
                    if (file != null) {
                        // VALIDASI UKURAN FILE (Max 5MB)
                        val fileSizeInBytes = file.length()
                        val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                        
                        if (fileSizeInBytes > 5 * 1024 * 1024) {
                             _errorMessage.value = "Ukuran gambar memakan $fileSizeInMB MB. Maksimal hanya 5MB ya! 📸"
                             _isLoading.value = false
                             return@launch
                        }

                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(selectedImageUri!!) ?: "image/jpeg"
                        val requestFile = RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                if (id == null) {
                    // Add Banner
                    if (imagePart == null) {
                        _banners.value = originalBanners // Rollback
                        _errorMessage.value = "Gambar wajib diisi untuk banner baru"
                        return@launch
                    }
                    val response = RetrofitClient.instance.addBanner(
                        titlePart, subtitlePart, highlightPart, imagePart, isActivePart
                    )
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        selectedImageUri = null
                        fetchBanners(isSilent = true) // Silent sync to remove skeleton
                        onSuccess() // Trigger navigation
                    } else {
                        _banners.value = originalBanners // Rollback
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
                        fetchBanners(isSilent = true) // Silent sync
                        onSuccess() // Trigger navigation
                    } else {
                        _banners.value = originalBanners // Rollback
                        _errorMessage.value = response.body()?.message ?: "Gagal update banner"
                    }
                }
            } catch (e: Exception) {
                _banners.value = originalBanners // Rollback
                _errorMessage.value = "Gagal menyimpan banner: ${e.localizedMessage}"
            } finally {
                // No need to set isLoading to false anymore for this action
                tempFile?.delete() // GARBAGE COLLECTION: Prevent Cache Leak (Local DoS)
            }
        }
    }

    fun deleteBanner(id: Int) {
        viewModelScope.launch {
            // OPTIMISTIC UI: Instant remove from screen (No Loading Dialog)
            val originalBanners = _banners.value.toList()
            _banners.value = _banners.value.filter { it.id != id }
            
            _errorMessage.value = null
            try {
                val response = RetrofitClient.instance.deleteBanner(id)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchBanners(isSilent = true) // Silent sync
                } else {
                    // Rollback
                    _banners.value = originalBanners
                     _errorMessage.value = response.body()?.message ?: "Gagal menghapus banner"
                }
            } catch (e: Exception) {
                // Rollback
                _banners.value = originalBanners
                _errorMessage.value = "Gagal menghapus banner: ${e.localizedMessage}"
            }
        }
    }

    fun toggleBannerStatus(banner: Banner, onComplete: () -> Unit = {}) {
        val updatedBanner = banner.copy(isActive = !banner.isActive)
        val originalBanners = _banners.value.toList() // Snapshot for Rollback

        // OPTIMISTIC UI: Instant Switch
        _banners.value = _banners.value.map {
            if (it.id == banner.id) updatedBanner else it
        }

        // NO VIEWMODEL CANCEL DEBOUNCE HERE! 
        // The UI's 3-click Anti-Spam (Tahap 21) already protects against single-item spam.
        // If we cancel here, we destroy valid sequential mass-toggles (Item 2, 3, 4, 5).
        
        toggleJobs[banner.id] = viewModelScope.launch {
             pendingTogglesCount.incrementAndGet() // Block socket updates globally
             try {
                val titlePart = createPartFromString(updatedBanner.title)
                val subtitlePart = if (updatedBanner.subtitle != null) createPartFromString(updatedBanner.subtitle) else null
                val highlightPart = if (updatedBanner.highlightText != null) createPartFromString(updatedBanner.highlightText) else null
                val isActivePart = createPartFromString(updatedBanner.isActive.toString())
                
                // WAIT IN LINE: Execute API requests one-by-one
                val response = toggleMutex.withLock {
                    RetrofitClient.instance.updateBanner(
                         updatedBanner.id, titlePart, subtitlePart, highlightPart, null, isActivePart
                    )
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchBanners(isSilent = true) // Final sync
                } else {
                    _banners.value = originalBanners // Rollback
                    _errorMessage.value = response.body()?.message ?: "Gagal mengubah status"
                }
             } catch (e: Exception) {
                 _banners.value = originalBanners // Rollback
                 _errorMessage.value = "Gagal mengubah status: ${e.localizedMessage}"
             } finally {
                 pendingTogglesCount.decrementAndGet() // Unblock socket updates
                 onComplete() // INFINITE LOCK RELAY: Tell BannerCard to unlock
             }
        }
    }
}
