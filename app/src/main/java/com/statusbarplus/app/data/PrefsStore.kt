package com.statusbarplus.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "statusbarplus_prefs")

/**
 * Single source of truth for the bar's appearance. The app writes it; the
 * accessibility service collects it so changes apply live.
 */
class PrefsStore(private val context: Context) {

    private object Keys {
        val ICON_ORDER = stringPreferencesKey("icon_order") // comma-separated package names
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val BATTERY_STYLE = stringPreferencesKey("battery_style")
        val OVERLAY_ENABLED = stringPreferencesKey("overlay_enabled") // string for v0.1 compatibility
        val BAR_COLOR = intPreferencesKey("bar_color")
        val CONTENT_COLOR = intPreferencesKey("content_color")
        val SHOW_CONNECTION = booleanPreferencesKey("show_connection")
        val HIDE_LANDSCAPE = booleanPreferencesKey("hide_in_landscape")
        val MAX_ICONS = intPreferencesKey("max_icons")
    }

    val settings: Flow<BarSettings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): BarSettings {
        val d = BarSettings()
        return BarSettings(
            enabled = this[Keys.OVERLAY_ENABLED] == "true",
            clockStyle = this[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: d.clockStyle,
            batteryStyle = this[Keys.BATTERY_STYLE]?.let { runCatching { BatteryStyle.valueOf(it) }.getOrNull() } ?: d.batteryStyle,
            barColor = this[Keys.BAR_COLOR] ?: d.barColor,
            contentColor = this[Keys.CONTENT_COLOR] ?: d.contentColor,
            showConnection = this[Keys.SHOW_CONNECTION] ?: d.showConnection,
            hideInLandscape = this[Keys.HIDE_LANDSCAPE] ?: d.hideInLandscape,
            maxIcons = this[Keys.MAX_ICONS] ?: d.maxIcons,
            iconOrder = this[Keys.ICON_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    suspend fun update(transform: (BarSettings) -> BarSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings())
            p[Keys.OVERLAY_ENABLED] = if (s.enabled) "true" else "false"
            p[Keys.CLOCK_STYLE] = s.clockStyle.name
            p[Keys.BATTERY_STYLE] = s.batteryStyle.name
            p[Keys.BAR_COLOR] = s.barColor
            p[Keys.CONTENT_COLOR] = s.contentColor
            p[Keys.SHOW_CONNECTION] = s.showConnection
            p[Keys.HIDE_LANDSCAPE] = s.hideInLandscape
            p[Keys.MAX_ICONS] = s.maxIcons
            p[Keys.ICON_ORDER] = s.iconOrder.joinToString(",")
        }
    }
}

/**
 * Applies the user's priority order: packages in [order] come first in
 * that order, anything else keeps its arrival order after them.
 */
fun <T> orderedByPriority(active: Map<String, T>, order: List<String>): List<Pair<String, T>> {
    val first = order.mapNotNull { pkg -> active[pkg]?.let { pkg to it } }
    val rest = active.filterKeys { it !in order }.toList()
    return first + rest
}
