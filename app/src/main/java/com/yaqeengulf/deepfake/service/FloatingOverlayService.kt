package com.yaqeengulf.deepfake.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yaqeengulf.deepfake.MainActivity
import com.yaqeengulf.deepfake.YaqeenApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Floating Overlay Service for Yaqeen Gulf.
 * Implements a draggable floating shield widget that stays on top when permitted.
 * Gracefully shuts down if overlay permissions are revoked or unavailable.
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayAttached = false
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var audioRecordingView: View? = null
    private var isAudioRecordingAttached = false
    private var audioResultView: View? = null
    private var isAudioResultAttached = false

    private var imageScanningView: View? = null
    private var isImageScanningAttached = false
    private var imageResultView: View? = null
    private var isImageResultAttached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!checkOverlayPermission()) {
            Log.w(TAG, "Overlay permission not granted; stopping service gracefully.")
            stopSelf()
            return
        }
        startAsForeground()
        createFloatingWidget()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (!checkOverlayPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun startAsForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, YaqeenApp.CHANNEL_OVERLAY_SERVICE)
            .setContentTitle("Yaqeen Gulf Shield Active")
            .setContentText("Tap the floating shield icon to verify content on your screen")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createFloatingWidget() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 60
                y = 300
            }

            // Create circular floating shield container
            val container = FrameLayout(this).apply {
                val backgroundDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#059669")) // Emerald Primary
                    setStroke(4, Color.parseColor("#10B981"))
                }
                background = backgroundDrawable
                val density = resources.displayMetrics.density
                val sizePx = (56 * density).toInt()
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                elevation = 16f
            }

            // Inner icon
            val iconView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_lock_lock)
                setColorFilter(Color.WHITE)
                val density = resources.displayMetrics.density
                val paddingPx = (14 * density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            }
            container.addView(iconView)

            // Touch listener for dragging & clicking
            container.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = false

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isClick = true
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (abs(dx) > 10 || abs(dy) > 10) {
                                isClick = false
                            }
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating overlay layout", e)
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                toggleActionMenu(params.x, params.y)
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager?.addView(container, params)
            overlayView = container
            isOverlayAttached = true
            Log.d(TAG, "Floating shield overlay attached.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach floating overlay: ${e.message}", e)
            stopSelf()
        }
    }

    private var menuView: View? = null
    private var isMenuAttached = false

    private fun toggleActionMenu(anchorX: Int, anchorY: Int) {
        if (isMenuAttached) {
            hideActionMenu()
        } else {
            showActionMenu(anchorX, anchorY)
        }
    }

    private fun hideActionMenu() {
        if (isMenuAttached && menuView != null) {
            try {
                windowManager?.removeView(menuView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing menu view", e)
            }
            menuView = null
            isMenuAttached = false
        }
    }

    private fun showActionMenu(anchorX: Int, anchorY: Int) {
        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val screenWidth = resources.displayMetrics.widthPixels
            val menuWidth = (220 * density).toInt()

            val menuX = if (anchorX + menuWidth > screenWidth) {
                screenWidth - menuWidth - (16 * density).toInt()
            } else {
                maxOf((16 * density).toInt(), anchorX)
            }
            val menuY = (anchorY + (60 * density).toInt()).coerceAtLeast((50 * density).toInt())

            val menuParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = menuX
                y = menuY
            }

            val menuLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C1B2E"))
                    cornerRadius = 16 * density
                    setStroke((1.5f * density).toInt(), Color.parseColor("#10B981"))
                }
                background = bg
                val pad = (12 * density).toInt()
                setPadding(pad, pad, pad, pad)
                elevation = 24f
            }

            // Header
            val headerText = android.widget.TextView(this).apply {
                text = "Yaqeen Quick Shield"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#10B981"))
                setPadding(0, 0, 0, (8 * density).toInt())
            }
            menuLayout.addView(headerText)

            // Button 0: Scan On-Screen Image (Feature A - Dual Model Live-Scan)
            menuLayout.addView(createMenuItem("🖼️ Scan Screen Image (Dual CAM)") {
                hideActionMenu()
                startFloatingImageScan()
            })

            // Button 2: Scan Audio / Voice In-Place (over Instagram, YouTube, TikTok, etc.)
            menuLayout.addView(createMenuItem("🎙️ Quick Voice Check (Listen)") {
                hideActionMenu()
                startFloatingAudioRecording()
            })

            // Button 3: Open App
            menuLayout.addView(createMenuItem("📱 Open Yaqeen App") {
                hideActionMenu()
                val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            })

            // Button 4: Dismiss
            menuLayout.addView(createMenuItem("✖️ Dismiss") {
                hideActionMenu()
            })

            windowManager?.addView(menuLayout, menuParams)
            menuView = menuLayout
            isMenuAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show action menu", e)
        }
    }

    private fun createMenuItem(title: String, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density
        return android.widget.TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(Color.WHITE)
            val vPad = (8 * density).toInt()
            val hPad = (10 * density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            isClickable = true
            isFocusable = true
            val itemBg = GradientDrawable().apply {
                setColor(Color.parseColor("#152840"))
                cornerRadius = 8 * density
            }
            background = itemBg
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }
    }

    private fun startFloatingAudioRecording() {
        if (isAudioRecordingAttached) return
        removeAudioResultView()

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val pillParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (48 * density).toInt()
            }

            val pillLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C1B2E"))
                    cornerRadius = 24 * density
                    setStroke((2 * density).toInt(), Color.parseColor("#F59E0B"))
                }
                background = bg
                val hPad = (16 * density).toInt()
                val vPad = (10 * density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
                elevation = 32f
            }

            val micIcon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.parseColor("#F59E0B"))
                val size = (24 * density).toInt()
                layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (10 * density).toInt()
                }
            }
            pillLayout.addView(micIcon)

            val statusText = android.widget.TextView(this).apply {
                text = "🎙️ Listening to audio (0/4s)..."
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            pillLayout.addView(statusText)

            windowManager?.addView(pillLayout, pillParams)
            audioRecordingView = pillLayout
            isAudioRecordingAttached = true

            // Record 4 seconds from MIC / Loudspeaker loopback
            serviceScope.launch {
                val app = application as YaqeenApp
                val isArabic = app.preferencesRepository.selectedAudioModel.first().equals("arabic", ignoreCase = true)
                val modelName = "ArEnAV (Bilingual)"

                val recordResult = com.yaqeengulf.deepfake.capture.audio.AudioCaptureManager.recordAudio(
                    durationSeconds = 4,
                    onProgress = { p ->
                        statusText.post {
                            val elapsed = (p * 4.0).toInt()
                            statusText.text = "🎙️ Listening ($elapsed/4s) [$modelName]..."
                        }
                    }
                )

                removeAudioRecordingView()

                recordResult.onSuccess { samples ->
                    statusText.text = "Analyzing acoustic spectrum..."
                    try {
                        val result = withContext(Dispatchers.Default) {
                            app.verificationOrchestrator.verifyAudio(
                                samples = samples,
                                isArabic = isArabic,
                                sourceTag = "Floating Overlay In-Place"
                            )
                        }
                        showFloatingAudioResult(result)
                    } catch (e: Exception) {
                        Toast.makeText(this@FloatingOverlayService, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(this@FloatingOverlayService, "Recording error: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start floating audio recording", e)
        }
    }

    private fun removeAudioRecordingView() {
        if (isAudioRecordingAttached && audioRecordingView != null) {
            try {
                windowManager?.removeView(audioRecordingView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing audio recording view", e)
            }
            audioRecordingView = null
            isAudioRecordingAttached = false
        }
    }

    private fun showFloatingAudioResult(result: com.yaqeengulf.deepfake.domain.orchestrator.UnifiedScanResult) {
        removeAudioResultView()

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val cardParams = WindowManager.LayoutParams(
                (300 * density).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (50 * density).toInt()
            }

            val isFake = result.verdict == "SYNTHETIC"
            val isSuspicious = result.verdict == "SUSPICIOUS"
            val verdictColor = when {
                isFake -> Color.parseColor("#EF4444")
                isSuspicious -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#10B981")
            }

            val cardLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C1B2E"))
                    cornerRadius = 16 * density
                    setStroke((2 * density).toInt(), verdictColor)
                }
                background = bg
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                elevation = 32f
            }

            // Header row with verdict and close button
            val headerRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleText = android.widget.TextView(this).apply {
                val iconStr = if (isFake) "❌" else if (isSuspicious) "⚠️" else "✅"
                text = "$iconStr ${result.verdict}: ${(result.confidence * 100).toInt()}%"
                setTextColor(verdictColor)
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(titleText)

            val closeBtn = android.widget.TextView(this).apply {
                text = "✕"
                setTextColor(Color.WHITE)
                textSize = 16f
                setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
                setOnClickListener { removeAudioResultView() }
            }
            headerRow.addView(closeBtn)
            cardLayout.addView(headerRow)

            // Score & Model
            val scoreText = android.widget.TextView(this).apply {
                text = "Model Score: ${String.format(java.util.Locale.US, "%.5f", result.rawScore)} (${result.modelName})"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                setPadding(0, (4 * density).toInt(), 0, (6 * density).toInt())
            }
            cardLayout.addView(scoreText)

            // Summary
            val summaryText = android.widget.TextView(this).apply {
                text = result.summary
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(0, 0, 0, (10 * density).toInt())
            }
            cardLayout.addView(summaryText)

            // View full report button
            val viewReportBtn = android.widget.Button(this).apply {
                text = "Open Full Report in Yaqeen"
                textSize = 12f
                setTextColor(Color.WHITE)
                val btnBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#059669"))
                    cornerRadius = 8 * density
                }
                background = btnBg
                setOnClickListener {
                    removeAudioResultView()
                    val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
            cardLayout.addView(viewReportBtn)

            windowManager?.addView(cardLayout, cardParams)
            audioResultView = cardLayout
            isAudioResultAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating audio result", e)
        }
    }

    private fun removeAudioResultView() {
        if (isAudioResultAttached && audioResultView != null) {
            try {
                windowManager?.removeView(audioResultView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing audio result view", e)
            }
            audioResultView = null
            isAudioResultAttached = false
        }
    }

    private fun startFloatingImageScan() {
        if (isImageScanningAttached) return
        removeImageResultView()
        removeAudioResultView()

        if (!com.yaqeengulf.deepfake.capture.overlay.YaqeenAccessibilityService.isServiceRunning) {
            Toast.makeText(this, "Please tap 'Installed apps' -> 'Yaqeen Gulf' -> Turn On, then tap the shield again.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val pillParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (48 * density).toInt()
            }

            val pillLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C1B2E"))
                    cornerRadius = 24 * density
                    setStroke((2 * density).toInt(), Color.parseColor("#10B981"))
                }
                background = bg
                val hPad = (16 * density).toInt()
                val vPad = (10 * density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
                elevation = 32f
            }

            val scanIcon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_camera)
                setColorFilter(Color.parseColor("#10B981"))
                val size = (24 * density).toInt()
                layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (10 * density).toInt()
                }
            }
            pillLayout.addView(scanIcon)

            val statusText = android.widget.TextView(this).apply {
                text = "🖼️ Capturing screen & detecting image..."
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            pillLayout.addView(statusText)

            windowManager?.addView(pillLayout, pillParams)
            imageScanningView = pillLayout
            isImageScanningAttached = true

            com.yaqeengulf.deepfake.capture.overlay.YaqeenAccessibilityService.captureAndCrop { croppedBitmap, cropBounds, tier, error ->
                if (error != null || croppedBitmap == null) {
                    removeImageScanningView()
                    Toast.makeText(this@FloatingOverlayService, error ?: "No image detected on screen.", Toast.LENGTH_SHORT).show()
                    return@captureAndCrop
                }

                statusText.post {
                    statusText.text = "⚡ Running EffNet-B3 + Swin-T..."
                }

                serviceScope.launch {
                    try {
                        val app = application as YaqeenApp
                        val result = withContext(Dispatchers.Default) {
                            app.verificationOrchestrator.verifyImageDual(
                                croppedBitmap = croppedBitmap,
                                cropBounds = cropBounds ?: android.graphics.Rect(0, 0, croppedBitmap.width, croppedBitmap.height),
                                resolutionTier = tier,
                                sourceTag = "Floating Overlay Live-Scan"
                            )
                        }
                        removeImageScanningView()
                        showFloatingImageResult(result)
                    } catch (e: Exception) {
                        removeImageScanningView()
                        Log.e(TAG, "Dual image scan error", e)
                        Toast.makeText(this@FloatingOverlayService, "Image verification error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start floating image scan", e)
        }
    }

    private fun showFloatingImageResult(result: com.yaqeengulf.deepfake.domain.model.DualImageScanResult) {
        removeImageResultView()

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val cardParams = WindowManager.LayoutParams(
                (340 * density).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (40 * density).toInt()
            }

            val hasSynthetic = result.effnetVerdict == "SYNTHETIC" || result.swinVerdict == "SYNTHETIC"
            val hasPotential = result.effnetVerdict == "POTENTIALLY FAKE" || result.swinVerdict == "POTENTIALLY FAKE"
            val borderColor = when {
                !result.modelsAgree -> Color.parseColor("#F59E0B") // Amber for disagreement
                hasSynthetic -> Color.parseColor("#EF4444")      // Red for synthetic agreement
                hasPotential -> Color.parseColor("#F59E0B")      // Amber for potential
                else -> Color.parseColor("#10B981")              // Green for authentic
            }

            val cardLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0C1B2E"))
                    cornerRadius = 16 * density
                    setStroke((2 * density).toInt(), borderColor)
                }
                background = bg
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                elevation = 32f
            }

            // Header row: Agreement badge and close button
            val headerRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleText = android.widget.TextView(this).apply {
                val iconStr = if (!result.modelsAgree) "⚠️" else if (hasSynthetic) "🚨" else "✅"
                text = if (result.modelsAgree) "$iconStr Models Agree: ${result.effnetVerdict}" else "$iconStr Models Disagree"
                setTextColor(borderColor)
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(titleText)

            val closeBtn = android.widget.TextView(this).apply {
                text = "✕"
                setTextColor(Color.WHITE)
                textSize = 16f
                setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
                setOnClickListener { removeImageResultView() }
            }
            headerRow.addView(closeBtn)
            cardLayout.addView(headerRow)

            // Telemetry & Tier Subtitle
            val tierText = android.widget.TextView(this).apply {
                val boundsStr = result.cropBounds?.let { "${it.width()}x${it.height()} px" } ?: "Auto Detected"
                text = "${result.resolutionTier.displayName} • $boundsStr"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, (2 * density).toInt(), 0, (8 * density).toInt())
            }
            cardLayout.addView(tierText)

            // Side-by-side Dual Grad-CAM visualization
            val dualCamRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val camWidth = (148 * density).toInt()
            val camHeight = (130 * density).toInt()

            // Column 1: EfficientNet-B3
            val effnetCol = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (4 * density).toInt()
                }
                val colBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#152840"))
                    cornerRadius = 8 * density
                }
                background = colBg
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            }

            val effnetImg = ImageView(this).apply {
                setImageBitmap(result.effnetComposite)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = android.widget.LinearLayout.LayoutParams(camWidth, camHeight)
            }
            effnetCol.addView(effnetImg)

            val effnetLabel = android.widget.TextView(this).apply {
                text = "EfficientNet-B3\n${result.effnetVerdict} (${String.format(java.util.Locale.US, "%.1f%%", result.effnetFakeProbability * 100f)})"
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, (4 * density).toInt(), 0, 0)
            }
            effnetCol.addView(effnetLabel)
            dualCamRow.addView(effnetCol)

            // Column 2: Swin-T
            val swinCol = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (4 * density).toInt()
                }
                val colBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#152840"))
                    cornerRadius = 8 * density
                }
                background = colBg
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            }

            val swinImg = ImageView(this).apply {
                setImageBitmap(result.swinComposite)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = android.widget.LinearLayout.LayoutParams(camWidth, camHeight)
            }
            swinCol.addView(swinImg)

            val swinLabel = android.widget.TextView(this).apply {
                text = "Swin-T (Patch)\n${result.swinVerdict} (${String.format(java.util.Locale.US, "%.1f%%", result.swinFakeProbability * 100f)})"
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, (4 * density).toInt(), 0, 0)
            }
            swinCol.addView(swinLabel)
            dualCamRow.addView(swinCol)

            cardLayout.addView(dualCamRow)

            // Explanation / Summary text
            val summaryText = android.widget.TextView(this).apply {
                text = result.agreementSummary
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 12f
                setPadding(0, (8 * density).toInt(), 0, (10 * density).toInt())
            }
            cardLayout.addView(summaryText)

            // Open Full Report Button
            val openAppBtn = android.widget.Button(this).apply {
                text = "View in Yaqeen History"
                textSize = 12f
                setTextColor(Color.WHITE)
                val btnBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#059669"))
                    cornerRadius = 8 * density
                }
                background = btnBg
                setOnClickListener {
                    removeImageResultView()
                    val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
            cardLayout.addView(openAppBtn)

            windowManager?.addView(cardLayout, cardParams)
            imageResultView = cardLayout
            isImageResultAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating image result", e)
        }
    }

    private fun removeImageScanningView() {
        if (isImageScanningAttached && imageScanningView != null) {
            try {
                windowManager?.removeView(imageScanningView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing image scanning view", e)
            }
            imageScanningView = null
            isImageScanningAttached = false
        }
    }

    private fun removeImageResultView() {
        if (isImageResultAttached && imageResultView != null) {
            try {
                windowManager?.removeView(imageResultView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing image result view", e)
            }
            imageResultView = null
            isImageResultAttached = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        hideActionMenu()
        removeAudioRecordingView()
        removeAudioResultView()
        removeImageScanningView()
        removeImageResultView()
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view: ${e.message}", e)
            }
            overlayView = null
            isOverlayAttached = false
        }
        Log.d(TAG, "FloatingOverlayService stopped.")
    }

    companion object {
        private const val TAG = "FloatingOverlayService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.yaqeengulf.deepfake.action.START_OVERLAY"
        const val ACTION_STOP = "com.yaqeengulf.deepfake.action.STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
