package com.statusbarplus.app.data

enum class ClockStyle(val label: String) {
    SYSTEM("Follow phone setting"),
    DEFAULT_24H("24-hour · 14:05"),
    DEFAULT_12H("12-hour · 2:05 PM"),
    SECONDS_24H("With seconds · 14:05:32"),
    MINIMAL_DOTS("Minimal · 14·05"),
    WITH_DATE("With date · Tue 3 · 14:05"),
}

enum class BatteryStyle(val label: String) {
    CLASSIC_ICON("Icon + percent"),
    ICON_ONLY("Icon only"),
    PERCENT_TEXT("Percent only"),
    CIRCLE_RING("Ring"),
    MINIMAL_DOT("Dot (red when low)"),
}

/** Bar background colors (ARGB). Opaque on purpose: the strip has to fully
 *  cover the real status bar underneath it. */
val barColors: List<Pair<String, Int>> = listOf(
    "Black" to 0xFF000000.toInt(),
    "Charcoal" to 0xFF1C1C1E.toInt(),
    "Navy" to 0xFF0D1B2A.toInt(),
    "Plum" to 0xFF2A1033.toInt(),
    "Forest" to 0xFF0F2A1D.toInt(),
)

/** Text/icon colors (ARGB). */
val contentColors: List<Pair<String, Int>> = listOf(
    "White" to 0xFFFFFFFF.toInt(),
    "Cyan" to 0xFF18FFFF.toInt(),
    "Mint" to 0xFF69F0AE.toInt(),
    "Amber" to 0xFFFFD740.toInt(),
    "Pink" to 0xFFFF80AB.toInt(),
)

data class BarSettings(
    val enabled: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.SYSTEM,
    val batteryStyle: BatteryStyle = BatteryStyle.CLASSIC_ICON,
    val barColor: Int = barColors[0].second,
    val contentColor: Int = contentColors[0].second,
    val showConnection: Boolean = true,
    val hideInLandscape: Boolean = true,
    val maxIcons: Int = 5,
    val iconOrder: List<String> = emptyList(),
)
