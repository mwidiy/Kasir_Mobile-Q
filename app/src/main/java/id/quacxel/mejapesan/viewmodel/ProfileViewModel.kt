package id.quacxel.mejapesan.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.quacxel.mejapesan.data.model.Store
import id.quacxel.mejapesan.data.model.WithdrawalRequest
import id.quacxel.mejapesan.data.model.Withdrawal
import id.quacxel.mejapesan.data.network.RetrofitClient
import id.quacxel.mejapesan.utils.FileUtils
import id.quacxel.mejapesan.utils.LocalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
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

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    private val _withdrawalHistory = MutableStateFlow<List<Withdrawal>>(emptyList())
    val withdrawalHistory: StateFlow<List<Withdrawal>> = _withdrawalHistory

    // WhatsApp Bot State
    private val _waStatus = MutableStateFlow("disconnected")
    val waStatus: StateFlow<String> = _waStatus

    private val _waQrCode = MutableStateFlow<String?>(null)
    val waQrCode: StateFlow<String?> = _waQrCode

    private val _waPairingCode = MutableStateFlow<String?>(null)
    val waPairingCode: StateFlow<String?> = _waPairingCode

    private val _pairingSuccess = MutableStateFlow(false)
    val pairingSuccess: StateFlow<Boolean> = _pairingSuccess

    // Promotion State
    private val _promotionStats = MutableStateFlow<id.quacxel.mejapesan.data.model.PromotionStats?>(null)
    val promotionStats: StateFlow<id.quacxel.mejapesan.data.model.PromotionStats?> = _promotionStats

    private val _isPromoting = MutableStateFlow(false)
    val isPromoting: StateFlow<Boolean> = _isPromoting

    private val _promotionMessage = MutableStateFlow<String?>(null)
    val promotionMessage: StateFlow<String?> = _promotionMessage

    fun setWaStatus(status: String) {
        val oldStatus = _waStatus.value
        _waStatus.value = status
        if (status == "connected") {
            _waQrCode.value = null
            _waPairingCode.value = null
            
            // TAHAP 40: Trigger Success UX if it was previously disconnected/pairing
            if (oldStatus != "connected") {
                viewModelScope.launch {
                    _pairingSuccess.value = true
                    kotlinx.coroutines.delay(2000)
                    _pairingSuccess.value = false
                }
            }
        }
    }

    fun setWaQrCode(qr: String) {
        _waQrCode.value = qr
    }

    fun clearPairingSuccess() {
        _pairingSuccess.value = false
    }

    fun initWhatsApp() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.initWhatsApp()
                if (!response.isSuccessful) {
                    _errorMessage.value = "Gagal memulai koneksi WhatsApp."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan: ${e.localizedMessage}"
            }
        }
    }

    fun fetchWhatsAppStatus() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getWhatsAppStatus()
                if (response.isSuccessful) {
                    val data = response.body()
                    _waStatus.value = data?.status ?: "disconnected"
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun disconnectWhatsApp() {
        // TAHAP 40: Optimistic UI - Update locally first for "ASEK" speed
        _waStatus.value = "disconnected"
        _waQrCode.value = null
        _waPairingCode.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.disconnectWhatsApp()
                if (!response.isSuccessful) {
                    // Fallback or log if server failed, but usually we keep it disconnected
                    Log.e("ProfileViewModel", "Server failed to disconnect WA: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception disconnecting WA: ${e.message}")
            }
        }
    }

    fun fetchPromotionStats() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getPromotionStats()
                if (response.success) {
                    _promotionStats.value = response.data
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun startPromotion(type: String) {
        viewModelScope.launch {
            _isPromoting.value = true
            _promotionMessage.value = "Memulai promosi..."
            try {
                val response = RetrofitClient.instance.startPromotion(mapOf("type" to type))
                _promotionMessage.value = response.message
                if (response.success) {
                    // Refresh stats after starting
                    fetchPromotionStats()
                }
            } catch (e: Exception) {
                _promotionMessage.value = "Gagal memulai promosi: ${e.localizedMessage}"
            } finally {
                kotlinx.coroutines.delay(3000)
                _isPromoting.value = false
            }
        }
    }

    fun clearPromotionMessage() {
        _promotionMessage.value = null
    }

    init {
        fetchStore()
        fetchBalance()
        fetchHistory()
        fetchWhatsAppStatus()
        fetchPromotionStats()

        // Observe WA Events from LocalEventBus
        viewModelScope.launch {
            LocalEventBus.waQrFlow.collect { qr ->
                _waQrCode.value = qr
            }
        }
        viewModelScope.launch {
            LocalEventBus.waPairingCodeFlow.collect { code ->
                _waPairingCode.value = code
            }
        }
        viewModelScope.launch {
            LocalEventBus.waStatusFlow.collect { status ->
                setWaStatus(status)
            }
        }
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
                    _errorMessage.value = "Waduh, profil kamu belum bisa dimuat nih. Coba cek koneksi sebentar ya."
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
                 val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(name = newName))
                 if (response.success && response.data != null) {
                     // Confirm with server data (usually same)
                     _storeState.value = response.data
                 } else {
                     // Revert on server error
                     _storeState.value = oldState
                      _errorMessage.value = "Maaf, nama resto belum bisa diubah. Coba klik simpan sekali lagi ya."
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
                _errorMessage.value = "Maaf, nama resto belum bisa diubah. Coba cek koneksi internet kamu ya."
            }
        }
    }

    fun updateWhatsApp(number: String) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(whatsappNumber = number)

        viewModelScope.launch {
            try {
                // Ensure number format (strip + or 62 if needed, but backend/PWA handles it usually. Let's just save as is)
                val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(whatsappNumber = number))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Oops, nomor WhatsApp belum tersimpan. Pastikan formatnya benar ya."
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

    fun toggleAutoReply(isEnabled: Boolean) {
        val oldState = _storeState.value
        // TAHAP AI: If auto-reply is turned OFF, AI must also be OFF
        val newAiStatus = if (!isEnabled) false else oldState?.isAiEnabled ?: false
        _storeState.value = oldState?.copy(isAutoReplyEnabled = isEnabled, isAiEnabled = newAiStatus)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(
                    id.quacxel.mejapesan.data.model.StoreUpdateRequest(
                        isAutoReplyEnabled = isEnabled,
                        isAiEnabled = newAiStatus
                    )
                )
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                }
            } catch (e: Exception) {
                _storeState.value = oldState
            }
        }
    }

    fun toggleAi(isEnabled: Boolean) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(isAiEnabled = isEnabled)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(
                    id.quacxel.mejapesan.data.model.StoreUpdateRequest(isAiEnabled = isEnabled)
                )
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                }
            } catch (e: Exception) {
                _storeState.value = oldState
            }
        }
    }

    fun updateKasirQrVerification(isEnabled: Boolean) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(isKasirQrVerificationEnabled = isEnabled)
        id.quacxel.mejapesan.utils.LocalEventBus.emitSettingsUpdate(isEnabled)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(isKasirQrVerificationEnabled = isEnabled))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Maaf, pengaturan verifikasi belum bisa diubah saat ini."
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
                val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(cashPaymentMode = mode))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Maaf, mode pembayaran belum bisa diganti. Silakan coba lagi ya."
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

    fun updateCashPaymentActive(isActive: Boolean) {
        val oldState = _storeState.value
        _storeState.value = oldState?.copy(isCashActive = isActive)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateStore(id.quacxel.mejapesan.data.model.StoreUpdateRequest(isCashActive = isActive))
                if (response.success && response.data != null) {
                    _storeState.value = response.data
                } else {
                    _storeState.value = oldState
                    _errorMessage.value = "Maaf, status pembayaran tunai belum bisa diubah."
                }
            } catch (e: retrofit2.HttpException) {
                _storeState.value = oldState
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    org.json.JSONObject(errorBody!!).getString("error")
                } catch(ex: Exception) { "Gagal update status: ${e.message()}" }
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                _storeState.value = oldState
                _errorMessage.value = "Gagal update status: ${e.localizedMessage}"
            }
        }
    }

    fun updateOrderMethodActive(type: String, isActive: Boolean) {
        val oldState = _storeState.value
        val newState = when (type) {
            "dinein" -> oldState?.copy(isDineInActive = isActive)
            "takeaway" -> oldState?.copy(isTakeawayActive = isActive)
            "delivery" -> oldState?.copy(isDeliveryActive = isActive)
            else -> oldState
        }
        _storeState.value = newState

        viewModelScope.launch {
            try {
                val request = when (type) {
                    "dinein" -> id.quacxel.mejapesan.data.model.StoreUpdateRequest(isDineInActive = isActive)
                    "takeaway" -> id.quacxel.mejapesan.data.model.StoreUpdateRequest(isTakeawayActive = isActive)
                    "delivery" -> id.quacxel.mejapesan.data.model.StoreUpdateRequest(isDeliveryActive = isActive)
                    else -> null
                }
                if (request != null) {
                    val response = RetrofitClient.instance.updateStore(request)
                    if (response.success && response.data != null) {
                        _storeState.value = response.data
                    } else {
                        _storeState.value = oldState
                        _errorMessage.value = "Maaf, pengaturan layanan belum bisa diubah."
                    }
                }
            } catch (e: Exception) {
                _storeState.value = oldState
                _errorMessage.value = "Gagal update layanan: ${e.localizedMessage}"
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
                     id.quacxel.mejapesan.data.model.StoreUpdateRequest(
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
                     _errorMessage.value = "Maaf, pengaturan pembayaran belum tersimpan. Coba cek lagi datanya ya."
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
                _errorMessage.value = "Maaf, simpan pengaturan belum berhasil. Silakan coba lagi sebentar lagi."
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
                        id.quacxel.mejapesan.data.model.WithdrawalRequest(amount, method)
                    )
                    if (response.success) {
                        fetchBalance() // Background sync to ensure precision
                        fetchHistory() // Refresh history silently
                    } else {
                         // Rollback
                         _availableBalance.value = previousAvailable
                         _balance.value = previousBalance
                          _errorMessage.value = "Maaf, penarikan dana belum bisa diproses. Coba beberapa saat lagi ya."
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
                _errorMessage.value = "Maaf, upload logo belum berhasil. Pastikan ukuran file tidak terlalu besar ya."
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

    private val _customSoundPath = MutableStateFlow<String?>(null)
    val customSoundPath: StateFlow<String?> = _customSoundPath

    fun loadCustomSound(context: Context) {
        viewModelScope.launch {
            _customSoundPath.value = id.quacxel.mejapesan.utils.SessionManager.getCustomSoundPath(context).first()
        }
    }

    fun updateCustomSound(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val path = id.quacxel.mejapesan.utils.FileUtils.saveCustomAudio(context, uri)
                if (path != null) {
                    id.quacxel.mejapesan.utils.SessionManager.setCustomSoundPath(context, path)
                    _customSoundPath.value = path
                    
                    // Force refresh notification channel to apply new sound
                    id.quacxel.mejapesan.utils.NotificationUtils.createOrderChannel(context, path)
                    
                    _errorMessage.value = "Nada notifikasi berhasil diubah!"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal mengubah nada notifikasi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetCustomSound(context: Context) {
        viewModelScope.launch {
            id.quacxel.mejapesan.utils.SessionManager.setCustomSoundPath(context, null)
            _customSoundPath.value = null
            id.quacxel.mejapesan.utils.NotificationUtils.createOrderChannel(context, null)
            _errorMessage.value = "Nada notifikasi dikembalikan ke default"
        }
    }

    fun playTestSound(context: Context) {
        viewModelScope.launch {
            val path = _customSoundPath.value
            id.quacxel.mejapesan.utils.NotificationUtils.playOrderSound(context, path)
        }
    }
}
