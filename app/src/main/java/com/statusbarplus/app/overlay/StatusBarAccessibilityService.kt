package com.statusbarplus.app.overlay

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.statusbarplus.app.data.BarSettings
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.data.orderedByPriority
import com.statusbarplus.app.service.NotificationIconListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Hosts the replacement status bar. It's an accessibility service purely
 * because TYPE_ACCESSIBILITY_OVERLAY is the only non-root window type that
 * draws ABOVE the real status bar — a TYPE_APPLICATION_OVERLAY window sits
 * underneath it on modern Android, so the system icons would show through.
 * Bonus: accessibility services restart on their own after a reboot and
 * need no foreground-service notification.
 *
 * The window is FLAG_NOT_TOUCHABLE, so pulling down the notification shade
 * still works exactly as normal.
 */
class StatusBarAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var bar: StatusBarCanvasView? = null
    private var settings = BarSettings()
    private val iconCache = HashMap<Icon, Drawable?>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        scope.launch {
            combine(PrefsStore(applicationContext).settings, NotificationIconListenerService.activeIcons) { s, icons -> s to icons }
                .collect { (s, icons) ->
                    settings = s
                    updateVisibility()
                    bar?.settings = s
                    bar?.icons = orderedByPriority(icons, s.iconOrder)
                        .take(s.maxIcons)
                        .mapNotNull { (_, icon) -> icon?.let { iconCache.getOrPut(it) { runCatching { it.loadDrawable(this@StatusBarAccessibilityService) }.getOrNull() } } }
                    // Notifications come and go all day — don't let the cache grow forever.
                    if (iconCache.size > 64) iconCache.clear()
                }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateVisibility()
    }

    private fun updateVisibility() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val shouldShow = settings.enabled && !(landscape && settings.hideInLandscape)
        if (shouldShow && bar == null) addBar() else if (!shouldShow && bar != null) removeBar()
    }

    private fun addBar() {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        val height = if (id > 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val view = StatusBarCanvasView(this).also { it.settings = settings }
        runCatching {
            windowManager.addView(view, params)
            bar = view
        }
    }

    private fun removeBar() {
        bar?.let { runCatching { windowManager.removeView(it) } }
        bar = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeBar()
        scope.cancel()
        super.onDestroy()
    }
}
