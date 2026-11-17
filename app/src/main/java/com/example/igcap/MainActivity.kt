package com.example.igcap

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView

/** Read-only status screen. There is intentionally no button to change or disable the limit. */
class MainActivity : Activity() {

    private lateinit var tv: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val refresh = object : Runnable {
        override fun run() {
            val owner = Enforcer.isDeviceOwner(this@MainActivity)
            val usage = Enforcer.hasUsageAccess(this@MainActivity)
            val usedMin = if (usage) Enforcer.usedTodayMs(this@MainActivity) / 60_000.0 else 0.0
            val limitMin = Enforcer.LIMIT_MS / 60_000L
            tv.text = buildString {
                appendLine("IgCap")
                appendLine()
                appendLine("Device owner: " + if (owner) "yes" else "NO (run setup.sh)")
                appendLine("Usage access: " + if (usage) "yes" else "NO (run setup.sh)")
                appendLine()
                appendLine("Instagram today: %.1f / %d min".format(usedMin, limitMin))
                appendLine(if (Enforcer.isBlockedNow(this@MainActivity)) "Status: BLOCKED until midnight" else "Status: allowed")
            }
            handler.postDelayed(this, 3_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tv = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER; setPadding(48, 48, 48, 48) }
        setContentView(tv)
        Enforcer.harden(this)
        MonitorService.start(this)
    }

    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
}
