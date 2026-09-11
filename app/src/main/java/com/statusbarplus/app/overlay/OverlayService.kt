package com.statusbarplus.app.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.statusbarplus.app.MainActivity
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.service.NotificationIconListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns the actual overlay window. Runs as a foreground service so Android
 * doesn't kill it while the overlay needs to stay visible/interactive.
 * Requires SYSTEM_ALERT_WINDOW to have already been granted — MainActivity
 * checks that and sends the user to the system grant screen if not.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: StatusBarCanvasView? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildForegroundNotification())
        addOverlayView()
        observePrefs()
    }

    private fun addOverlayView() {
        val statusBarHeightPx = resources.getIdentifier("status_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 72 }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }

        overlayView = StatusBarCanvasView(this)
        windowManager.addView(overlayView, params)
    }

    private fun observePrefs() {
        val prefs = PrefsStore(applicationContext)

        scope.launch { prefs.clockStyle.collect { overlayView?.clockStyle = it } }
        scope.launch { prefs.batteryStyle.collect { overlayView?.batteryStyle = it } }

        // Recompute the drawn icon row whenever EITHER the user's saved order
        // OR the set of apps with an active notification changes.
        scope.launch {
            prefs.iconOrder.collect { savedOrder ->
                val active = NotificationIconListenerService.activePackages.value
                overlayView?.iconOrder = savedOrder.filter { it in active }
            }
        }
        scope.launch {
            NotificationIconListenerService.activePackages.collect { active ->
                // read the latest saved order synchronously via a fresh collect
                // is unnecessary here since prefs.iconOrder's collector above
                // re-fires on its own cadence; this second collector exists so
                // a notification appearing/disappearing updates the row even
                // if iconOrder itself hasn't changed. Re-filter the last known
                // overlay list against the new active set.
                overlayView?.iconOrder = overlayView?.iconOrder?.filter { it in active } ?: emptyList()
            }
        }
    }

    private fun buildForegroundNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "StatusBar+ overlay", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StatusBar+ is running")
            .setContentText("Custom status bar overlay is active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "statusbarplus_overlay"
        private const val NOTIF_ID = 42
    }
}
