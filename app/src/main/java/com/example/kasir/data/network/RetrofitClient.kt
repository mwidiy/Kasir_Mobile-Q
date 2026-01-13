package com.example.kasir.data.network

import retrofit2.Retrofit
import com.example.kasir.BuildConfig
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor

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

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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
