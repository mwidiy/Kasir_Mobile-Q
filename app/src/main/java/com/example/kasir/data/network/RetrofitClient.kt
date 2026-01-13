package com.example.kasir.data.network

import retrofit2.Retrofit
import com.example.kasir.BuildConfig
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    const val BASE_URL = BuildConfig.API_BASE_URL
    
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
