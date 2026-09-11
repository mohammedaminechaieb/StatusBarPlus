package com.statusbarplus.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "statusbarplus_prefs")

/**
 * Single source of truth for the overlay's appearance. MainActivity writes
 * to this when the user reorders icons or picks a style; OverlayService
 * reads it (via Flow) so changes apply live without restarting the overlay.
 */
class PrefsStore(private val context: Context) {

    private object Keys {
        val ICON_ORDER = stringPreferencesKey("icon_order")       // comma-separated package names
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val BATTERY_STYLE = stringPreferencesKey("battery_style")
        val OVERLAY_ENABLED = stringPreferencesKey("overlay_enabled")
    }

    val iconOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.ICON_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val clockStyle: Flow<ClockStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: ClockStyle.DEFAULT_24H
    }

    val batteryStyle: Flow<BatteryStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.BATTERY_STYLE]?.let { runCatching { BatteryStyle.valueOf(it) }.getOrNull() } ?: BatteryStyle.PERCENT_TEXT
    }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OVERLAY_ENABLED] == "true"
    }

    suspend fun setIconOrder(packages: List<String>) {
        context.dataStore.edit { it[Keys.ICON_ORDER] = packages.joinToString(",") }
    }

    suspend fun setClockStyle(style: ClockStyle) {
        context.dataStore.edit { it[Keys.CLOCK_STYLE] = style.name }
    }

    suspend fun setBatteryStyle(style: BatteryStyle) {
        context.dataStore.edit { it[Keys.BATTERY_STYLE] = style.name }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OVERLAY_ENABLED] = if (enabled) "true" else "false" }
    }
}
