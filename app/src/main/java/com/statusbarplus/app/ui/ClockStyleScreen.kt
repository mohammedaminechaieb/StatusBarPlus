package com.statusbarplus.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.statusbarplus.app.data.ClockStyle
import com.statusbarplus.app.data.PrefsStore
import kotlinx.coroutines.launch

private val labels = mapOf(
    ClockStyle.DEFAULT_24H to "24-hour (14:05)",
    ClockStyle.DEFAULT_12H to "12-hour (2:05 PM)",
    ClockStyle.SECONDS_24H to "24-hour with seconds (14:05:32)",
    ClockStyle.MINIMAL_DOTS to "Minimal (14·05)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStyleScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val current by prefs.clockStyle.collectAsState(initial = ClockStyle.DEFAULT_24H)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Clock style") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            labels.forEach { (style, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = current == style) {
                            scope.launch { prefs.setClockStyle(style) }
                        }
                        .padding(16.dp)
                ) {
                    RadioButton(selected = current == style, onClick = { scope.launch { prefs.setClockStyle(style) } })
                    Spacer(Modifier.width(12.dp))
                    Text(label)
                }
            }
        }
    }
}
