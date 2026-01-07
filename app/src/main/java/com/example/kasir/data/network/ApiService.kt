package com.example.kasir.data.network

import com.example.kasir.data.model.Product
import com.example.kasir.data.model.ProductResponse
import com.example.kasir.data.model.SingleProductResponse
import retrofit2.http.*
import com.example.kasir.data.model.Banner
import com.example.kasir.data.model.ApiResponse
import com.example.kasir.data.model.Location
import com.example.kasir.data.model.LocationData
import com.example.kasir.data.model.LocationResponse
import com.example.kasir.data.model.SingleLocationResponse
import com.example.kasir.data.model.Table
import com.example.kasir.data.model.TableResponse
import com.example.kasir.data.model.StoreResponse
import com.example.kasir.data.model.SingleTableResponse
import com.example.kasir.data.model.TableRequest
import com.example.kasir.data.model.Order
import com.example.kasir.data.model.OrderResponse
import com.example.kasir.data.model.OrderStatusRequest
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

    // Location Endpoints
    @GET("api/locations")
    suspend fun getLocations(): List<Location>

    @POST("api/locations")
    suspend fun addLocation(@Body location: Map<String, String>): LocationData

    @PUT("api/locations/{id}")
    suspend fun updateLocation(@Path("id") id: Int, @Body location: Map<String, String>): LocationData

    @DELETE("api/locations/{id}")
    suspend fun deleteLocation(@Path("id") id: Int): Response<Unit>

    // Table Endpoints
    @GET("api/tables")
    suspend fun getTables(): List<Table>

    @POST("api/tables")
    suspend fun addTable(@Body request: TableRequest): Response<Table>

    @PUT("api/tables/{id}")
    suspend fun updateTable(@Path("id") id: Int, @Body request: TableRequest): Response<Table>

    // Specific endpoint for toggling status (isActive)
    @PATCH("api/tables/{id}/status")
    suspend fun updateTableStatus(@Path("id") id: Int, @Body status: Map<String, Boolean>): Response<Table>

    @DELETE("api/tables/{id}")
    suspend fun deleteTable(@Path("id") id: Int): Response<Unit>

    @GET("api/orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<com.example.kasir.data.model.OrderListResponse>

    @PUT("api/orders/{id}/status")
    suspend fun updateOrderStatus(@Path("id") id: Int, @Body status: OrderStatusRequest): Response<Any>

    // Store Endpoints
    @GET("api/store")
    suspend fun getStore(): StoreResponse

    @PUT("api/store")
    suspend fun updateStore(@Body store: Map<String, String>): StoreResponse

    @Multipart
    @POST("api/store/upload-logo")
    suspend fun uploadLogo(@Part image: MultipartBody.Part): StoreResponse

    @Multipart
    @POST("api/store/upload-qris")
    suspend fun uploadQris(@Part image: MultipartBody.Part): StoreResponse
}
