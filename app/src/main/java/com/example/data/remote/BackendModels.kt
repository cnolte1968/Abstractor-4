package com.example.data.remote

import com.example.domain.model.DomainSummary
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @param:Json(name = "username") val username: String,
    @param:Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @param:Json(name = "username") val username: String,
    @param:Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @param:Json(name = "token") val token: String,
    @param:Json(name = "userId") val userId: String,
    @param:Json(name = "username") val username: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "username") val username: String
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    @param:Json(name = "analyses") val analyses: List<DomainSummary>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "synced_ids") val syncedIds: List<String>
)
