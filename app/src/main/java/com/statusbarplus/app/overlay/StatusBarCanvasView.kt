package com.statusbarplus.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import com.statusbarplus.app.data.BatteryStyle
import com.statusbarplus.app.data.ClockStyle
import java.util.Calendar

/**
 * A thin full-width strip drawn at the top of the screen, visually
 * replacing the real status bar. Everything here is plain Canvas drawing
 * so it's cheap enough to redraw every second for the clock tick.
 */
class StatusBarCanvasView(context: Context) : View(context) {

    var clockStyle: ClockStyle = ClockStyle.DEFAULT_24H
    var batteryStyle: BatteryStyle = BatteryStyle.PERCENT_TEXT
    var iconOrder: List<String> = emptyList()
        set(value) { field = value; invalidate() }

    private val pm: PackageManager = context.packageManager
    private val handler = Handler(Looper.getMainLooper())
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 28f
    }
    private val backgroundPaint = Paint().apply { color = Color.BLACK }

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val centerY = height / 2f + textPaint.textSize / 3f

        // --- Clock, left-aligned ---
        canvas.drawText(formatClock(), 24f, centerY, textPaint)

        // --- Icon row, centered ---
        drawIconRow(canvas, centerY)

        // --- Battery, right-aligned ---
        drawBattery(canvas, centerY)
    }

    private fun formatClock(): String {
        val now = Calendar.getInstance()
        return when (clockStyle) {
            ClockStyle.DEFAULT_24H -> String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            ClockStyle.DEFAULT_12H -> DateFormat.format("h:mm a", now).toString()
            ClockStyle.SECONDS_24H -> String.format("%02d:%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))
            ClockStyle.MINIMAL_DOTS -> String.format("%02d·%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }
    }

    private fun drawIconRow(canvas: Canvas, centerY: Float) {
        var x = width / 2f - (iconOrder.size * 44f) / 2f
        for (pkg in iconOrder) {
            val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull() as? BitmapDrawable
            if (icon != null) {
                val bmp = Bitmap.createScaledBitmap(icon.bitmap, 32, 32, true)
                canvas.drawBitmap(bmp, x, centerY - 32f, null)
            }
            x += 44f
        }
    }

    private fun drawBattery(canvas: Canvas, centerY: Float) {
        val state = readBatteryState(context)
        val label = when (batteryStyle) {
            BatteryStyle.PERCENT_TEXT -> "${state.percent}%${if (state.isCharging) " ⚡" else ""}"
            BatteryStyle.CLASSIC_ICON -> "${state.percent}%"
            BatteryStyle.CIRCLE_RING -> "" // drawn as a ring below, no text
            BatteryStyle.MINIMAL_DOT -> "" // drawn as a dot below
        }

        if (label.isNotEmpty()) {
            val textWidth = textPaint.measureText(label)
            canvas.drawText(label, width - textWidth - 24f, centerY, textPaint)
        }

        when (batteryStyle) {
            BatteryStyle.CIRCLE_RING -> drawBatteryRing(canvas, state.percent, width - 50f, height / 2f)
            BatteryStyle.MINIMAL_DOT -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (state.percent < 20) Color.RED else Color.GREEN
                }
                canvas.drawCircle(width - 30f, height / 2f, 8f, dotPaint)
            }
            else -> Unit
        }
    }

    private fun drawBatteryRing(canvas: Canvas, percent: Int, cx: Float, cy: Float) {
        val radius = 16f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.DKGRAY
        }
        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.GREEN
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)
        val sweep = 360f * (percent / 100f)
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, sweep, false, fgPaint)
    }
}
