package com.statusbarplus.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.statusbarplus.app.data.PrefsStore
import kotlinx.coroutines.launch

private enum class Screen { HOME, REORDER, CLOCK, BATTERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: PrefsStore,
    hasOverlayPermission: () -> Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val scope = rememberCoroutineScope()
    val overlayEnabled by prefs.overlayEnabled.collectAsState(initial = false)

    when (screen) {
        Screen.REORDER -> IconReorderScreen(prefs = prefs, onBack = { screen = Screen.HOME })
        Screen.CLOCK -> ClockStyleScreen(prefs = prefs, onBack = { screen = Screen.HOME })
        Screen.BATTERY -> BatteryStyleScreen(prefs = prefs, onBack = { screen = Screen.HOME })
        Screen.HOME -> Scaffold(topBar = { TopAppBar(title = { Text("StatusBar+") }) }) { padding ->
            Column(
                Modifier.padding(padding).padding(20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!hasOverlayPermission()) {
                    Text("Step 1 — allow StatusBar+ to draw over other apps:")
                    Button(onClick = onRequestOverlayPermission) { Text("Grant \"draw over other apps\"") }
                } else {
                    Text("✓ Overlay permission granted")
                }

                Text("Step 2 — grant notification access (needed to detect which apps currently have active icons):")
                Button(onClick = onRequestNotificationAccess) { Text("Grant notification access") }

                Divider(Modifier.padding(vertical = 8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overlay is ${if (overlayEnabled) "ON" else "OFF"}", modifier = Modifier.weight(1f))
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { prefs.setOverlayEnabled(enabled) }
                            onToggleOverlay(enabled)
                        },
                        enabled = hasOverlayPermission()
                    )
                }

                Divider(Modifier.padding(vertical = 8.dp))

                Button(onClick = { screen = Screen.REORDER }, modifier = Modifier.fillMaxWidth()) { Text("Reorder icons") }
                Button(onClick = { screen = Screen.CLOCK }, modifier = Modifier.fillMaxWidth()) { Text("Clock style") }
                Button(onClick = { screen = Screen.BATTERY }, modifier = Modifier.fillMaxWidth()) { Text("Battery style") }
            }
        }
    }
}
