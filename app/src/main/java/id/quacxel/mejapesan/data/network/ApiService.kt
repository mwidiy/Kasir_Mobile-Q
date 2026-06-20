package id.quacxel.mejapesan.data.network

import id.quacxel.mejapesan.data.model.Product
import id.quacxel.mejapesan.data.model.ProductResponse
import id.quacxel.mejapesan.data.model.SingleProductResponse
import retrofit2.http.*
import id.quacxel.mejapesan.data.model.Banner
import id.quacxel.mejapesan.data.model.ApiResponse
import id.quacxel.mejapesan.data.model.Location
import id.quacxel.mejapesan.data.model.LocationData
import id.quacxel.mejapesan.data.model.LocationResponse
import id.quacxel.mejapesan.data.model.SingleLocationResponse
import id.quacxel.mejapesan.data.model.Table
import id.quacxel.mejapesan.data.model.TableResponse
import id.quacxel.mejapesan.data.model.StoreResponse
import id.quacxel.mejapesan.data.model.SingleTableResponse
import id.quacxel.mejapesan.data.model.TableRequest
import id.quacxel.mejapesan.data.model.Order
import id.quacxel.mejapesan.data.model.OrderResponse
import id.quacxel.mejapesan.data.model.OrderStatusRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

interface ApiService {
    @GET("api/products")
    suspend fun getProducts(): ProductResponse

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") id: Int): SingleProductResponse

    @GET("api/categories")
    suspend fun getCategories(): id.quacxel.mejapesan.data.model.CategoryResponse

    @POST("api/categories")
    suspend fun addCategory(@Body category: Map<String, @JvmSuppressWildcards Any>): id.quacxel.mejapesan.data.model.ApiResponse<id.quacxel.mejapesan.data.model.Category>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body category: Map<String, @JvmSuppressWildcards Any>): id.quacxel.mejapesan.data.model.ApiResponse<id.quacxel.mejapesan.data.model.Category>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): id.quacxel.mejapesan.data.model.ApiResponse<Any?>

    @GET("api/ar/assets")
    suspend fun getArAssets(): id.quacxel.mejapesan.data.model.ArAssetResponse

    @Multipart
    @POST("api/ar/upload")
    suspend fun uploadArAsset(
        @Part model: MultipartBody.Part
    ): id.quacxel.mejapesan.data.model.SingleArAssetResponse

    @DELETE("api/ar/delete/{id}")
    suspend fun deleteArAsset(
        @Path("id") id: Int
    ): ApiResponse<Void>

    @Multipart
    @POST("api/products")
    suspend fun addProduct(
        @Part("name") name: okhttp3.RequestBody,
        @Part("categoryId") categoryId: okhttp3.RequestBody,
        @Part("price") price: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?,
        @Part("isActive") isActive: okhttp3.RequestBody,
        @Part("ar3dModel") ar3dModel: okhttp3.RequestBody?,
        @Part("isArActive") isArActive: okhttp3.RequestBody?
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
        @Part("isActive") isActive: okhttp3.RequestBody,
        @Part("ar3dModel") ar3dModel: okhttp3.RequestBody?,
        @Part("isArActive") isArActive: okhttp3.RequestBody?
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
    suspend fun getOrders(@Query("status") status: String? = null): Response<id.quacxel.mejapesan.data.model.OrderListResponse>

    @PUT("api/orders/{id}/status")
    suspend fun updateOrderStatus(@Path("id") id: Int, @Body status: OrderStatusRequest): Response<Any>

    // Store Endpoints
    @GET("api/store")
    suspend fun getStore(): StoreResponse

    @PUT("api/store")
    suspend fun updateStore(@Body store: id.quacxel.mejapesan.data.model.StoreUpdateRequest): StoreResponse

    @Multipart
    @POST("api/store/upload-logo")
    suspend fun uploadLogo(@Part image: MultipartBody.Part): StoreResponse

    @Multipart
    @POST("api/store/upload-qris")
    suspend fun uploadQris(@Part image: MultipartBody.Part): StoreResponse

    @GET("api/orders/code/{code}")
    suspend fun getOrderByCode(@Path("code") code: String): Response<id.quacxel.mejapesan.data.model.SingleOrderResponse>

    // Cancellation & Refund
    @PUT("api/orders/{id}/cancel-approve")
    suspend fun approveCancel(@Path("id") id: Int): Response<Any>

    @PUT("api/orders/{id}/cancel-reject")
    suspend fun rejectCancel(@Path("id") id: Int, @Body body: Map<String, String>): Response<Any>

    @POST("api/orders/refund-verify")
    suspend fun verifyRefund(@Body body: Map<String, String>): Response<id.quacxel.mejapesan.data.model.RefundResponse>

    // Auth
    @POST("api/auth/google-login")
    suspend fun googleLogin(@Body request: id.quacxel.mejapesan.data.model.LoginRequest): Response<id.quacxel.mejapesan.data.model.LoginResponse>

    // Withdrawal
    @GET("api/withdraw/balance")
    suspend fun getBalance(): id.quacxel.mejapesan.data.model.BalanceResponse

    @POST("api/withdraw/request")
    suspend fun requestWithdrawal(@Body request: id.quacxel.mejapesan.data.model.WithdrawalRequest): id.quacxel.mejapesan.data.model.WithdrawalResponse
    
    @GET("api/withdraw/history")
    suspend fun getWithdrawalHistory(): id.quacxel.mejapesan.data.model.WithdrawalHistoryResponse

    @POST("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Any>

    // WhatsApp Bot
    @POST("api/whatsapp/init")
    suspend fun initWhatsApp(@Body body: Map<String, String>): Response<id.quacxel.mejapesan.data.model.ApiResponse<String>>

    @GET("api/whatsapp/status")
    suspend fun getWhatsAppStatus(): Response<id.quacxel.mejapesan.data.model.WhatsAppStatusResponse>

    @POST("api/whatsapp/disconnect")
    suspend fun disconnectWhatsApp(): Response<id.quacxel.mejapesan.data.model.ApiResponse<String>>

    // Promotion
    @GET("api/whatsapp/promotion/stats")
    suspend fun getPromotionStats(): id.quacxel.mejapesan.data.model.PromotionStatsResponse

    @POST("api/whatsapp/promotion/start")
    suspend fun startPromotion(@Body body: Map<String, String>): id.quacxel.mejapesan.data.model.PromotionResponse
}

data class FcmTokenRequest(
    val userId: String,
    val fcmToken: String
)
