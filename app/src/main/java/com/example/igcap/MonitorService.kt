package com.example.igcap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var ticks = 0

    private val tick = object : Runnable {
        override fun run() {
            Enforcer.check(this@MonitorService)
            if (ticks++ % 60 == 0) Enforcer.harden(this@MonitorService) // about every 5 minutes
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("igcap", "IgCap", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, "igcap")
            .setContentTitle("IgCap is running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(ctx: Context) {
            try { ctx.startForegroundService(Intent(ctx, MonitorService::class.java)) }
            catch (_: Exception) {}
        }
    }
}
