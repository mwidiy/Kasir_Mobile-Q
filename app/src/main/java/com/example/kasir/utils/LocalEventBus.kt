package com.example.kasir.utils

import com.example.kasir.data.model.OrderResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
}
