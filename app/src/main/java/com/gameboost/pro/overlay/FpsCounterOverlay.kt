package com.gameboost.pro.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FpsCounterOverlay : Service() {
    private var overlayView: LinearLayout? = null
    private var windowManager: WindowManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // REAL FPS tracking using frame timing
    private var frameTimestamps = LongArray(60)
    private var frameIndex = 0
    private var totalFrames = 0L
    private var lastFrameTime = 0L
    private var currentFps = 0
    
    companion object {
        var isRunning = false
        var currentFpsValue = 0
        var fpsHistory = mutableListOf<Int>()
        
        fun start(ctx: Context) {
            if (!Settings.canDrawOverlays(ctx)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                Toast.makeText(ctx, "Enable 'Display over other apps'", Toast.LENGTH_LONG).show()
                return
            }
            if (!isRunning) {
                fpsHistory.clear()
                ctx.startForegroundService(Intent(ctx, FpsCounterOverlay::class.java))
            }
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, FpsCounterOverlay::class.java)) }
    }
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            isRunning = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel("fps_overlay", "FPS Counter", NotificationManager.IMPORTANCE_LOW))
            }
            startForeground(3001, NotificationCompat.Builder(this, "fps_overlay")
                .setContentTitle("FPS Counter Active").setSmallIcon(android.R.drawable.ic_menu_camera).setOngoing(true).build())
            setupOverlay()
            startRealFpsTracking()
        } catch (e: Exception) { isRunning = false; stopSelf() }
    }
    
    private fun setupOverlay() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            overlayView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10, 6, 10, 6)
                setBackgroundColor(Color.argb(150, 0, 0, 0))
            }
            
            // FPS Value
            val fpsLabel = TextView(this).apply {
                text = "FPS: --"
                setTextColor(Color.parseColor("#00FF00"))
                textSize = 14f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            overlayView?.addView(fpsLabel)
            
            // FPS Status
            val statusLabel = TextView(this).apply {
                text = "Measuring..."
                setTextColor(Color.parseColor("#888888"))
                textSize = 9f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            overlayView?.addView(statusLabel)
            
            // Min/Max/Avg line
            val statsLabel = TextView(this).apply {
                text = ""
                setTextColor(Color.parseColor("#666666"))
                textSize = 8f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            overlayView?.addView(statsLabel)
            
            // Draggable
            var ix=0; var iy=0; var itx=0f; var ity=0f
            overlayView?.setOnTouchListener { _, e ->
                when(e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val p = overlayView?.layoutParams as? WindowManager.LayoutParams
                        ix = p?.x ?: 0; iy = p?.y ?: 0; itx = e.rawX; ity = e.rawY; true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val p = overlayView?.layoutParams as? WindowManager.LayoutParams
                        p?.x = ix + (e.rawX - itx).toInt()
                        p?.y = iy + (e.rawY - ity).toInt()
                        p?.let { windowManager?.updateViewLayout(overlayView, it) }; true
                    }
                    else -> false
                }
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START; x = 16; y = 350 }
            windowManager?.addView(overlayView, params)
            
            // Start updating UI
            Thread {
                while (isRunning) {
                    try {
                        val fps = currentFps
                        val avgFps = if (fpsHistory.isNotEmpty()) fpsHistory.average().toInt() else 0
                        val minFps = if (fpsHistory.isNotEmpty()) fpsHistory.min() else 0
                        val maxFps = if (fpsHistory.isNotEmpty()) fpsHistory.max() else 0
                        
                        currentFpsValue = fps
                        
                        mainHandler.post {
                            try {
                                // FPS Color & Status
                                val (fpsColor, status, statusColor) = when {
                                    fps >= 55 -> Triple("#00FF00", "✅ SMOOTH", "#00FF00")
                                    fps in 40..54 -> Triple("#AAFF00", "🟡 PLAYABLE", "#AAFF00")
                                    fps in 25..39 -> Triple("#FFA500", "🟠 LAGGING", "#FFA500")
                                    fps in 1..24 -> Triple("#FF0000", "🔴 CHOPPY", "#FF0000")
                                    else -> Triple("#888888", "Measuring...", "#888888")
                                }
                                
                                fpsLabel.text = "FPS: ${if (fps > 0) fps else "--"}"
                                fpsLabel.setTextColor(Color.parseColor(fpsColor))
                                statusLabel.text = status
                                statusLabel.setTextColor(Color.parseColor(statusColor))
                                
                                if (minFps > 0 && maxFps > 0) {
                                    statsLabel.text = "min:${minFps} avg:${avgFps} max:${maxFps}"
                                    statsLabel.setTextColor(Color.parseColor("#666666"))
                                }
                            } catch (_: Exception) { }
                        }
                        Thread.sleep(1000)
                    } catch (_: Exception) { }
                }
            }.start()
            
        } catch (e: Exception) { stopSelf() }
    }
    
    /**
     * REAL FPS tracking using Choreographer frame callbacks
     */
    private fun startRealFpsTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val choreographer = Choreographer.getInstance()
            val frameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isRunning) return
                    
                    val now = frameTimeNanos
                    frameTimestamps[frameIndex % 60] = now
                    frameIndex++
                    totalFrames++
                    
                    // Calculate FPS every second
                    if (frameIndex % 60 == 0 || (frameIndex > 0 && now - frameTimestamps[0] > 1_000_000_000L)) {
                        val oldest = frameTimestamps.filter { it > 0 }.minOrNull() ?: now
                        val elapsed = (now - oldest) / 1_000_000_000f
                        val frames = frameIndex.coerceAtMost(60)
                        currentFps = if (elapsed > 0) (frames / elapsed).toInt().coerceIn(0, 120) else 0
                        
                        if (currentFps > 0) {
                            fpsHistory.add(currentFps)
                            if (fpsHistory.size > 100) fpsHistory.removeAt(0)
                        }
                    }
                    
                    // Post next frame
                    choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(frameCallback)
        } else {
            // Fallback: simple time-based estimation
            Thread {
                while (isRunning) {
                    val now = System.currentTimeMillis()
                    if (lastFrameTime > 0) {
                        val delta = now - lastFrameTime
                        currentFps = if (delta > 0) (1000f / delta).toInt().coerceIn(0, 120) else 0
                    }
                    lastFrameTime = now
                    try { Thread.sleep(1000) } catch (_: Exception) { }
                }
            }.start()
        }
    }
    
    override fun onDestroy() {
        isRunning = false
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) { }
        super.onDestroy()
    }
}
