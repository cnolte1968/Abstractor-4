package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionStorage
import com.example.data.remote.BackendApiService
import com.example.data.remote.LoginRequest
import com.example.data.remote.RegisterRequest
import com.example.domain.repository.UserRepository

class UserRepositoryImpl(
    private val context: Context,
    private val api: BackendApiService
) : UserRepository {

    override suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                SessionStorage.saveSession(context, username, body.token)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun register(username: String, password: String): Boolean {
        return try {
            val response = api.register(RegisterRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                SessionStorage.saveSession(context, username, body.token ?: "")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getActiveUsername(): String? {
        return SessionStorage.getActiveUsername(context)
    }

    override suspend fun getActiveToken(): String? {
        return SessionStorage.getActiveToken(context)
    }
}
