package com.example.data.remote

import android.util.Log

data class SystemStatusResult(
    val isReachable: Boolean,
    val httpCode: Int? = null,
    val status: String? = null,
    val backendVersion: String? = null,
    val isPass: Boolean = false,
    val message: String
)

data class EdgeFunctionStatusResult(
    val isReachable: Boolean,
    val httpCode: Int? = null,
    val status: String? = null,
    val version: String? = null,
    val message: String
)

class SupabaseSystemStatusChecker(
    private val apiService: SupabaseApiService = SupabaseApiService.create()
) {
    suspend fun checkEdgeFunctionStatus(): EdgeFunctionStatusResult {
        return try {
            val response = apiService.checkEdgeFunctionHealth()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    EdgeFunctionStatusResult(
                        isReachable = true,
                        httpCode = response.code(),
                        status = dto.status,
                        version = dto.version,
                        message = "PASS (status=${dto.status}, message=${dto.message}, version=${dto.version})"
                    )
                } else {
                    EdgeFunctionStatusResult(
                        isReachable = true,
                        httpCode = response.code(),
                        message = "FAIL (Empty response body)"
                    )
                }
            } else {
                EdgeFunctionStatusResult(
                    isReachable = false,
                    httpCode = response.code(),
                    message = "HTTP ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.w("SupabaseStatusCheck", "Error checking edge function: ${e.message}")
            EdgeFunctionStatusResult(
                isReachable = false,
                message = "Exception: ${e.message}"
            )
        }
    }

    suspend fun checkStatus(): SystemStatusResult {
        return try {
            val response = apiService.getSystemStatus()
            if (response.isSuccessful) {
                val list = response.body()
                val firstRecord = list?.firstOrNull()
                if (firstRecord != null) {
                    val statusVal = firstRecord.status
                    val versionVal = firstRecord.backendVersion
                    val pass = (statusVal == "online" && versionVal == "1")
                    SystemStatusResult(
                        isReachable = true,
                        httpCode = response.code(),
                        status = statusVal,
                        backendVersion = versionVal,
                        isPass = pass,
                        message = if (pass) "PASS (status=$statusVal, version=$versionVal)" else "FAIL (status=$statusVal, version=$versionVal)"
                    )
                } else {
                    SystemStatusResult(
                        isReachable = true,
                        httpCode = response.code(),
                        isPass = false,
                        message = "FAIL (Empty record list)"
                    )
                }
            } else {
                SystemStatusResult(
                    isReachable = false,
                    httpCode = response.code(),
                    isPass = false,
                    message = "HTTP ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.w("SupabaseStatusCheck", "Error checking system_status: ${e.message}")
            SystemStatusResult(
                isReachable = false,
                isPass = false,
                message = "Exception: ${e.message}"
            )
        }
    }
}
