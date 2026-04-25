package id.quacxel.mejapesan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.Product
import id.quacxel.mejapesan.data.model.Category
import id.quacxel.mejapesan.data.network.RetrofitClient
import id.quacxel.mejapesan.utils.FileUtils
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
    
    // TAHAP 63: Optimistic Image Cache (Key: ProductName, Value: Local file:// URI)
    private val optimisticImagesMap = mutableMapOf<String, String>()

    init {
        // Initialize Socket
        id.quacxel.mejapesan.utils.SocketHandler.setSocket()
        id.quacxel.mejapesan.utils.SocketHandler.establishConnection()
        
        val mSocket = id.quacxel.mejapesan.utils.SocketHandler.getSocket()
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
        id.quacxel.mejapesan.utils.SocketHandler.closeConnection()
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
                    // TAHAP 63: Silent Interceptor (Bypass Cloudinary URL if we have a local cache for this session)
                    val interceptedData = response.data.map { serverProduct ->
                        val localImage = optimisticImagesMap[serverProduct.name]
                        if (localImage != null) {
                            serverProduct.copy(image = localImage)
                        } else {
                            serverProduct
                        }
                    }
                    _products.value = interceptedData
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

    fun addProduct(product: Product, imageUri: android.net.Uri? = null, context: android.content.Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            var tempFile: java.io.File? = null
            var optimisticImageUrl: String = ""

            // 1. Prepare Local File if Image Exists (To prevent URI Permission drop)
            if (imageUri != null) {
                tempFile = FileUtils.getFileFromUri(context, imageUri)
                if (tempFile != null) {
                    val fileSizeInBytes = tempFile.length()
                    if (fileSizeInBytes > 5 * 1024 * 1024) {
                        val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                        _errorMessage.value = "Ukuran gambar memakan $fileSizeInMB MB. Maksimal hanya 5MB ya! 📸"
                        _isLoading.value = false
                        tempFile.delete()
                        return@launch
                    }
                    optimisticImageUrl = "file://${tempFile.absolutePath}"
                }
            }

            // 2. OPTIMISTIC LOCAL UI UPDATE (Secure file:// uri)
            val dummyId = -(System.currentTimeMillis().toInt())
            val optimisticProduct = product.copy(id = dummyId, image = optimisticImageUrl)
            val originalProducts = _products.value.toList()
            _products.value = listOf(optimisticProduct) + originalProducts
            
            // TAHAP 63: Save to interceptor map so fetchProducts doesn't overwrite it with Cloudinary URL
            if (optimisticImageUrl.isNotEmpty()) {
                optimisticImagesMap[product.name] = optimisticImageUrl
            }

            try {
                val name = createPartFromString(product.name)
                val categoryId = createPartFromString(product.categoryId?.toString() ?: "0")
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())
                val ar3dModel = if (product.ar3dModel != null) createPartFromString(product.ar3dModel) else null
                val isArActive = createPartFromString(product.isArActive.toString())
                
                var imagePart: okhttp3.MultipartBody.Part? = null
                if (tempFile != null) {
                    val contentResolver = context.contentResolver
                    val type = contentResolver.getType(imageUri!!) ?: "image/jpeg"
                    val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), tempFile)
                    imagePart = okhttp3.MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                }

                val response = RetrofitClient.instance.addProduct(
                    name, categoryId, price, description, imagePart, isActive, ar3dModel, isArActive
                )
                
                if (response.success) {
                    fetchProducts(isSilent = true) // Sync to get new ID gracefully
                    onSuccess() // Callback to close screen
                } else {
                    _products.value = originalProducts // Rollback
                    _errorMessage.value = response.message
                    tempFile?.delete()
                }
            } catch (e: Exception) {
                _products.value = originalProducts // Rollback
                _errorMessage.value = "Gagal menambah produk: ${e.localizedMessage}"
                tempFile?.delete()
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun updateProduct(id: Int, product: Product, imageUri: android.net.Uri? = null, context: android.content.Context? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            var tempFile: java.io.File? = null
            var optimisticImageUrl: String? = null

            // 1. Prepare Local File if Image Passed
            if (imageUri != null && context != null) {
                tempFile = FileUtils.getFileFromUri(context, imageUri)
                if (tempFile != null) {
                    val fileSizeInBytes = tempFile.length()
                    if (fileSizeInBytes > 5 * 1024 * 1024) {
                        val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                        _errorMessage.value = "Ukuran gambar memakan $fileSizeInMB MB. Maksimal hanya 5MB ya! 📸"
                        _isLoading.value = false
                        tempFile.delete()
                        return@launch
                    }
                    optimisticImageUrl = "file://${tempFile.absolutePath}"
                }
            }

            // 2. OPTIMISTIC LOCAL UI UPDATE (Secure file:// uri or old image)
            val originalProducts = _products.value.toList()
            _products.value = originalProducts.map {
                if (it.id == id) product.copy(id = id, image = optimisticImageUrl ?: it.image) else it
            }
            
            // TAHAP 63: Save to interceptor map so fetchProducts doesn't overwrite it with Cloudinary URL
            if (optimisticImageUrl != null) {
                optimisticImagesMap[product.name] = optimisticImageUrl
            }

            try {
                val name = createPartFromString(product.name)
                val categoryId = createPartFromString(product.categoryId?.toString() ?: "0")
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())
                val ar3dModel = if (product.ar3dModel != null) createPartFromString(product.ar3dModel) else null
                val isArActive = createPartFromString(product.isArActive.toString())

                var imagePart: okhttp3.MultipartBody.Part? = null
                if (tempFile != null && context != null) {
                    val contentResolver = context.contentResolver
                    val type = contentResolver.getType(imageUri!!) ?: "image/jpeg"
                    val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), tempFile)
                    imagePart = okhttp3.MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                }

                val response = RetrofitClient.instance.updateProduct(
                    id, name, categoryId, price, description, imagePart, isActive, ar3dModel, isArActive
                )
                
                if (response.success) {
                    fetchProducts(isSilent = true)
                    onSuccess()
                } else {
                    _products.value = originalProducts // Rollback
                    _errorMessage.value = response.message
                    tempFile?.delete()
                }
            } catch (e: Exception) {
                _products.value = originalProducts // Rollback
                _errorMessage.value = "Gagal mengubah produk: ${e.localizedMessage}"
                tempFile?.delete()
            }
        }
    }

    fun toggleProductStatus(product: Product, onComplete: () -> Unit = {}) {
        val updatedProduct = product.copy(isActive = !product.isActive)
        // NO VIEWMODEL CANCEL DEBOUNCE HERE! 
        // The UI's Anti-Spam (Tahap 21) already protects against single-item spam.
        
        toggleJobs[product.id] = viewModelScope.launch {
             pendingTogglesCount.incrementAndGet() // Block socket updates & intermediate refreshes globally
             try {
                // ZERO-BOUNCE OPTIMISTIC UI: Update instantly outside the Mutex queue!
                _products.value = _products.value.map {
                    if (it.id == product.id) updatedProduct else it
                }

                val name = createPartFromString(updatedProduct.name)
                val categoryId = createPartFromString(updatedProduct.categoryId?.toString() ?: "0")
                val price = createPartFromString(updatedProduct.price.toString())
                val description = createPartFromString(updatedProduct.description ?: "")
                val isActive = createPartFromString(updatedProduct.isActive.toString())
                val ar3dModel = if (updatedProduct.ar3dModel != null) createPartFromString(updatedProduct.ar3dModel) else null
                val isArActive = createPartFromString(updatedProduct.isArActive.toString())
                
                // WAIT IN LINE: Execute API requests one-by-one via Mutex
                toggleMutex.withLock {
                    val response = RetrofitClient.instance.updateProduct(
                         updatedProduct.id, name, categoryId, price, description, null, isActive, ar3dModel, isArActive
                    )
                    
                    if (response.success) {
                        fetchProducts(isSilent = true) // Final sync (Gatekeeper protected now)
                    } else {
                        // Backend Error -> Full Server Sync (Better Rollback)
                        fetchProducts(isSilent = false)  
                        _errorMessage.value = response.message
                    }
                }
             } catch (e: Exception) {
                 // Network Error -> Full Server Sync
                 fetchProducts(isSilent = false)
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
