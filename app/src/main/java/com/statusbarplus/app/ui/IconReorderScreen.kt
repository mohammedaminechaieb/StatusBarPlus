package com.statusbarplus.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.service.NotificationIconListenerService
import kotlinx.coroutines.launch

/**
 * Reordering is expressed as: "of the apps that currently have a
 * notification, in what order should their icons appear". We seed the
 * list from active notifications and let the user drag the priority up
 * or down with arrow buttons (keeps this dependency-free — no external
 * drag-and-drop library needed for v0.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconReorderScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pm = context.packageManager

    val savedOrder by prefs.iconOrder.collectAsState(initial = emptyList())
    val activePackages by NotificationIconListenerService.activePackages.collectAsState(initial = emptySet())

    // Merge: saved order first (dropping anything no longer active), then
    // any newly-active packages appended at the end.
    val orderedList = remember(savedOrder, activePackages) {
        val known = savedOrder.filter { it in activePackages }
        val new = activePackages.filter { it !in savedOrder }
        known + new
    }

    fun persist(newOrder: List<String>) {
        scope.launch { prefs.setIconOrder(newOrder) }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Reorder icons") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        if (orderedList.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No apps with active notifications right now.\nTrigger a notification to see it here.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(orderedList, key = { it }) { pkg ->
                    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                    val index = orderedList.indexOf(pkg)

                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text(pkg) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newOrder = orderedList.toMutableList()
                                            newOrder.removeAt(index); newOrder.add(index - 1, pkg)
                                            persist(newOrder)
                                        }
                                    },
                                    enabled = index > 0
                                ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }

                                IconButton(
                                    onClick = {
                                        if (index < orderedList.lastIndex) {
                                            val newOrder = orderedList.toMutableList()
                                            newOrder.removeAt(index); newOrder.add(index + 1, pkg)
                                            persist(newOrder)
                                        }
                                    },
                                    enabled = index < orderedList.lastIndex
                                ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
