package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class AbstractorAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val extractedList = mutableListOf<String>()
                scrapeNodes(rootNode, extractedList)
                if (extractedList.isNotEmpty()) {
                    val fullText = extractedList.joinToString("\n")
                    LocalContentExtractionEngine.updateScrapedText(fullText, rootNode.packageName?.toString())
                }
            }
        }
    }

    private fun scrapeNodes(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text.length > 3) {
            val className = node.className?.toString() ?: ""
            // Filter out input/text fields, password fields, or Abstractor itself to stay clean and secure
            if (!className.contains("EditText", ignoreCase = true) && 
                !text.contains("Abstractor", ignoreCase = true) &&
                !node.isPassword) {
                list.add(text.trim())
            }
        }
        
        for (i in 0 until node.childCount) {
            try {
                scrapeNodes(node.getChild(i), list)
            } catch (e: Exception) {
                // Catch any indexing/bounds anomalies on rapid view rendering
            }
        }
    }

    override fun onInterrupt() {
        Log.w("AbstractorService", "Accessibility Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AbstractorService", "Accessibility Service connected and starting tracking...")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info
    }
}
