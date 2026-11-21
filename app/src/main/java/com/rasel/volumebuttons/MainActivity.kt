package com.rasel.volumebuttons

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
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
        val on = serviceOn()
        status.setText(if (on) R.string.status_on else R.string.status_off)
        turnOn.setText(if (on) R.string.accessibility_settings else R.string.turn_on)
    }

    private fun serviceOn(): Boolean {
        val me = ComponentName(this, VolumeService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
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
