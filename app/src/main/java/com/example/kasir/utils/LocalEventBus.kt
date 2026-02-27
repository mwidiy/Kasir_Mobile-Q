package com.example.kasir.utils

import com.example.kasir.data.model.OrderResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LocalEventBus {
    // SharedFlow allows multiple subscribers, replay = 0 means it only emits to active subscribers
    private val _orderUpdateFlow = MutableSharedFlow<OrderResponse>(replay = 0, extraBufferCapacity = 1)
    val orderUpdateFlow = _orderUpdateFlow.asSharedFlow()

    suspend fun emitOrderUpdate(order: OrderResponse) {
        _orderUpdateFlow.emit(order)
    }
}
