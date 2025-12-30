package com.example.kasir.data.network

import com.example.kasir.data.model.Product
import com.example.kasir.data.model.ProductResponse
import com.example.kasir.data.model.SingleProductResponse
import retrofit2.http.*
import com.example.kasir.data.model.Banner
import com.example.kasir.data.model.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

interface ApiService {
    @GET("api/products")
    suspend fun getProducts(): ProductResponse

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") id: Int): SingleProductResponse

    @GET("api/categories")
    suspend fun getCategories(): com.example.kasir.data.model.CategoryResponse

    @POST("api/categories")
    suspend fun addCategory(@Body category: Map<String, String>): com.example.kasir.data.model.SingleCategoryResponse

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body category: Map<String, String>): com.example.kasir.data.model.SingleCategoryResponse

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): com.example.kasir.data.model.SingleCategoryResponse

    @Multipart
    @POST("api/products")
    suspend fun addProduct(
        @Part("name") name: okhttp3.RequestBody,
        @Part("categoryId") categoryId: okhttp3.RequestBody,
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
        @Part("categoryId") categoryId: okhttp3.RequestBody,
        @Part("price") price: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?,
        @Part("isActive") isActive: okhttp3.RequestBody
    ): SingleProductResponse

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): SingleProductResponse

    // Banner Endpoints
    @GET("api/banners")
    suspend fun getBanners(): Response<ApiResponse<List<Banner>>>

    @DELETE("api/banners/{id}")
    suspend fun deleteBanner(@Path("id") id: Int): Response<ApiResponse<Any>>

    @Multipart
    @POST("api/banners")
    suspend fun addBanner(
        @Part("title") title: RequestBody,
        @Part("subtitle") subtitle: RequestBody?,
        @Part("highlightText") highlightText: RequestBody?,
        @Part image: MultipartBody.Part,
        @Part("isActive") isActive: RequestBody
    ): Response<ApiResponse<Banner>>

    @Multipart
    @PUT("api/banners/{id}")
    suspend fun updateBanner(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part("subtitle") subtitle: RequestBody?,
        @Part("highlightText") highlightText: RequestBody?,
        @Part image: MultipartBody.Part?,
        @Part("isActive") isActive: RequestBody
    ): Response<ApiResponse<Banner>>
}
