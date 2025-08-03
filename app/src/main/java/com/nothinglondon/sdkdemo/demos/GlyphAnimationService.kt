package com.nothinglondon.sdkdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject

class GlyphAnimationService : Service() {

    private lateinit var glyphMatrixManager: GlyphMatrixManager
    private val animationHandler = Handler(Looper.getMainLooper())
    private var isAnimating = false
    private var brightness = 0
    private var isFadingIn = true
    private val TAG = "GlyphGemini"

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return

            if (isFadingIn) {
                brightness += 20
                if (brightness >= 255) {
                    brightness = 255
                    isFadingIn = false
                }
            } else {
                brightness -= 20
                if (brightness <= 0) {
                    brightness = 0
                    isFadingIn = true
                }
            }

            val frame = buildPulsingMicFrame()
            glyphMatrixManager.setMatrixFrame(frame)
            animationHandler.postDelayed(this, 50)
        }
    }

    // --- FINAL CENTERED MICROPHONE ANIMATION ---
    private fun buildPulsingMicFrame(): GlyphMatrixFrame {
        // 1. The head of the microphone (pulses)
        // Calculated to place the VISUAL CENTER at (12, 7)
        val micHead = GlyphMatrixObject.Builder()
            .setText("TEST")
            .setScale(160)
            .setBrightness(brightness)
            .setPosition(4, 9) // Centered X, upper Y
            .build()

        return GlyphMatrixFrame.Builder()
            .addMid(micHead) // Head is on the middle layer
            .build(this)
    }
    // --- END OF FINAL ANIMATION ---

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "GlyphAnimationService: onStartCommand called.")
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, "GEMINI_GLYPH_CHANNEL")
            .setContentTitle("Glyph Service Active")
            .setContentText("Listening for Gemini assistant.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)

        glyphMatrixManager = GlyphMatrixManager.getInstance(this)

        glyphMatrixManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager service connected.")
                glyphMatrixManager.register(Glyph.DEVICE_23112)

                Log.d(TAG, "STARTING ANIMATION.")
                if (!isAnimating) {
                    isAnimating = true
                    animationHandler.post(animationRunnable)
                }
            }

            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager service disconnected.")
                isAnimating = false
            }
        })

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GlyphAnimationService: onDestroy called. Releasing Glyphs.")
        isAnimating = false
        animationHandler.removeCallbacks(animationRunnable)

        if (::glyphMatrixManager.isInitialized) {
            Log.d(TAG, "Manager is initialized. Sending 'all off' frame and un-initializing.")

            val offObject = GlyphMatrixObject.Builder().setText(" ").setBrightness(0).build()
            val offFrame = GlyphMatrixFrame.Builder().addLow(offObject).build(this)
            glyphMatrixManager.setMatrixFrame(offFrame)
            glyphMatrixManager.unInit()
        } else {
            Log.d(TAG, "Manager was not initialized, skipping cleanup.")
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "GEMINI_GLYPH_CHANNEL",
            "Gemini Glyph Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}