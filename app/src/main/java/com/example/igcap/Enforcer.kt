package com.example.igcap

import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import java.time.LocalDate
import java.util.Calendar

object Enforcer {

    val LIMIT_MS: Long = BuildConfig.LIMIT_MINUTES * 60_000L

    /** Instagram and Instagram Lite. Usage is summed across both. */
    val TARGETS = setOf("com.instagram.android", "com.instagram.lite")

    private const val PREFS = "igcap"
    private const val KEY_LOCKED_DAY = "locked_day"

    fun admin(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    fun dpm(ctx: Context): DevicePolicyManager =
        ctx.getSystemService(DevicePolicyManager::class.java)

    fun isDeviceOwner(ctx: Context) = dpm(ctx).isDeviceOwnerApp(ctx.packageName)

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(android.app.AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), ctx.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun installedTargets(ctx: Context): Array<String> =
        TARGETS.filter {
            try { ctx.packageManager.getPackageInfo(it, 0); true } catch (_: Exception) { false }
        }.toTypedArray()

    /** Milliseconds Instagram was on screen since local midnight. */
    fun usedTodayMs(ctx: Context): Long {
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val dayStart = startOfToday()
        // Look back an extra day so a session that began before midnight is still seen.
        val events = usm.queryEvents(dayStart - 24L * 3600_000L, now)
        val e = UsageEvents.Event()

        val resumed = HashMap<String, MutableSet<String>>() // package -> resumed activity classes
        val since = HashMap<String, Long>()                 // package -> session start
        var total = 0L

        fun close(pkg: String, t: Long) {
            val s = since.remove(pkg) ?: return
            resumed[pkg]?.clear()
            total += (t - maxOf(s, dayStart)).coerceAtLeast(0L)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> if (pkg in TARGETS) {
                    resumed.getOrPut(pkg) { mutableSetOf() }.add(e.className ?: "")
                    since.putIfAbsent(pkg, e.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> if (pkg in TARGETS) {
                    val set = resumed[pkg]
                    set?.remove(e.className ?: "")
                    if (set.isNullOrEmpty()) close(pkg, e.timeStamp)
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN ->
                    for (p in since.keys.toList()) close(p, e.timeStamp)
            }
        }
        for (p in since.keys.toList()) close(p, now) // still open right now
        return total
    }

    /** One-way lock for today. Survives reinstall of Instagram (which could wipe its usage events). */
    private fun lockedToday(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCKED_DAY, null) == LocalDate.now().toString()

    private fun markLockedToday(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LOCKED_DAY, LocalDate.now().toString()).apply()
    }

    fun isBlockedNow(ctx: Context) = lockedToday(ctx)

    /** Call every few seconds. Suspends Instagram when the limit is hit, unsuspends at the next day. */
    fun check(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        if (!lockedToday(ctx) && hasUsageAccess(ctx) && usedTodayMs(ctx) >= LIMIT_MS) {
            markLockedToday(ctx)
        }
        val block = lockedToday(ctx)
        val pkgs = installedTargets(ctx)
        if (pkgs.isNotEmpty()) {
            try { dpm(ctx).setPackagesSuspended(admin(ctx), pkgs, block) } catch (_: Exception) {}
        }
    }

    /** Idempotent. Makes the limit hard to undo from the phone itself. */
    fun harden(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        val d = dpm(ctx); val a = admin(ctx)
        try { d.setUninstallBlocked(a, ctx.packageName, true) } catch (_: Exception) {}
        for (p in TARGETS) {
            // stops uninstall + reinstall as a way to reset Instagram
            try { d.setUninstallBlocked(a, p, true) } catch (_: Exception) {}
        }
        listOf(
            UserManager.DISALLOW_CONFIG_DATE_TIME,  // no changing the clock
            UserManager.DISALLOW_APPS_CONTROL,      // no force-stop / clear data / disable apps
            UserManager.DISALLOW_SAFE_BOOT,         // safe mode would disable this app
            UserManager.DISALLOW_ADD_USER           // no guest/secondary users with a fresh Instagram
        ).forEach { try { d.addUserRestriction(a, it) } catch (_: Exception) {} }
        try { d.setAutoTimeEnabled(a, true) } catch (_: Exception) {}
        try { d.setAutoTimeZoneEnabled(a, true) } catch (_: Exception) {}
        try {
            d.setPermissionGrantState(
                a, ctx.packageName, android.Manifest.permission.POST_NOTIFICATIONS,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        } catch (_: Exception) {}
    }
}
