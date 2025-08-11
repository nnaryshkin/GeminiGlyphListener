package com.nothinglondon.sdkdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import kotlin.math.max

class GlyphAnimationService : Service() {

    private lateinit var glyphMatrixManager: GlyphMatrixManager
    private val animationHandler = Handler(Looper.getMainLooper())
    private var isAnimating = false
    private val TAG = "GlyphGemini"

    private enum class AnimationPhase { INTRO, SCANNING }

    private var currentPhase = AnimationPhase.INTRO
    private var lineBrightness = 0
    private var eyePositionX = 0
    private var isScanningRight = true

    private val maxLineBrightness = 80
    private val maxPositionX = 11
    private val minPositionX = 0
    private var lineBitmap: Bitmap? = null

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return

            when (currentPhase) {
                AnimationPhase.INTRO -> {
                    lineBrightness += 15
                    if (lineBrightness >= maxLineBrightness) {
                        lineBrightness = maxLineBrightness
                        currentPhase = AnimationPhase.SCANNING
                    }
                    animationHandler.postDelayed(this, 75)
                }
                AnimationPhase.SCANNING -> {
                    if (isScanningRight) {
                        eyePositionX++
                        if (eyePositionX >= maxPositionX) { eyePositionX = maxPositionX; isScanningRight = false }
                    } else {
                        eyePositionX--
                        if (eyePositionX <= minPositionX) { eyePositionX = minPositionX; isScanningRight = true }
                    }
                    animationHandler.postDelayed(this, 90)
                }
            }
            val frame = buildFrame()
            glyphMatrixManager.setMatrixFrame(frame)
        }
    }

    private fun buildFrame(): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()

        // FINAL POSITION
        val lineObject = GlyphMatrixObject.Builder()
            .setImageSource(lineBitmap)
            .setBrightness(lineBrightness)
            .setScale(120)
            .setPosition(0, 0)
            .build()
        frameBuilder.addLow(lineObject)

        if (currentPhase == AnimationPhase.SCANNING) {
            // FINAL POSITION
            val eyeObject = GlyphMatrixObject.Builder()
                .setText(".")
                .setScale(100)
                .setBrightness(255)
                .setPosition(eyePositionX, 7)
                .build()
            frameBuilder.addTop(eyeObject)
        }

        return frameBuilder.build(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "GlyphAnimationService: onStartCommand called.")
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "GEMINI_GLYPH_CHANNEL")
            .setContentTitle("Glyph Service Active").setContentText("Listening for Gemini assistant.")
            .setSmallIcon(R.mipmap.ic_launcher).build()
        startForeground(1, notification)

        val lineDrawable = ContextCompat.getDrawable(this, R.drawable.shape_line)
        if (lineDrawable != null) {
            lineBitmap = drawableToBitmap(lineDrawable)
        }

        glyphMatrixManager = GlyphMatrixManager.getInstance(this)
        glyphMatrixManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                Log.d(TAG, "GlyphMatrixManager service connected.")
                glyphMatrixManager.register(Glyph.DEVICE_23112)
                Log.d(TAG, "STARTING FINAL KITT ANIMATION.")
                if (!isAnimating) {
                    currentPhase = AnimationPhase.INTRO
                    lineBrightness = 0
                    eyePositionX = 0
                    isScanningRight = true
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

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = max(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val left = (size - drawable.intrinsicWidth) / 2
        val top = (size - drawable.intrinsicHeight) / 2
        drawable.setBounds(left, top, left + drawable.intrinsicWidth, top + drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
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
            "GEMINI_GLYPH_CHANNEL", "Gemini Glyph Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}