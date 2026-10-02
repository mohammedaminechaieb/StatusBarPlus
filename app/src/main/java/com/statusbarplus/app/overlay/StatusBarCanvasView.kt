package com.statusbarplus.app.overlay

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import com.statusbarplus.app.data.BarSettings
import com.statusbarplus.app.data.BatteryStyle
import com.statusbarplus.app.data.ClockStyle
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * The replacement status bar strip, laid out like the stock one so a
 * centered camera cutout never covers anything: clock + notification
 * icons on the left, connection + battery on the right.
 *
 * [preview] = true draws sample data (used inside the app), otherwise it
 * listens to the real battery and network.
 */
class StatusBarCanvasView(context: Context, private val preview: Boolean = false) : View(context) {

    var settings: BarSettings = BarSettings()
        set(value) {
            if (field.contentColor != value.contentColor) tintIcons(value.contentColor)
            field = value
            invalidate()
        }

    /** Notification icons in display order (already limited/ordered by the caller). */
    var icons: List<Drawable> = emptyList()
        set(value) {
            field = value
            tintIcons(settings.contentColor)
            invalidate()
        }

    private val dp = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            // Align to the next whole second so the clock flips exactly on time.
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000)
        }
    }

    // ---- Live data ------------------------------------------------------

    private var batteryPercent = 76
    private var charging = false
    private enum class Conn { WIFI, CELL, NONE }
    private var conn = Conn.WIFI
    private var wifiLevel = 3

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) batteryPercent = level * 100 / scale
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            invalidate()
        }
    }

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            conn = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Conn.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Conn.CELL
                else -> Conn.NONE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && conn == Conn.WIFI) {
                val rssi = caps.signalStrength
                wifiLevel = when {
                    rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 3
                    rssi >= -60 -> 3
                    rssi >= -70 -> 2
                    rssi >= -80 -> 1
                    else -> 0
                }
            }
            postInvalidate()
        }

        override fun onLost(network: Network) {
            conn = Conn.NONE
            postInvalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tick)
        if (!preview) {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            conn = Conn.NONE
            runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback) }
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        if (!preview) {
            runCatching { context.unregisterReceiver(batteryReceiver) }
            runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
        }
        super.onDetachedFromWindow()
    }

    // ---- Drawing --------------------------------------------------------

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private fun tintIcons(color: Int) {
        icons.forEach { it.mutate().setTint(color) }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = settings
        canvas.drawColor(s.barColor)

        val h = height.toFloat()
        val textSize = min(14 * dp, h * 0.5f)
        textPaint.textSize = textSize
        textPaint.color = s.contentColor
        val cy = h / 2f
        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        val side = 16 * dp

        // Left: clock then notification icons.
        val clock = formatClock(s.clockStyle)
        canvas.drawText(clock, side, baseline, textPaint)
        var x = side + textPaint.measureText(clock) + 10 * dp
        val iconSize = (textSize * 1.15f).toInt()
        icons.forEach { d ->
            val top = (cy - iconSize / 2f).toInt()
            d.setBounds(x.toInt(), top, x.toInt() + iconSize, top + iconSize)
            d.draw(canvas)
            x += iconSize + 6 * dp
        }

        // Right: battery, then connection to its left.
        var right = width - side
        right = drawBattery(canvas, right, cy, baseline, textSize, s)
        if (s.showConnection) drawConnection(canvas, right - 8 * dp, cy, textSize, s.contentColor)
    }

    private fun formatClock(style: ClockStyle): String {
        val now = Calendar.getInstance()
        val h24 = now.get(Calendar.HOUR_OF_DAY)
        val m = now.get(Calendar.MINUTE)
        return when (style) {
            ClockStyle.SYSTEM -> DateFormat.getTimeFormat(context).format(now.time)
            ClockStyle.DEFAULT_24H -> String.format(Locale.US, "%02d:%02d", h24, m)
            ClockStyle.DEFAULT_12H -> DateFormat.format("h:mm a", now).toString()
            ClockStyle.SECONDS_24H -> String.format(Locale.US, "%02d:%02d:%02d", h24, m, now.get(Calendar.SECOND))
            ClockStyle.MINIMAL_DOTS -> String.format(Locale.US, "%02d·%02d", h24, m)
            ClockStyle.WITH_DATE -> DateFormat.format("EEE d", now).toString() + " · " +
                (if (DateFormat.is24HourFormat(context)) String.format(Locale.US, "%02d:%02d", h24, m) else DateFormat.format("h:mm", now))
        }
    }

    /** Draws the battery ending at [right]; returns the x where it starts. */
    private fun drawBattery(canvas: Canvas, right: Float, cy: Float, baseline: Float, textSize: Float, s: BarSettings): Float {
        val low = batteryPercent <= 15 && !charging
        val levelColor = when {
            low -> Color.rgb(255, 82, 82)
            charging -> Color.rgb(105, 240, 174)
            else -> s.contentColor
        }
        return when (s.batteryStyle) {
            BatteryStyle.PERCENT_TEXT -> {
                val label = "$batteryPercent%" + if (charging) " ⚡" else ""
                val w = textPaint.measureText(label)
                canvas.drawText(label, right - w, baseline, textPaint)
                right - w
            }
            BatteryStyle.CLASSIC_ICON, BatteryStyle.ICON_ONLY -> {
                var r = right
                if (s.batteryStyle == BatteryStyle.CLASSIC_ICON) {
                    val label = "$batteryPercent%"
                    val w = textPaint.measureText(label)
                    canvas.drawText(label, r - w, baseline, textPaint)
                    r -= w + 5 * dp
                }
                drawBatteryIcon(canvas, r, cy, textSize, s.contentColor, levelColor)
            }
            BatteryStyle.CIRCLE_RING -> {
                val radius = textSize * 0.5f
                val cx = right - radius
                strokePaint.strokeWidth = 2.5f * dp
                strokePaint.color = Color.argb(70, Color.red(s.contentColor), Color.green(s.contentColor), Color.blue(s.contentColor))
                canvas.drawCircle(cx, cy, radius, strokePaint)
                strokePaint.color = levelColor
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, 360f * batteryPercent / 100f, false, strokePaint)
                right - radius * 2
            }
            BatteryStyle.MINIMAL_DOT -> {
                fillPaint.color = if (low) Color.rgb(255, 82, 82) else levelColor
                val r = 4 * dp
                canvas.drawCircle(right - r, cy, r, fillPaint)
                right - r * 2
            }
        }
    }

    private fun drawBatteryIcon(canvas: Canvas, right: Float, cy: Float, textSize: Float, outline: Int, level: Int): Float {
        val bodyH = textSize * 0.78f
        val bodyW = bodyH * 1.9f
        val nubW = 2 * dp
        val left = right - nubW - bodyW
        val top = cy - bodyH / 2
        strokePaint.strokeWidth = 1.4f * dp
        strokePaint.color = outline
        canvas.drawRoundRect(left, top, left + bodyW, top + bodyH, 3 * dp, 3 * dp, strokePaint)
        fillPaint.color = outline
        canvas.drawRoundRect(left + bodyW, cy - bodyH / 4, right, cy + bodyH / 4, dp, dp, fillPaint)
        val inset = 2.4f * dp
        fillPaint.color = level
        val fillW = (bodyW - inset * 2) * (batteryPercent / 100f)
        canvas.drawRoundRect(left + inset, top + inset, left + inset + fillW, top + bodyH - inset, 1.5f * dp, 1.5f * dp, fillPaint)
        if (charging) {
            // Lightning bolt across the body.
            val bx = left + bodyW / 2
            val bolt = Path().apply {
                moveTo(bx + bodyH * 0.12f, top - dp)
                lineTo(bx - bodyH * 0.28f, cy + bodyH * 0.08f)
                lineTo(bx, cy + bodyH * 0.08f)
                lineTo(bx - bodyH * 0.12f, top + bodyH + dp)
                lineTo(bx + bodyH * 0.28f, cy - bodyH * 0.08f)
                lineTo(bx, cy - bodyH * 0.08f)
                close()
            }
            fillPaint.color = Color.WHITE
            canvas.drawPath(bolt, fillPaint)
        }
        return left
    }

    private fun drawConnection(canvas: Canvas, right: Float, cy: Float, textSize: Float, color: Int) {
        val size = textSize * 1.05f
        strokePaint.strokeWidth = 1.8f * dp
        when (conn) {
            Conn.WIFI, Conn.NONE -> {
                // Wi-Fi: dot + three arcs, unlit arcs dimmed by signal level.
                val cx = right - size / 2
                val bottom = cy + size * 0.42f
                fillPaint.color = color
                canvas.drawCircle(cx, bottom - dp, 1.6f * dp, fillPaint)
                for (i in 1..3) {
                    val r = size * 0.22f * i
                    val lit = conn == Conn.WIFI && i <= wifiLevel
                    strokePaint.color = if (lit) color else Color.argb(70, Color.red(color), Color.green(color), Color.blue(color))
                    canvas.drawArc(cx - r, bottom - r, cx + r, bottom + r, 225f, 90f, false, strokePaint)
                }
                if (conn == Conn.NONE) {
                    strokePaint.color = color
                    canvas.drawLine(cx - size * 0.4f, cy - size * 0.4f, cx + size * 0.4f, cy + size * 0.4f, strokePaint)
                }
            }
            Conn.CELL -> {
                // Mobile data: up/down arrows (signal strength needs a phone permission we don't ask for).
                strokePaint.color = color
                val ux = right - size * 0.65f
                val dx = right - size * 0.25f
                val t = cy - size * 0.4f
                val b = cy + size * 0.4f
                canvas.drawLine(ux, b, ux, t, strokePaint)
                canvas.drawLine(ux - size * 0.15f, t + size * 0.15f, ux, t, strokePaint)
                canvas.drawLine(ux + size * 0.15f, t + size * 0.15f, ux, t, strokePaint)
                canvas.drawLine(dx, t, dx, b, strokePaint)
                canvas.drawLine(dx - size * 0.15f, b - size * 0.15f, dx, b, strokePaint)
                canvas.drawLine(dx + size * 0.15f, b - size * 0.15f, dx, b, strokePaint)
            }
        }
    }
}
