package com.example.data.remote

import com.example.domain.model.DomainSummary
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface BackendApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @POST("analysis")
    suspend fun createAnalysis(@Body summary: DomainSummary): Response<DomainSummary>

    @GET("analysis/{id}")
    suspend fun getAnalysis(@Path("id") id: String): Response<DomainSummary>

    @GET("analysis/user/{userId}")
    suspend fun getUserAnalyses(@Path("userId") userId: String): Response<List<DomainSummary>>

    @DELETE("analysis/{id}")
    suspend fun deleteAnalysis(@Path("id") id: String): Response<Unit>

    @POST("sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<SyncResponse>

    @GET("sync/pull")
    suspend fun syncPull(): Response<List<DomainSummary>>

    companion object {
        private var activeToken: String? = null
        private var customBaseUrl: String = "https://abstractor-backend.fly.dev/api/"

        fun setToken(token: String?) {
            activeToken = token
        }

        fun setBaseUrl(url: String) {
            var formattedUrl = url.trim()
            if (!formattedUrl.endsWith("/")) formattedUrl += "/"
            customBaseUrl = formattedUrl
        }

        fun getBaseUrl(): String = customBaseUrl

        fun create(): BackendApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    activeToken?.let { token ->
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .addInterceptor(logging)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val url = if (customBaseUrl.startsWith("http")) customBaseUrl else "http://10.0.2.2:8080/api/"

            return Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(com.example.data.SummaryResponseParser.moshiInstance))
                .build()
                .create(BackendApiService::class.java)
        }
    }
}
