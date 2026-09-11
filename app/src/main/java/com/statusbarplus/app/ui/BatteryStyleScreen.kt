package com.statusbarplus.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.statusbarplus.app.data.BatteryStyle
import com.statusbarplus.app.data.PrefsStore
import kotlinx.coroutines.launch

private val labels = mapOf(
    BatteryStyle.PERCENT_TEXT to "Percent text (\"82%\", ⚡ when charging)",
    BatteryStyle.CLASSIC_ICON to "Percent only, no charging glyph",
    BatteryStyle.CIRCLE_RING to "Circular progress ring",
    BatteryStyle.MINIMAL_DOT to "Minimal dot (red under 20%)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryStyleScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val current by prefs.batteryStyle.collectAsState(initial = BatteryStyle.PERCENT_TEXT)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Battery style") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            labels.forEach { (style, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = current == style) {
                            scope.launch { prefs.setBatteryStyle(style) }
                        }
                        .padding(16.dp)
                ) {
                    RadioButton(selected = current == style, onClick = { scope.launch { prefs.setBatteryStyle(style) } })
                    Spacer(Modifier.width(12.dp))
                    Text(label)
                }
            }
        }
    }
}
