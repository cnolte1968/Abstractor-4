package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.InetAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PreflightCheckResult(
    val name: String,
    val status: String, // "PASS" or "FAIL"
    val detail: String,
    val exceptionClass: String? = null,
    val exceptionMessage: String? = null,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
)

data class PreflightReport(
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
    val appVersion: String,
    val device: String,
    val networkType: String,
    val checks: List<PreflightCheckResult>
) {
    fun toJsonString(): String {
        val checkJsonList = checks.joinToString(",\n    ") { check ->
            """{
      "name": "${check.name}",
      "status": "${check.status}",
      "detail": "${check.detail.replace("\"", "\\\"").replace("\n", " ")}",
      "exceptionClass": ${check.exceptionClass?.let { "\"$it\"" } ?: "null"},
      "exceptionMessage": ${check.exceptionMessage?.let { "\"${it.replace("\"", "\\\"").replace("\n", " ")}\"" } ?: "null"},
      "timestamp": "${check.timestamp}"
    }"""
        }
        return """{
  "timestamp": "$timestamp",
  "appVersion": "$appVersion",
  "device": "$device",
  "networkType": "$networkType",
  "checks": [
    $checkJsonList
  ]
}"""
    }
}

object RuntimePreflight {
    private const val TAG = "RUNTIME_SMOKE"

    fun runPreflight(context: Context?): PreflightReport {
        Log.i(TAG, "RUNTIME_SMOKE_START - Preflight checks beginning")
        val checks = mutableListOf<PreflightCheckResult>()

        // 1. Check Internet Permission
        val permissionResult = if (context != null) {
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                PreflightCheckResult("Android-Permission (INTERNET)", "PASS", "INTERNET-Berechtigung ist in AndroidManifest erteilt.")
            } else {
                PreflightCheckResult("Android-Permission (INTERNET)", "FAIL", "INTERNET-Berechtigung wurde nicht erteilt!")
            }
        } else {
            PreflightCheckResult("Android-Permission (INTERNET)", "FAIL", "Context ist null, Permission-Check nicht möglich.")
        }
        checks.add(permissionResult)

        // 2. Check DNS Resolution of Gemini Host
        var dnsResult: PreflightCheckResult
        try {
            val addresses = InetAddress.getAllByName("generativelanguage.googleapis.com")
            if (addresses.isNotEmpty()) {
                val ipList = addresses.joinToString { it.hostAddress ?: "" }
                dnsResult = PreflightCheckResult(
                    name = "DNS-Auflösung (Gemini Host)",
                    status = "PASS",
                    detail = "Erfolgreich aufgelöst auf: $ipList"
                )
                Log.i(TAG, "DNS-Auflösung erfolgreich: $ipList")
            } else {
                dnsResult = PreflightCheckResult(
                    name = "DNS-Auflösung (Gemini Host)",
                    status = "FAIL",
                    detail = "Keine IP-Adressen zurückgegeben."
                )
                Log.e(TAG, "GEMINI_DNS_FAILURE - Keine IP-Adressen für generativelanguage.googleapis.com")
            }
        } catch (e: Exception) {
            dnsResult = PreflightCheckResult(
                name = "DNS-Auflösung (Gemini Host)",
                status = "FAIL",
                detail = e.localizedMessage ?: "Unbekannter DNS-Fehler",
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message
            )
            Log.e(TAG, "GEMINI_DNS_FAILURE - DNS-Auflösung für generativelanguage.googleapis.com fehlgeschlagen: ${e.message}", e)
        }
        checks.add(dnsResult)

        // 3. Check HTTPS Reachability (Port 443 TCP socket check is safer/faster on Android without full request overhead)
        var httpsResult: PreflightCheckResult
        try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress("generativelanguage.googleapis.com", 443), 5000)
            val isConnected = socket.isConnected
            socket.close()
            if (isConnected) {
                httpsResult = PreflightCheckResult(
                    name = "HTTPS-Erreichbarkeit (Port 443)",
                    status = "PASS",
                    detail = "TCP-Verbindung zu generativelanguage.googleapis.com:443 erfolgreich hergestellt."
                )
                Log.i(TAG, "HTTPS-Port 443 erfolgreich erreichbar.")
            } else {
                httpsResult = PreflightCheckResult(
                    name = "HTTPS-Erreichbarkeit (Port 443)",
                    status = "FAIL",
                    detail = "Socket-Verbindung fehlgeschlagen."
                )
                Log.e(TAG, "GEMINI_HTTP_FAILURE - TCP-Verbindung zu generativelanguage.googleapis.com:443 fehlgeschlagen")
            }
        } catch (e: Exception) {
            httpsResult = PreflightCheckResult(
                name = "HTTPS-Erreichbarkeit (Port 443)",
                status = "FAIL",
                detail = e.localizedMessage ?: "Verbindungsfehler",
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message
            )
            Log.e(TAG, "GEMINI_HTTP_FAILURE - Verbindung zu generativelanguage.googleapis.com:443 fehlgeschlagen: ${e.message}", e)
        }
        checks.add(httpsResult)

        val anyFail = checks.any { it.status == "FAIL" }
        if (anyFail) {
            Log.e(TAG, "RUNTIME_SMOKE_FAIL - Preflight checks contained failures")
        } else {
            Log.i(TAG, "RUNTIME_SMOKE_PASS - Preflight checks completed successfully")
        }

        // Populate GatewayDiagnostics with results of actual preflight check
        val dnsCheck = checks.find { it.name.contains("DNS") }
        val httpsCheck = checks.find { it.name.contains("HTTPS") }
        GatewayDiagnostics.preflightDns = dnsCheck?.status ?: "FAIL"
        GatewayDiagnostics.preflightHttps = httpsCheck?.status ?: "FAIL"
        GatewayDiagnostics.preflightDnsException = dnsCheck?.exceptionMessage ?: ""
        GatewayDiagnostics.preflightHttpsException = httpsCheck?.exceptionMessage ?: ""
        GatewayDiagnostics.preflightExecuted = true

        // Gather system details
        val appVersion = try {
            if (context != null) {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "unknown"
            } else "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        
        val networkType = if (context != null) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
                else -> "UNKNOWN / NONE"
            }
        } else {
            "UNKNOWN"
        }

        val report = PreflightReport(
            appVersion = appVersion,
            device = device,
            networkType = networkType,
            checks = checks
        )

        Log.i(TAG, "Preflight-Report:\n${report.toJsonString()}")
        return report
    }
}
