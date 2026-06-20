package id.quacxel.mejapesan.utils

import id.quacxel.mejapesan.data.model.OrderResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

object LocalEventBus {
    // SharedFlow allows multiple subscribers, replay = 0 means it only emits to active subscribers
    private val _orderUpdateFlow = MutableSharedFlow<OrderResponse>(replay = 0, extraBufferCapacity = 1)
    val orderUpdateFlow = _orderUpdateFlow.asSharedFlow()

    private val _settingsUpdateFlow = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)
    val settingsUpdateFlow = _settingsUpdateFlow.asSharedFlow()

    suspend fun emitOrderUpdate(order: OrderResponse) {
        _orderUpdateFlow.emit(order)
    }

    fun emitSettingsUpdate(isEnabled: Boolean) {
        _settingsUpdateFlow.tryEmit(isEnabled)
    }

    private val _waQrFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val waQrFlow = _waQrFlow.asSharedFlow()

    private val _waPairingCodeFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val waPairingCodeFlow = _waPairingCodeFlow.asSharedFlow()

    private val _waStatusFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val waStatusFlow = _waStatusFlow.asSharedFlow()

    fun emitWaQr(qr: String) {
        _waQrFlow.tryEmit(qr)
    }

    fun emitWaPairingCode(code: String) {
        _waPairingCodeFlow.tryEmit(code)
    }

    fun emitWaStatus(status: String) {
        _waStatusFlow.tryEmit(status)
    }
}
