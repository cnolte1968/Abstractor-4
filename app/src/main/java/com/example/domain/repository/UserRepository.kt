package com.example.domain.repository

interface UserRepository {
    suspend fun login(username: String, password: String): Boolean
    suspend fun register(username: String, password: String): Boolean
    suspend fun logout()
    suspend fun getActiveUsername(): String?
    suspend fun getActiveToken(): String?
}
