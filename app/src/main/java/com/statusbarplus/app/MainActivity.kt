package com.statusbarplus.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.statusbarplus.app.data.PrefsStore
import com.statusbarplus.app.overlay.OverlayService
import com.statusbarplus.app.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsStore(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        prefs = prefs,
                        hasOverlayPermission = { Settings.canDrawOverlays(this) },
                        onRequestOverlayPermission = {
                            startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                            )
                        },
                        onRequestNotificationAccess = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onToggleOverlay = { enable -> toggleOverlayService(enable) }
                    )
                }
            }
        }
    }

    private fun toggleOverlayService(enable: Boolean) {
        val intent = Intent(this, OverlayService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } else {
            stopService(intent)
        }
    }
}
