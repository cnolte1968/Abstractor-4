package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.GatewayDiagnostics
import com.example.data.PipelineReportStore

@Composable
fun DiagnosticCenterDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    clipboardManager: ClipboardManager,
    context: Context
) {
    if (showDialog) {
        val jsonReport = PipelineReportStore.getFormattedDiagnosticReport()

        val appVersion = BuildConfig.VERSION_NAME
        val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"

        val report = PipelineReportStore.getReport()
        val finalStatus = report?.final_result?.get("finalStatus") as? String
        val functionalStatus = report?.final_result?.get("functionalStatus") as? String

        val exceptionMsg = GatewayDiagnostics.exceptionMessage
        val failureStage = GatewayDiagnostics.failureStage.ifEmpty { GatewayDiagnostics.requestFailureStage }
        val runStatus = when {
            finalStatus == "DEGRADED" || functionalStatus == "DEGRADED" -> "DEGRADED"
            finalStatus == "FAIL" || exceptionMsg.isNotEmpty() || failureStage.isNotEmpty() -> "FAIL"
            finalStatus == "PASS" -> "PASS"
            else -> if (exceptionMsg.isNotEmpty() || failureStage.isNotEmpty()) "FAIL" else "PASS"
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Diagnose & Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        Text("App-Version:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(appVersion, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Geräteinformation:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(deviceInfo, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Status letzter Analyse-Lauf:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val statusColor = when (runStatus) {
                            "FAIL" -> MaterialTheme.colorScheme.error
                            "DEGRADED" -> Color(0xFFC2410C)
                            else -> Color(0xFF15803D)
                        }
                        Text("Status: $runStatus", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (runStatus == "FAIL") {
                            Text("Fehlerstufe: $failureStage", fontSize = 14.sp)
                            Text("Fehlermeldung: $exceptionMsg", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        } else if (runStatus == "DEGRADED") {
                            val reason = report?.final_result?.get("semanticOutcomeReason") as? String
                                ?: "Eingeschränkte Analyse (Metadaten-Fallback ohne Transkript)"
                            Text("Hinweis: $reason", color = Color(0xFF9A3412), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Pipeline Report Vorschau:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        SelectionContainer {
                            Text(
                                text = jsonReport,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(jsonReport))
                    Toast.makeText(context, "Diagnose-Report kopiert", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Diagnose-Report kopieren")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        )
    }
}
