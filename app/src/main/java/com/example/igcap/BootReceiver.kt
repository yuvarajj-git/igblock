package com.example.igcap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Enforcer.harden(context)
        Enforcer.check(context)
        MonitorService.start(context)
    }
}
