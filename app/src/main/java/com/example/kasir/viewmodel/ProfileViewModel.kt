package com.example.kasir.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasir.data.model.Store
import com.example.kasir.data.model.WithdrawalRequest
import com.example.kasir.data.model.Withdrawal
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val withdrawMutex = Mutex() // TAHAP 34: Anti-Double Click Guard

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _withdrawalHistory = MutableStateFlow<List<Withdrawal>>(emptyList())
    val withdrawalHistory: StateFlow<List<Withdrawal>> = _withdrawalHistory

    init {
        fetchStore()
        fetchBalance()
        fetchHistory()
    }

    fun fetchHistory() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getWithdrawalHistory()
                if (response.success) {
                    _withdrawalHistory.value = response.data
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun fetchStore() {
        viewModelScope.launch {
            // Only show full loading if we have no data yet (Skeleton/Spinner)
            // If we have data, we do "Silent Refresh" (background update)
            if (_storeState.value == null) {
                _isLoading.value = true
                kotlinx.coroutines.delay(400) // TAHAP 34: Guarantee skeleton visibility on fast networks
            }
            
            try {
                val response = RetrofitClient.instance.getStore()
                if (response.success) {
                    _storeState.value = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Only show error if we have no data to show
                if (_storeState.value == null) {
                    _errorMessage.value = "Gagal memuat profil: ${e.localizedMessage}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(newName: String) {
        val oldState = _storeState.value
        // Optimistic Update: Apply change immediately
        _storeState.value = oldState?.copy(name = newName)

        viewModelScope.launch {
            // No loading spinner for "instant" feel
            try {
                 val response = RetrofitClient.instance.updateStore(com.example.kasir.data.model.StoreUpdateRequest(name = newName))
                 if (response.success && response.data != null) {
                     // Confirm with server data (usually same)
                     _storeState.value = response.data
                 } else {
                     // Revert on server error
                     _storeState.value = oldState
                     _errorMessage.value = "Gagal update nama"
                 }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal update nama: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                // Revert on network error
                _storeState.value = oldState
                _errorMessage.value = "Gagal update nama: ${e.localizedMessage}"
            }
        }
    }

    fun updateWhatsApp(number: String) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(whatsappNumber = number)

        viewModelScope.launch {
            try {
                // Ensure number format (strip + or 62 if needed, but backend/PWA handles it usually. Let's just save as is)
                val response = RetrofitClient.instance.updateStore(com.example.kasir.data.model.StoreUpdateRequest(whatsappNumber = number))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Gagal update WhatsApp"
                }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal update WhatsApp: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                _storeState.value = oldState
                _errorMessage.value = "Gagal update WhatsApp: ${e.localizedMessage}"
            }
        }
    }

    fun updateKasirQrVerification(isEnabled: Boolean) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(isKasirQrVerificationEnabled = isEnabled)
        com.example.kasir.utils.LocalEventBus.emitSettingsUpdate(isEnabled)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(com.example.kasir.data.model.StoreUpdateRequest(isKasirQrVerificationEnabled = isEnabled))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Gagal update pengaturan verifikasi Kasir"
                }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal update pengaturan: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                _storeState.value = oldState
                _errorMessage.value = "Gagal update pengaturan: ${e.localizedMessage}"
            }
        }
    }

    fun updateCashPaymentMode(mode: String) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(cashPaymentMode = mode)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(com.example.kasir.data.model.StoreUpdateRequest(cashPaymentMode = mode))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Gagal update mode pembayaran"
                }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal update mode: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                _storeState.value = oldState
                _errorMessage.value = "Gagal update mode: ${e.localizedMessage}"
            }
        }
    }

    private val _balance = MutableStateFlow<Int>(0)
    val balance: StateFlow<Int> = _balance

    private val _availableBalance = MutableStateFlow<Int>(0)
    val availableBalance: StateFlow<Int> = _availableBalance

    private val _pendingSettlement = MutableStateFlow<Int>(0)
    val pendingSettlement: StateFlow<Int> = _pendingSettlement

    fun fetchBalance() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getBalance()
                if (response.success) {
                    _balance.value = response.balance
                    _availableBalance.value = response.availableBalance
                    _pendingSettlement.value = response.pendingSettlement
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun updatePaymentSettings(
        bankName: String?, 
        bankNumber: String?, 
        bankHolder: String?,
        ewalletType: String?,
        ewalletNumber: String?,
        ewalletName: String?,
        isDelete: Boolean = false
    ) {
         val oldState = _storeState.value
         
         // Optimistic Update: Construct new state locally
         // Note: handling nulls carefully to match Store model
         val newState = oldState?.copy(
             bankName = bankName,
             bankNumber = bankNumber,
             bankHolder = bankHolder,
             ewalletType = ewalletType,
             ewalletNumber = ewalletNumber,
             ewalletName = ewalletName
         )
         _storeState.value = newState
         
         if (!isDelete) {
            _errorMessage.value = "Pengaturan pembayaran disimpan"
         }

         viewModelScope.launch {
            // No loading spinner
            try {
                 val response = RetrofitClient.instance.updateStore(
                     com.example.kasir.data.model.StoreUpdateRequest(
                         bankName = bankName,
                         bankNumber = bankNumber,
                         bankHolder = bankHolder,
                         ewalletType = ewalletType, // e.g. "ShopeePay"
                         ewalletNumber = ewalletNumber,
                         ewalletName = ewalletName
                     )
                 )
                 if (response.success && response.data != null) {
                     _storeState.value = response.data
                 } else {
                     // Revert
                     _storeState.value = oldState
                     _errorMessage.value = "Gagal simpan pengaturan"
                 }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal simpan pengaturan: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                // Revert
                _storeState.value = oldState
                _errorMessage.value = "Gagal simpan pengaturan: ${e.localizedMessage}"
            }
        }
    }

    fun requestWithdrawal(amount: Int, method: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (!withdrawMutex.tryLock()) return@launch // TAHAP 34: Reject spam clicks instantly (Anti-Double Click)
            
            try {
                _isLoading.value = true
                
                // OPTIMISTIC UPDATE: Instant Saldo Deduction (Zero-Latency for Cashier)
                val previousAvailable = _availableBalance.value
                val previousBalance = _balance.value
                
                _availableBalance.value -= amount
                _balance.value -= amount
                
                onSuccess() // TAHAP 34: Close dialog IMMEDIATELY (0ms latency UI)
                
                try {
                    val response = RetrofitClient.instance.requestWithdrawal(
                        com.example.kasir.data.model.WithdrawalRequest(amount, method)
                    )
                    if (response.success) {
                        fetchBalance() // Background sync to ensure precision
                        fetchHistory() // Refresh history silently
                    } else {
                         // Rollback
                         _availableBalance.value = previousAvailable
                         _balance.value = previousBalance
                         _errorMessage.value = "Gagal tarik dana"
                    }
                } catch (e: retrofit2.HttpException) {
                    // Rollback
                    _availableBalance.value = previousAvailable
                    _balance.value = previousBalance
                    val errorMsg = try {
                        val errorBody = e.response()?.errorBody()?.string()
                        org.json.JSONObject(errorBody!!).getString("error")
                    } catch(ex: Exception) { "Gagal tarik dana: ${e.message()}" }
                    _errorMessage.value = errorMsg
                } catch (e: Exception) {
                    // Rollback
                    _availableBalance.value = previousAvailable
                    _balance.value = previousBalance
                    _errorMessage.value = "Gagal tarik dana: ${e.localizedMessage}"
                } finally {
                    _isLoading.value = false
                }
            } finally {
                withdrawMutex.unlock()
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
            } catch (e: retrofit2.HttpException) {
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal upload logo: ${e.message()}" }
                _errorMessage.value = errorMsg
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
            } catch (e: retrofit2.HttpException) {
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal upload QRIS: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                _errorMessage.value = "Gagal upload QRIS: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
