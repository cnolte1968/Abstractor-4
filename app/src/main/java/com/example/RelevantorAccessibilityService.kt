package com.example

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class RelevantorAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Local content extraction service triggered on screen shifts
    }

    override fun onInterrupt() {
        // Interrupt handling
    }
}
