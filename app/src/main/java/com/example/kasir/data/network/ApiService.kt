package com.example.kasir.data.network

import com.example.kasir.data.model.ProductResponse
import retrofit2.http.GET

interface ApiService {
    @GET("api/products")
    suspend fun getProducts(): ProductResponse
}
