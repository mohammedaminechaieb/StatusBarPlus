package com.statusbarplus.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.statusbarplus.app.data.*
import com.statusbarplus.app.overlay.StatusBarCanvasView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    prefs: PrefsStore,
    accessibilityOn: Boolean,
    notificationAccessOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenReorder: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val s by prefs.settings.collectAsState(initial = BarSettings())
    fun update(t: (BarSettings) -> BarSettings) { scope.launch { prefs.update(t) } }

    Scaffold(topBar = { LargeTopAppBar(title = { Text("StatusBar+") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BarPreview(s)

            // ---- Setup / on-off ---------------------------------------
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    StepRow(
                        done = accessibilityOn,
                        title = "Turn on the StatusBar+ service",
                        subtitle = "Accessibility → StatusBar+. Needed to draw above the real status bar; it can't read your screen.",
                        action = "Open", onAction = onOpenAccessibility
                    )
                    StepRow(
                        done = notificationAccessOn,
                        title = "Allow notification access",
                        subtitle = "So your bar can show the same app icons the real one does.",
                        action = "Open", onAction = onOpenNotificationAccess
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Custom status bar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    !accessibilityOn -> "Finish step 1 first"
                                    s.enabled -> "On — swipe down still opens your notifications"
                                    else -> "Off"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = s.enabled && accessibilityOn, enabled = accessibilityOn, onCheckedChange = { on -> update { it.copy(enabled = on) } })
                    }
                }
            }

            // ---- Clock ------------------------------------------------
            Section("Clock") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockStyle.entries.forEach { style ->
                        FilterChip(selected = s.clockStyle == style, onClick = { update { it.copy(clockStyle = style) } }, label = { Text(style.label) })
                    }
                }
            }

            // ---- Battery ----------------------------------------------
            Section("Battery") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BatteryStyle.entries.forEach { style ->
                        FilterChip(selected = s.batteryStyle == style, onClick = { update { it.copy(batteryStyle = style) } }, label = { Text(style.label) })
                    }
                }
            }

            // ---- Colors -----------------------------------------------
            Section("Colors") {
                Text("Background", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Swatches(barColors, s.barColor) { c -> update { it.copy(barColor = c) } }
                Spacer(Modifier.height(12.dp))
                Text("Text & icons", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Swatches(contentColors, s.contentColor) { c -> update { it.copy(contentColor = c) } }
            }

            // ---- Icons & behavior -------------------------------------
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ListItem(
                        headlineContent = { Text("Notification icon order") },
                        supportingContent = { Text("Choose which apps' icons come first") },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable(onClick = onOpenReorder),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Show up to ${s.maxIcons} icons", style = MaterialTheme.typography.bodyMedium)
                        Slider(value = s.maxIcons.toFloat(), valueRange = 0f..8f, steps = 7, onValueChange = { v -> update { it.copy(maxIcons = v.toInt()) } })
                    }
                    SwitchItem("Show Wi-Fi / mobile data", s.showConnection) { on -> update { it.copy(showConnection = on) } }
                    SwitchItem("Hide in landscape", s.hideInLandscape, "Games and videos keep the real (hidden) bar") { on -> update { it.copy(hideInLandscape = on) } }
                }
            }

            Text(
                "How it works: Android doesn't let apps change the real status bar without root, so StatusBar+ draws a look-alike strip on top of it. The real one keeps working underneath.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** A live StatusBarCanvasView with sample icons, framed like a phone top. */
@Composable
private fun BarPreview(s: BarSettings) {
    val context = LocalContext.current
    val sampleIcons = remember {
        listOf(android.R.drawable.stat_notify_chat, android.R.drawable.ic_dialog_email, android.R.drawable.stat_sys_download)
            .mapNotNull { ContextCompat.getDrawable(context, it)?.mutate() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                factory = { StatusBarCanvasView(it, preview = true) },
                update = { v ->
                    v.settings = s
                    v.icons = sampleIcons.take(s.maxIcons)
                }
            )
            Text("Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp).align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun StepRow(done: Boolean, title: String, subtitle: String, action: String, onAction: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { if (!done) FilledTonalButton(onClick = onAction) { Text(action) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Swatches(options: List<Pair<String, Int>>, selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (_, argb) ->
            val isSel = argb == selected
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onPick(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSel) Icon(Icons.Default.Check, null, tint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SwitchItem(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
