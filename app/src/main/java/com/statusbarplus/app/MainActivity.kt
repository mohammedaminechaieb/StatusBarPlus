package com.statusbarplus.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.ui.HomeScreen
import com.statusbarplus.app.ui.IconReorderScreen

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsStore

    // Refreshed in onResume so the checklist updates when the user returns
    // from the system settings screens.
    private var accessibilityOn by mutableStateOf(false)
    private var notificationAccessOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = PrefsStore(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFF7FD8BE), background = Color(0xFF0E1110), surface = Color(0xFF0E1110))
                } else {
                    lightColorScheme(primary = Color(0xFF00796B))
                }
            ) {
                var reorder by remember { mutableStateOf(false) }
                BackHandler(enabled = reorder) { reorder = false }
                if (reorder) {
                    IconReorderScreen(prefs = prefs, onBack = { reorder = false })
                } else {
                    HomeScreen(
                        prefs = prefs,
                        accessibilityOn = accessibilityOn,
                        notificationAccessOn = notificationAccessOn,
                        onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onOpenNotificationAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        onOpenReorder = { reorder = true }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityOn = isEnabledIn(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "overlay.StatusBarAccessibilityService")
        notificationAccessOn = isEnabledIn("enabled_notification_listeners", "service.NotificationIconListenerService")
    }

    /** Checks a colon-separated list of enabled components in Settings.Secure. */
    private fun isEnabledIn(key: String, className: String): Boolean {
        val raw = Settings.Secure.getString(contentResolver, key) ?: return false
        val short = "$packageName/.$className"
        val full = "$packageName/$packageName.$className"
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(raw) }
        while (splitter.hasNext()) {
            val s = splitter.next()
            if (s.equals(short, true) || s.equals(full, true)) return true
        }
        return false
    }
}
