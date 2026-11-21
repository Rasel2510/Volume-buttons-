package com.rasel.volumebuttons

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings

/**
 * Switches the bubble's accessibility service back on.
 *
 * Some phones (vivo) switch every accessibility service off when Settings or
 * a permission pop-up opens, and don't always switch them back on. When the
 * service goes off while the app is still running, [watch] switches it back
 * on a moment later, whichever app is open.
 *
 * Needs WRITE_SECURE_SETTINGS, granted once over USB (see README); without it
 * nothing happens here. A force stop still switches the bubble off for good,
 * since it stops the app too.
 */
object KeepOn {

    private const val DELAY_MS = 2_000L
    /** Switched off again this soon after coming back: something keeps doing
     *  it, so wait longer instead of fighting. */
    private const val AGAIN_MS = 15_000L
    private const val AGAIN_DELAY_MS = 30_000L

    private val handler = Handler(Looper.getMainLooper())
    private var lastTurnedOn = 0L

    fun isOn(context: Context): Boolean {
        val resolver = context.contentResolver
        return Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 &&
            enabledServices(context).any { isMe(context, it) }
    }

    /** Switches the service on, keeping any other services that are on. */
    fun turnOn(context: Context) {
        if (!canTurnOn(context)) return
        val me = ComponentName(context, VolumeService::class.java).flattenToString()
        val services = enabledServices(context).filterNot { isMe(context, it) } + me
        val resolver = context.contentResolver
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services.joinToString(":"))
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    }

    /** The service was switched off: switch it back on in a moment. */
    fun watch(context: Context) {
        val app = context.applicationContext
        if (!canTurnOn(app)) return
        handler.removeCallbacksAndMessages(null)
        val again = SystemClock.elapsedRealtime() - lastTurnedOn < AGAIN_MS
        handler.postDelayed({
            if (!isOn(app)) {
                turnOn(app)
                lastTurnedOn = SystemClock.elapsedRealtime()
            }
        }, if (again) AGAIN_DELAY_MS else DELAY_MS)
    }

    fun stop() = handler.removeCallbacksAndMessages(null)

    private fun canTurnOn(context: Context) =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    private fun enabledServices(context: Context): List<String> =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').filter { it.isNotBlank() }

    private fun isMe(context: Context, name: String) =
        ComponentName.unflattenFromString(name) == ComponentName(context, VolumeService::class.java)
}
