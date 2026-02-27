package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Product
import com.example.kasir.data.model.Category
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MenuViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    var selectedCategoryId by mutableStateOf(0)
    
    private var socketDebounceJob: Job? = null
    private val toggleJobs = mutableMapOf<Int, Job>()
    
    // SERIAL QUEUE: Prevent bombarding the single-threaded backend
    private val toggleMutex = Mutex()
    
    // Track pending api requests to prevent socket overwrites during queued toggles
    private val pendingTogglesCount = java.util.concurrent.atomic.AtomicInteger(0)

    init {
        // Initialize Socket
        com.example.kasir.utils.SocketHandler.setSocket()
        com.example.kasir.utils.SocketHandler.establishConnection()
        
        val mSocket = com.example.kasir.utils.SocketHandler.getSocket()
        mSocket.on("products_updated") {
            // Trigger fetch in silent mode with 1-second debounce (DoS Protection)
            socketDebounceJob?.cancel()
            socketDebounceJob = viewModelScope.launch {
                delay(1000L)
                fetchProducts(isSilent = true)
            }
        }

        fetchProducts()
        fetchCategories()
    }

    override fun onCleared() {
        super.onCleared()
        com.example.kasir.utils.SocketHandler.closeConnection()
    }

    fun fetchProducts(isSilent: Boolean = false) {
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
                val response = RetrofitClient.instance.getProducts()
                if (response.success) {
                    _products.value = response.data
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data: ${e.localizedMessage}"
            } finally {
                if (!isSilent) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getCategories()
                if (response.success) {
                    _categories.value = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            // OPTIMISTIC UI: Instant add to state with dummy ID
            val dummyId = -(System.currentTimeMillis().toInt()) // Ensure uniqueness
            val optimisticCategory = Category(id = dummyId, name = name)
            _categories.value = _categories.value + optimisticCategory

            try {
                val response = RetrofitClient.instance.addCategory(mapOf("name" to name))
                if (response.success) {
                    fetchCategories() // Silently fetch real ID from server
                } else {
                    // Rollback on failure
                    _categories.value = _categories.value.filter { it.id != dummyId }
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                // Rollback on crash
                _categories.value = _categories.value.filter { it.id != dummyId }
                _errorMessage.value = "Gagal menambah kategori: ${e.localizedMessage}"
            }
        }
    }

    fun updateCategory(id: Int, name: String) {
        viewModelScope.launch {
            // OPTIMISTIC UI: Instant update in state
            val originalCategories = _categories.value.toList()
            _categories.value = _categories.value.map {
                if (it.id == id) it.copy(name = name) else it
            }

            try {
                val response = RetrofitClient.instance.updateCategory(id, mapOf("name" to name))
                if (response.success) {
                    fetchCategories() // Silent sync
                } else {
                    // Rollback
                    _categories.value = originalCategories
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                // Rollback
                _categories.value = originalCategories
                _errorMessage.value = "Gagal update kategori: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            // OPTIMISTIC UI: Instant remove from state
            val originalCategories = _categories.value.toList()
            _categories.value = _categories.value.filter { it.id != id }

            try {
                val response = RetrofitClient.instance.deleteCategory(id)
                if (response.success) {
                    fetchCategories() // Silent sync
                } else {
                    // Rollback
                    _categories.value = originalCategories
                    _errorMessage.value = response.message // Display backend error (e.g., used by products)
                }
            } catch (e: Exception) {
                // Rollback
                _categories.value = originalCategories
                _errorMessage.value = "Gagal menghapus kategori: ${e.localizedMessage}"
            }
        }
    }


    private fun createPartFromString(stringData: String): okhttp3.RequestBody {
        return okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), stringData)
    }

    fun addProduct(product: Product, imageUri: android.net.Uri? = null, context: android.content.Context) {
        viewModelScope.launch {
            // OPTIMISTIC SKELETON UI: Instant add to state with dummy ID to simulate upload
            val dummyId = -(System.currentTimeMillis().toInt()) // Ensure uniqueness
            val optimisticProduct = product.copy(id = dummyId, image = "uploading")
            val originalProducts = _products.value.toList()
            _products.value = listOf(optimisticProduct) + _products.value

            _errorMessage.value = null
            var tempFile: java.io.File? = null
            try {
                val name = createPartFromString(product.name)
                // val category = createPartFromString(product.category)
                val categoryId = createPartFromString(product.categoryId?.toString() ?: "0")
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())
                val ar3dModel = if (product.ar3dModel != null) createPartFromString(product.ar3dModel) else null
                val isArActive = createPartFromString(product.isArActive.toString())
                
                var imagePart: okhttp3.MultipartBody.Part? = null
                if (imageUri != null) {
                    val file = FileUtils.getFileFromUri(context, imageUri)
                    tempFile = file // TRACK FOR DELETION
                    if (file != null) {
                        // VALIDASI UKURAN FILE (Max 5MB)
                        val fileSizeInBytes = file.length()
                        if (fileSizeInBytes > 5 * 1024 * 1024) {
                            val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                            _errorMessage.value = "Ukuran gambar memakan $fileSizeInMB MB. Maksimal hanya 5MB ya! 📸"
                            _isLoading.value = false
                            tempFile?.delete() // Cleanup on failure
                            return@launch
                        }

                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(imageUri) ?: "image/jpeg"
                        val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                val response = RetrofitClient.instance.addProduct(
                    name, categoryId, price, description, imagePart, isActive, ar3dModel, isArActive
                )
                
                if (response.success) {
                    fetchProducts(isSilent = true) // Silent sync to replace dummy with real data
                } else {
                    _products.value = originalProducts // Rollback
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _products.value = originalProducts // Rollback
                _errorMessage.value = "Gagal menambah produk: ${e.localizedMessage}"
            } finally {
                tempFile?.delete() // DELETE TEMP FILE TO PREVENT STORAGE LEAK
            }
        }
    }

    fun updateProduct(id: Int, product: Product, imageUri: android.net.Uri? = null, context: android.content.Context? = null) {
        viewModelScope.launch {
            // OPTIMISTIC UPDATE
            val originalProducts = _products.value.toList()
            _products.value = _products.value.map {
                if (it.id == id) product.copy(id = id, image = it.image) else it // Keep old image during update upload
            }

            _errorMessage.value = null
            var tempFile: java.io.File? = null
            try {
                val name = createPartFromString(product.name)
                // val category = createPartFromString(product.category)
                val categoryId = createPartFromString(product.categoryId?.toString() ?: "0")
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())
                val ar3dModel = if (product.ar3dModel != null) createPartFromString(product.ar3dModel) else null
                val isArActive = createPartFromString(product.isArActive.toString())

                var imagePart: okhttp3.MultipartBody.Part? = null
                if (imageUri != null && context != null) {
                    val file = FileUtils.getFileFromUri(context, imageUri)
                    tempFile = file // TRACK FOR DELETION
                    if (file != null) {
                        // VALIDASI UKURAN FILE (Max 5MB)
                        val fileSizeInBytes = file.length()
                        if (fileSizeInBytes > 5 * 1024 * 1024) {
                            val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                            _errorMessage.value = "Ukuran gambar memakan $fileSizeInMB MB. Maksimal hanya 5MB ya! 📸"
                            _isLoading.value = false
                            tempFile?.delete() // Cleanup on failure
                            return@launch
                        }

                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(imageUri) ?: "image/jpeg"
                        val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                val response = RetrofitClient.instance.updateProduct(
                    id, name, categoryId, price, description, imagePart, isActive, ar3dModel, isArActive
                )
                
                if (response.success) {
                    // Sync with backend (silent) to get real image URL if changed
                    fetchProducts(isSilent = true)
                } else {
                    _products.value = originalProducts // Rollback
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _products.value = originalProducts // Rollback
                _errorMessage.value = "Gagal mengupdate produk: ${e.localizedMessage}"
            } finally {
                tempFile?.delete() // DELETE TEMP FILE TO PREVENT STORAGE LEAK
            }
        }
    }

    fun toggleProductStatus(product: Product, onComplete: () -> Unit = {}) {
        val updatedProduct = product.copy(isActive = !product.isActive)
        val originalProducts = _products.value.toList() // Snapshot for Rollback

        // OPTIMISTIC UI: Instant Switch
        _products.value = _products.value.map {
            if (it.id == product.id) updatedProduct else it
        }

        // NO VIEWMODEL CANCEL DEBOUNCE HERE! 
        // The UI's 3-click Anti-Spam (Tahap 21) already protects against single-item spam.
        // If we cancel here, we destroy valid sequential mass-toggles (Item 2, 3, 4, 5).
        
        toggleJobs[product.id] = viewModelScope.launch {
             pendingTogglesCount.incrementAndGet() // Block socket updates globally
             try {
                val name = createPartFromString(updatedProduct.name)
                val categoryId = createPartFromString(updatedProduct.categoryId?.toString() ?: "0")
                val price = createPartFromString(updatedProduct.price.toString())
                val description = createPartFromString(updatedProduct.description ?: "")
                val isActive = createPartFromString(updatedProduct.isActive.toString())
                val ar3dModel = if (updatedProduct.ar3dModel != null) createPartFromString(updatedProduct.ar3dModel) else null
                val isArActive = createPartFromString(updatedProduct.isArActive.toString())
                
                // WAIT IN LINE: Execute API requests one-by-one
                val response = toggleMutex.withLock {
                    RetrofitClient.instance.updateProduct(
                         updatedProduct.id, name, categoryId, price, description, null, isActive, ar3dModel, isArActive
                    )
                }
                
                if (response.success) {
                    fetchProducts(isSilent = true) // Final sync
                } else {
                    _products.value = originalProducts // Rollback
                    _errorMessage.value = response.message
                }
             } catch (e: Exception) {
                 _products.value = originalProducts // Rollback
                 _errorMessage.value = "Gagal mengubah status: ${e.localizedMessage}"
                 e.printStackTrace()
             } finally {
                 pendingTogglesCount.decrementAndGet() // Unblock socket updates
                 onComplete() // INFINITE LOCK RELAY: Tell UI it is safe to listen to server again
             }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            // OPTIMISTIC UI: Instant remove from screen (No Loading Dialog)
            val originalProducts = _products.value.toList()
            _products.value = _products.value.filter { it.id != id }
            
            _errorMessage.value = null
            try {
                val response = RetrofitClient.instance.deleteProduct(id)
                if (response.success) {
                    fetchProducts(isSilent = true) // Silent sync
                } else {
                    // Rollback
                    _products.value = originalProducts
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                // Rollback
                _products.value = originalProducts
                _errorMessage.value = "Gagal menghapus produk: ${e.localizedMessage}"
            }
        }
    }
}
