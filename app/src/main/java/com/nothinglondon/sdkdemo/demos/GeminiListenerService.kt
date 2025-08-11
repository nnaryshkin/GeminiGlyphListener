package com.nothinglondon.sdkdemo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GeminiListenerService : AccessibilityService() {

    private val geminiPackageName = "com.google.android.googlequicksearchbox"
    private val TAG = "GlyphGemini"

    private var isGeminiUiVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable {
        Log.d(TAG, "Timeout reached. GEMINI DISMISSED! Stopping animation.")
        isGeminiUiVisible = false
        stopService(Intent(this, GlyphAnimationService::class.java))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val eventPackage = event.packageName?.toString()
        val isCurrentEventFromGemini = eventPackage == geminiPackageName

        if (isCurrentEventFromGemini) {
            handler.removeCallbacks(stopRunnable)

            if (!isGeminiUiVisible) {
                Log.d(TAG, "GEMINI APPEARED! Starting animation.")
                isGeminiUiVisible = true
                startService(Intent(this, GlyphAnimationService::class.java))
            }
            // Sets the animation to stop after 3 seconds of no new Gemini events
            handler.postDelayed(stopRunnable, 4000)
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "GeminiListenerService connected and running.")
    }
}