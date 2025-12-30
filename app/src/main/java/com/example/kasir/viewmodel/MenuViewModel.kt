package com.example.kasir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Product
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType

class MenuViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Initialize Socket
        com.example.kasir.utils.SocketHandler.setSocket()
        com.example.kasir.utils.SocketHandler.establishConnection()
        
        val mSocket = com.example.kasir.utils.SocketHandler.getSocket()
        mSocket.on("products_updated") {
            // Trigger fetch in silent mode
            fetchProducts(isSilent = true)
        }

        fetchProducts()
    }

    override fun onCleared() {
        super.onCleared()
        com.example.kasir.utils.SocketHandler.closeConnection()
    }

    fun fetchProducts(isSilent: Boolean = false) {
        viewModelScope.launch {
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


    private fun createPartFromString(stringData: String): okhttp3.RequestBody {
        return okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), stringData)
    }

    fun addProduct(product: Product, imageUri: android.net.Uri? = null, context: android.content.Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val name = createPartFromString(product.name)
                val category = createPartFromString(product.category)
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())
                
                var imagePart: okhttp3.MultipartBody.Part? = null
                if (imageUri != null) {
                    val file = FileUtils.getFileFromUri(context, imageUri)
                    if (file != null) {
                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(imageUri) ?: "image/jpeg"
                        val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                val response = RetrofitClient.instance.addProduct(
                    name, category, price, description, imagePart, isActive
                )
                
                if (response.success) {
                    fetchProducts()
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah produk: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProduct(id: Int, product: Product, imageUri: android.net.Uri? = null, context: android.content.Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val name = createPartFromString(product.name)
                val category = createPartFromString(product.category)
                val price = createPartFromString(product.price.toString())
                val description = createPartFromString(product.description ?: "")
                val isActive = createPartFromString(product.isActive.toString())

                var imagePart: okhttp3.MultipartBody.Part? = null
                if (imageUri != null && context != null) {
                    val file = FileUtils.getFileFromUri(context, imageUri)
                    if (file != null) {
                        val contentResolver = context.contentResolver
                        val type = contentResolver.getType(imageUri) ?: "image/jpeg"
                        val requestFile = okhttp3.RequestBody.create(type.toMediaTypeOrNull(), file)
                        imagePart = okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                val response = RetrofitClient.instance.updateProduct(
                    id, name, category, price, description, imagePart, isActive
                )
                
                if (response.success) {
                    fetchProducts()
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengupdate produk: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleProductStatus(product: Product) {
        val updatedProduct = product.copy(isActive = !product.isActive)
        // For toggle, we don't change image, so pass null context/uri
        // But we need to use the new signature. The function handles nulls gracefully.
        // We do basic update without file.
        viewModelScope.launch {
             try {
                val name = createPartFromString(updatedProduct.name)
                val category = createPartFromString(updatedProduct.category)
                val price = createPartFromString(updatedProduct.price.toString())
                val description = createPartFromString(updatedProduct.description ?: "")
                val isActive = createPartFromString(updatedProduct.isActive.toString())
                
                RetrofitClient.instance.updateProduct(
                     updatedProduct.id, name, category, price, description, null, isActive
                )
                fetchProducts(isSilent = true)
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.instance.deleteProduct(id)
                if (response.success) {
                    fetchProducts()
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus produk: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
