package com.example.kasir.data.network

import com.example.kasir.data.model.Product
import com.example.kasir.data.model.ProductResponse
import com.example.kasir.data.model.SingleProductResponse
import retrofit2.http.*

interface ApiService {
    @GET("api/products")
    suspend fun getProducts(): ProductResponse

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") id: Int): SingleProductResponse

    @POST("api/products")
    suspend fun addProduct(@Body product: Product): SingleProductResponse

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body product: Product): SingleProductResponse

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): SingleProductResponse
}
