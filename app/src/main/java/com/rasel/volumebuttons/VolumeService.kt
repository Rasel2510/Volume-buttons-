package com.rasel.volumebuttons

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent

/**
 * Keeps the bubble on screen. Turned on once in Accessibility settings, then
 * Android keeps it running over every app and brings it back after a restart.
 */
class VolumeService : AccessibilityService() {

    private var bubble: Bubble? = null

    override fun onServiceConnected() {
        bubble = Bubble(this).also { it.show() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bubble?.onScreenChanged() // turned sideways or back
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bubble?.hide()
        bubble = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        bubble?.hide()
        bubble = null
        super.onDestroy()
    }

    // It asks for no events (see res/xml/volume_service.xml).
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
