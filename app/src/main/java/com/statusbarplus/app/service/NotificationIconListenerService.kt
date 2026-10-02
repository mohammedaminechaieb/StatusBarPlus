package com.statusbarplus.app.service

import android.app.NotificationManager
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real status bar icons come from active notifications. We can't move the
 * system's own icons without root, so this mirrors "which apps have a
 * notification right now" together with each one's SMALL icon — the
 * monochrome glyph the real status bar draws — so the overlay can show
 * the same icons in the user's chosen order.
 */
class NotificationIconListenerService : NotificationListenerService() {

    companion object {
        private val _activeIcons = MutableStateFlow<Map<String, Icon?>>(emptyMap())

        /** package -> small icon, in arrival order (oldest first). */
        val activeIcons: StateFlow<Map<String, Icon?>> = _activeIcons.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
        rebuild()
    }

    override fun onListenerDisconnected() {
        _connected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = rebuild()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = rebuild()

    private fun rebuild() {
        val all = runCatching { activeNotifications }.getOrNull() ?: return
        val ranking = currentRanking
        val icons = LinkedHashMap<String, Icon?>()
        all.sortedBy { it.postTime }.forEach { sbn ->
            if (sbn.packageName == packageName) return@forEach
            // The real status bar hides "minimized" (IMPORTANCE_MIN) notifications.
            val r = Ranking()
            if (ranking?.getRanking(sbn.key, r) == true && r.importance <= NotificationManager.IMPORTANCE_MIN) return@forEach
            if (sbn.packageName !in icons) icons[sbn.packageName] = sbn.notification.smallIcon
        }
        _activeIcons.value = icons
    }
}
