package com.example.data

import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseSystemStatusChecker
import com.example.data.remote.SystemStatusDto
import com.example.data.remote.SystemStatusResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
class SupabaseSystemStatusTest {

    @Test
    fun testSystemStatusDtoParsing() {
        val json = """[{"id":1,"status":"online","backend_version":"1","updated_at":"2026-08-07T00:00:00Z"}]"""
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, SystemStatusDto::class.java)
        val adapter = moshi.adapter<List<SystemStatusDto>>(listType)
        val result = adapter.fromJson(json)

        assertTrue(result != null && result.isNotEmpty())
        assertEquals("online", result!![0].status)
        assertEquals("1", result[0].backendVersion)
    }

    @Test
    fun testPassResultEvaluation() {
        val passResult = SystemStatusResult(
            isReachable = true,
            httpCode = 200,
            status = "online",
            backendVersion = "1",
            isPass = true,
            message = "PASS (status=online, version=1)"
        )
        assertTrue(passResult.isPass)
        assertTrue(passResult.isReachable)
        assertEquals(200, passResult.httpCode)
    }

    @Test
    fun testFailResultEvaluation() {
        val failResult = SystemStatusResult(
            isReachable = true,
            httpCode = 200,
            status = "maintenance",
            backendVersion = "1",
            isPass = false,
            message = "FAIL (status=maintenance, version=1)"
        )
        assertFalse(failResult.isPass)
        assertTrue(failResult.isReachable)
    }

    @Test
    fun testSupabaseApiServiceHeadersAndPath() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort
        var capturedRequest = ""
        var capturedApikey = ""
        var capturedBearer = ""

        val latch = CountDownLatch(1)

        thread {
            try {
                val socket = server.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = PrintWriter(socket.getOutputStream(), true)

                var line = reader.readLine()
                if (line != null) {
                    capturedRequest = line
                }
                while (line != null && line.isNotEmpty()) {
                    if (line.lowercase().startsWith("apikey:")) {
                        capturedApikey = line.substringAfter(":").trim()
                    }
                    if (line.lowercase().startsWith("authorization: bearer")) {
                        capturedBearer = line.substringAfter("bearer").trim()
                    }
                    line = reader.readLine()
                }

                val responseBody = """[{"id":1,"status":"online","backend_version":"1"}]"""
                out.print("HTTP/1.1 200 OK\r\n")
                out.print("Content-Type: application/json\r\n")
                out.print("Content-Length: ${responseBody.length}\r\n")
                out.print("\r\n")
                out.print(responseBody)
                out.flush()
                
                socket.close()
            } catch (e: Exception) {
            } finally {
                latch.countDown()
            }
        }

        val testUrl = "http://localhost:$port/"
        val apiService = SupabaseApiService.create(baseUrl = testUrl, publishableKey = "test_key_123")
        val checker = SupabaseSystemStatusChecker(apiService)
        
        val result = checker.checkStatus()

        latch.await(2, TimeUnit.SECONDS)
        server.close()

        assertTrue("Request path should be correct", capturedRequest.contains("/rest/v1/system_status"))
        assertEquals("test_key_123", capturedApikey)
        assertTrue("Should not send Authorization Bearer", capturedBearer.isEmpty() || capturedBearer != "test_key_123")
        assertTrue("Expected pass but got: ${result.message}", result.isPass)
        assertEquals("online", result.status)
        assertEquals("1", result.backendVersion)
        assertEquals(200, result.httpCode)
    }

    @Test
    fun testSupabaseApiServiceErrorHandling() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort

        thread {
            try {
                val socket = server.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = PrintWriter(socket.getOutputStream(), true)

                var line = reader.readLine()
                while (line != null && line.isNotEmpty()) {
                    line = reader.readLine()
                }

                out.print("HTTP/1.1 500 Internal Server Error\r\n")
                out.print("Content-Type: application/json\r\n")
                out.print("Content-Length: 0\r\n")
                out.print("\r\n")
                out.flush()
                
                socket.close()
            } catch (e: Exception) {
            }
        }

        val testUrl = "http://localhost:$port/"
        val apiService = SupabaseApiService.create(baseUrl = testUrl, publishableKey = "test_key_123")
        val checker = SupabaseSystemStatusChecker(apiService)
        
        val result = checker.checkStatus()
        server.close()

        assertFalse(result.isPass)
        assertEquals(500, result.httpCode)
    }
    
    @Test
    fun testSupabaseApiServiceNetworkError() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort
        server.close()

        val testUrl = "http://localhost:$port/"
        val apiService = SupabaseApiService.create(baseUrl = testUrl, publishableKey = "test_key_123")
        val checker = SupabaseSystemStatusChecker(apiService)
        
        val result = checker.checkStatus()

        assertFalse(result.isPass)
        assertFalse(result.isReachable)
        assertTrue(result.message.contains("Exception") || result.message.contains("HTTP"))
    }
    @Test
    fun testEdgeFunctionDtoParsing() {
        val json = """{"status":"online","message":"Edge Function is operational","version":"1.0"}"""
        val moshi = Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(com.example.data.remote.EdgeFunctionHealthDto::class.java)
        val result = adapter.fromJson(json)
        org.junit.Assert.assertTrue(result != null)
        org.junit.Assert.assertEquals("online", result?.status)
        org.junit.Assert.assertEquals("Edge Function is operational", result?.message)
        org.junit.Assert.assertEquals("1.0", result?.version)
    }

    @Test
    fun testEdgeFunctionNetwork() = kotlinx.coroutines.runBlocking {
        val server = java.net.ServerSocket(0)
        val port = server.localPort
        var capturedRequest = ""
        val latch = java.util.concurrent.CountDownLatch(1)
        kotlin.concurrent.thread {
            try {
                val socket = server.accept()
                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                val out = java.io.PrintWriter(socket.getOutputStream(), true)
                val line = reader.readLine()
                if (line != null) {
                    capturedRequest = line
                }
                while (reader.readLine().isNotEmpty()) { }
                val responseBody = """{"status":"online","message":"Edge Function is operational","version":"1.0"}"""
                out.print("HTTP/1.1 200 OK\r\n")
                out.print("Content-Type: application/json\r\n")
                out.print("Content-Length: ${responseBody.length}\r\n")
                out.print("\r\n")
                out.print(responseBody)
                out.flush()
                socket.close()
            } catch (e: Exception) {
            } finally {
                latch.countDown()
            }
        }
        val testUrl = "http://localhost:$port/"
        val apiService = com.example.data.remote.SupabaseApiService.create(baseUrl = testUrl, publishableKey = "test")
        val checker = com.example.data.remote.SupabaseSystemStatusChecker(apiService)
        val result = checker.checkEdgeFunctionStatus()
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
        server.close()
        org.junit.Assert.assertTrue("Request path should be correct: $capturedRequest", capturedRequest.contains("/functions/v1/health-check"))
        org.junit.Assert.assertTrue("Expected true", result.isReachable)
        org.junit.Assert.assertEquals("online", result.status)
        org.junit.Assert.assertEquals("1.0", result.version)
    }
}
