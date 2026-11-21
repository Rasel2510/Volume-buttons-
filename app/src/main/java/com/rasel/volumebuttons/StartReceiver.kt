package com.rasel.volumebuttons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a restart or an app update, switches the bubble back on if the
 *  phone left it off (the app isn't running then to notice by itself). */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!KeepOn.isOn(context)) KeepOn.turnOn(context)
    }
}
