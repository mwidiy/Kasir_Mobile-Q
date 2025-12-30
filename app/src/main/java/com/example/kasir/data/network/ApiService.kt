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

    @Multipart
    @POST("api/products")
    suspend fun addProduct(
        @Part("name") name: okhttp3.RequestBody,
        @Part("category") category: okhttp3.RequestBody,
        @Part("price") price: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?,
        @Part("isActive") isActive: okhttp3.RequestBody
    ): SingleProductResponse

    @Multipart
    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Part("name") name: okhttp3.RequestBody,
        @Part("category") category: okhttp3.RequestBody,
        @Part("price") price: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?,
        @Part("isActive") isActive: okhttp3.RequestBody
    ): SingleProductResponse

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): SingleProductResponse
}
