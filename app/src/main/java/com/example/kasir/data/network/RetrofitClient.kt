package com.example.kasir.data.network

import retrofit2.Retrofit
import com.example.kasir.BuildConfig
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = BuildConfig.API_BASE_URL
    
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        
        com.example.kasir.utils.SessionManager.jwtToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        val request = requestBuilder.build()
        chain.proceed(request)
    }

    // TAHAP 64: Smart Auto-Retry Interceptor for Koyeb Cold Start (503 Service Unavailable)
    private val retryInterceptor = Interceptor { chain ->
        val original = chain.request()
        var response: okhttp3.Response? = null
        var responseOK = false
        var tryCount = 0
        val maxLimit = 5

        while (!responseOK && tryCount < maxLimit) {
            try {
                response = chain.proceed(original)
                // If got 502, 503, 504 -> Koyeb is starting up the instance. Wait and retry.
                if (response.code == 502 || response.code == 503 || response.code == 504) {
                    response.close()
                    tryCount++
                    // Backoff delay: 2s, 4s, 6s, 8s, 10s
                    Thread.sleep((tryCount * 2000).toLong())
                    continue
                }
                responseOK = response.isSuccessful || response.code < 500
            } catch (e: Exception) {
                // If it's a SocketTimeoutException or network drop, retry as well
                tryCount++
                if (tryCount >= maxLimit) {
                    throw e
                }
                Thread.sleep((tryCount * 2000).toLong())
            }
        }
        
        // Return the final response (or proceed one last time if response is somehow null to let it throw)
        response ?: chain.proceed(original)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor) // Inject the smart retry before timeout logic
        .connectTimeout(60, TimeUnit.SECONDS) // Long timeout for mass-toggle safety
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
