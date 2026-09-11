package com.statusbarplus.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real status bar icons come from active notifications. We can't relocate
 * the system's own icons (no API for that without root), so instead we
 * mirror "which apps currently have an active notification" and let the
 * overlay draw ITS OWN icon row in the user's chosen order, visually
 * replacing what's underneath. This service is just the data source.
 */
class NotificationIconListenerService : NotificationListenerService() {

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())
        val activePackages: StateFlow<Set<String>> = _activePackages.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _activePackages.value = activeNotifications?.map { it.packageName }?.toSet() ?: emptySet()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        _activePackages.value = _activePackages.value + sbn.packageName
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val stillActive = activeNotifications?.map { it.packageName }?.toSet() ?: emptySet()
        _activePackages.value = stillActive
    }
}
