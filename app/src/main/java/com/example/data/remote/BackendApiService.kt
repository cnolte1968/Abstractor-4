package com.example.data.remote

import com.example.domain.model.DomainSummary
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
    val username_or_email: String = username
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val username: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val username: String,
    val email: String? = null,
    val token: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    val analyses: List<DomainSummary>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val status: String,
    val count: Int
)

interface BackendApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @POST("api/analyses")
    suspend fun createAnalysis(@Body summary: DomainSummary): Response<DomainSummary>

    @GET("api/analyses/{id}")
    suspend fun getAnalysis(@Path("id") id: String): Response<DomainSummary>

    @GET("api/users/{userId}/analyses")
    suspend fun getUserAnalyses(@Path("userId") userId: String): Response<List<DomainSummary>>

    @DELETE("api/analyses/{id}")
    suspend fun deleteAnalysis(@Path("id") id: String): Response<Unit>

    @POST("api/sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<SyncResponse>

    @GET("api/sync/pull")
    suspend fun syncPull(): Response<List<DomainSummary>>

    companion object {
        fun create(): BackendApiService {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://relevantor-backend.example.com/")
                .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
                .build()
            return retrofit.create(BackendApiService::class.java)
        }
    }
}
