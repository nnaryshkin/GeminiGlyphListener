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

    private enum class AnimationPhase { INTRO, PULSE, SCANNING, OUTRO }

    private var currentPhase = AnimationPhase.INTRO
    private var lineBrightness = 0
    private var eyePositionX = 0
    private var isScanningRight = true
    private var eyeBrightness = 0
    private var isPulsingIn = true
    private var pulseCount = 0
    private var scanSweeps = 0
    private var loopsPerformed = 0

    // Animation constants
    private val maxLineBrightness = 80
    private val maxPositionX = 25
    private val minPositionX = 0
    private val pulsesToPerform = 1
    private val centerPositionX = 13
    // FIX: Set to 1 to prevent the entire animation from repeating
    private val maxLoops = 1
    // FIX: Set to 4 for a longer scan sequence (center-left, left-right, right-left, left-right)
    private val sweepsToPerform = 4

    private var lineBitmap: Bitmap? = null

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return

            when (currentPhase) {
                AnimationPhase.INTRO -> {
                    lineBrightness += 15
                    if (lineBrightness >= maxLineBrightness) {
                        lineBrightness = maxLineBrightness
                        currentPhase = AnimationPhase.PULSE
                    }
                    animationHandler.postDelayed(this, 75)
                }
                AnimationPhase.PULSE -> {
                    if (isPulsingIn) {
                        eyeBrightness += 50
                        if (eyeBrightness >= 255) { eyeBrightness = 255; isPulsingIn = false }
                    } else {
                        eyeBrightness -= 50
                        if (eyeBrightness <= 0) { eyeBrightness = 0; isPulsingIn = true; pulseCount++ }
                    }
                    if (pulseCount >= pulsesToPerform) { currentPhase = AnimationPhase.SCANNING }
                    animationHandler.postDelayed(this, 40)
                }
                AnimationPhase.SCANNING -> {
                    if (isScanningRight) {
                        eyePositionX++
                        if (eyePositionX >= maxPositionX) {
                            eyePositionX = maxPositionX; isScanningRight = false; scanSweeps++
                        }
                    } else {
                        eyePositionX--
                        if (eyePositionX <= minPositionX) {
                            eyePositionX = minPositionX; isScanningRight = true; scanSweeps++
                        }
                    }

                    if (scanSweeps >= sweepsToPerform) {
                        currentPhase = AnimationPhase.OUTRO
                    }
                    animationHandler.postDelayed(this, 40)
                }
                AnimationPhase.OUTRO -> {
                    lineBrightness -= 10
                    if (lineBrightness <= 0) {
                        lineBrightness = 0
                        loopsPerformed++
                        if (loopsPerformed < maxLoops) {
                            currentPhase = AnimationPhase.INTRO
                            pulseCount = 0
                            scanSweeps = 0
                            eyePositionX = centerPositionX
                            isScanningRight = false
                            animationHandler.postDelayed(this, 500)
                        } else {
                            isAnimating = false
                        }
                    } else {
                        animationHandler.postDelayed(this, 40)
                    }
                }
            }

            if (isAnimating) {
                val frame = buildFrame()
                glyphMatrixManager.setMatrixFrame(frame)
            }
        }
    }

    private fun buildFrame(): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()

        val currentEyeBrightness = when(currentPhase) {
            AnimationPhase.PULSE -> eyeBrightness
            AnimationPhase.OUTRO -> lineBrightness * 3
            else -> 255
        }

        val lineObject = GlyphMatrixObject.Builder()
            .setImageSource(lineBitmap).setBrightness(lineBrightness)
            .setScale(120).setPosition(0, 0).build()
        frameBuilder.addLow(lineObject)

        if (currentPhase != AnimationPhase.INTRO) {
            val currentEyePosition = if (currentPhase == AnimationPhase.PULSE) centerPositionX else eyePositionX
            val eyeObject = GlyphMatrixObject.Builder()
                .setText(".").setScale(100).setBrightness(currentEyeBrightness)
                .setPosition(currentEyePosition, 7).build()
            frameBuilder.addTop(eyeObject)
        }

        return frameBuilder.build(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "GEMINI_GLYPH_CHANNEL")
            .setContentTitle("Glyph Service Active").setContentText("Listening for Gemini assistant.")
            .setSmallIcon(R.mipmap.ic_launcher).build()
        startForeground(1, notification)

        val lineDrawable = ContextCompat.getDrawable(this, R.drawable.shape_line)
        if (lineDrawable != null) { lineBitmap = drawableToBitmap(lineDrawable) }

        glyphMatrixManager = GlyphMatrixManager.getInstance(this)
        glyphMatrixManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                glyphMatrixManager.register(Glyph.DEVICE_23112)
                if (!isAnimating) {
                    currentPhase = AnimationPhase.INTRO
                    lineBrightness = 0
                    eyePositionX = centerPositionX
                    isScanningRight = false
                    eyeBrightness = 0
                    isPulsingIn = true
                    pulseCount = 0
                    scanSweeps = 0
                    loopsPerformed = 0
                    isAnimating = true
                    animationHandler.post(animationRunnable)
                }
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) { isAnimating = false }
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
        isAnimating = false
        animationHandler.removeCallbacks(animationRunnable)
        if (::glyphMatrixManager.isInitialized) {
            val offObject = GlyphMatrixObject.Builder().setText(" ").setBrightness(0).build()
            val offFrame = GlyphMatrixFrame.Builder().addLow(offObject).build(this)
            glyphMatrixManager.setMatrixFrame(offFrame)
            glyphMatrixManager.unInit()
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