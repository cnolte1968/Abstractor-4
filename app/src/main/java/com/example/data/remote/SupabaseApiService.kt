package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SystemStatusDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "backend_version") val backendVersion: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class EdgeFunctionHealthDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "version") val version: String? = null
)

interface SupabaseApiService {
    @GET("rest/v1/system_status")
    suspend fun getSystemStatus(
        @Query("select") select: String = "*"
    ): Response<List<SystemStatusDto>>

    @GET("functions/v1/health-check")
    suspend fun checkEdgeFunctionHealth(): Response<EdgeFunctionHealthDto>

    companion object {
        fun create(
            baseUrl: String = BuildConfig.SUPABASE_URL,
            publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ): SupabaseApiService {
            val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val headerInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("apikey", publishableKey)
                    .build()
                chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(headerInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(SupabaseApiService::class.java)
        }
    }
}
