package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SupabaseVideoApi {
    @POST
    suspend fun extractVideo(
        @Url url: String,
        @Header("Authorization") authHeader: String?,
        @Header("apikey") apiKey: String?,
        @Body request: ExtractVideoRequestDto
    ): Response<ExtractVideoResponseDto>
}
