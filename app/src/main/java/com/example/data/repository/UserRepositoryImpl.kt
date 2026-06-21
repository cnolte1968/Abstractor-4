package com.example.data.repository

import com.example.data.local.AbstractorDatabase
import com.example.data.local.UserCacheEntity
import com.example.data.remote.BackendApiService
import com.example.data.remote.LoginRequest
import com.example.data.remote.RegisterRequest
import com.example.domain.repository.UserRepository

class UserRepositoryImpl(
    private val database: AbstractorDatabase,
    private val apiService: BackendApiService
) : UserRepository {

    private val userCacheDao = database.userCacheDao()

    override suspend fun login(username: String, password: String): Boolean {
        if (!com.example.data.BackendFeatureConfig.authEnabled) {
            return false
        }
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Set the token inside the API service for authorization headers
                BackendApiService.setToken(data.token)
                
                // Save session in local Room cache
                userCacheDao.insertUser(
                    UserCacheEntity(
                        id = data.userId,
                        username = data.username,
                        token = data.token,
                        isActive = true
                    )
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun register(username: String, password: String): Boolean {
        if (!com.example.data.BackendFeatureConfig.authEnabled) {
            return false
        }
        return try {
            val response = apiService.register(RegisterRequest(username, password))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun logout() {
        BackendApiService.setToken(null)
        userCacheDao.clearUser()
    }

    override suspend fun getActiveUsername(): String? {
        val user = userCacheDao.getActiveUser()
        return user?.username
    }

    override suspend fun getActiveToken(): String? {
        val user = userCacheDao.getActiveUser()
        return user?.token
    }
}
