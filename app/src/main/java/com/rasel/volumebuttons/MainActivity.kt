package com.rasel.volumebuttons

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

/** One screen: is the bubble on, and a button to turn it on. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var turnOn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        turnOn = findViewById(R.id.turn_on)
        turnOn.setOnClickListener { openAccessibilitySettings() }
    }

    override fun onResume() {
        super.onResume()
        if (!serviceOn()) turnOnDirectly()
        val on = serviceOn()
        status.setText(if (on) R.string.status_on else R.string.status_off)
        turnOn.setText(if (on) R.string.accessibility_settings else R.string.turn_on)
    }

    private fun enabledServices(): List<String> =
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').filter { it.isNotBlank() }

    private fun isMe(name: String) =
        ComponentName.unflattenFromString(name) == ComponentName(this, VolumeService::class.java)

    private fun serviceOn() = enabledServices().any(::isMe)

    /** A force stop makes Android switch the service off. With the
     *  WRITE_SECURE_SETTINGS permission (granted over USB, see README) the app
     *  switches it back on itself, keeping any other services that are on. */
    private fun turnOnDirectly() {
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return
        val me = ComponentName(this, VolumeService::class.java).flattenToString()
        val services = enabledServices().filterNot(::isMe) + me
        Settings.Secure.putString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services.joinToString(":"),
        )
        Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    }

    private fun openAccessibilitySettings() {
        val key = ComponentName(this, VolumeService::class.java).flattenToString()
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                // Asks the list to scroll to this app (works on many phones).
                .putExtra(":settings:fragment_args_key", key)
                .putExtra(":settings:show_fragment_args", Bundle().apply {
                    putString(":settings:fragment_args_key", key)
                }),
        )
    }
}
