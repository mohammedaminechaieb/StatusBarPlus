package com.statusbarplus.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.statusbarplus.app.data.BarSettings
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.service.NotificationIconListenerService
import kotlinx.coroutines.launch

/**
 * Priority order for notification icons. The list keeps every app you've
 * ordered before (even when it has nothing showing right now, so the order
 * survives), plus any app that currently has a notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconReorderScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()

    val settings by prefs.settings.collectAsState(initial = BarSettings())
    val active by NotificationIconListenerService.activeIcons.collectAsState()
    val list = remember(settings.iconOrder, active) {
        settings.iconOrder + active.keys.filter { it !in settings.iconOrder }
    }

    fun persist(newOrder: List<String>) {
        scope.launch { prefs.update { it.copy(iconOrder = newOrder) } }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Icon order") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        if (list.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No apps yet.\nApps appear here as soon as they post a notification (make sure notification access is on).",
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    "Top of the list = leftmost icon. Apps without a notification right now are dimmed but keep their place.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
            itemsIndexed(list, key = { _, pkg -> pkg }) { index, pkg ->
                val label = remember(pkg) { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg) }
                val icon = remember(pkg) { runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
                val showing = pkg in active
                ListItem(
                    leadingContent = {
                        if (icon != null) Image(icon, null, Modifier.size(36.dp), alpha = if (showing) 1f else 0.4f)
                    },
                    headlineContent = { Text(label) },
                    supportingContent = { Text(if (showing) "Showing now" else "No notification right now") },
                    trailingContent = {
                        Row {
                            IconButton(enabled = index > 0, onClick = {
                                persist(list.toMutableList().apply { removeAt(index); add(index - 1, pkg) })
                            }) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                            IconButton(enabled = index < list.lastIndex, onClick = {
                                persist(list.toMutableList().apply { removeAt(index); add(index + 1, pkg) })
                            }) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                            if (pkg in settings.iconOrder && !showing) {
                                IconButton(onClick = { persist(settings.iconOrder - pkg) }) { Icon(Icons.Default.Close, "Forget") }
                            }
                        }
                    }
                )
            }
        }
    }
}
