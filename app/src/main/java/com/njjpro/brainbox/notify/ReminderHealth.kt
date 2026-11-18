package com.njjpro.brainbox.notify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 提醒可靠性自检：通知、精确闹钟、电池优化、厂商自启动。
 * 这些开关任何一项没开，AlarmManager 的提醒都可能到不了用户眼前。
 */
object ReminderHealth {

    fun notificationsAllowed(context: Context): Boolean = Notifications.hasPermission(context)

    fun channelBlocked(context: Context): Boolean = Notifications.channelBlocked(context)

    fun exactAlarmsAllowed(context: Context): Boolean = ReminderScheduler.canScheduleExact(context)

    /** 电池优化已对本 App 放行（即「不受限制 / 允许后台活动」）。 */
    fun batteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun nextAlarmTime(context: Context): Long? = ReminderScheduler.nextAlarmTime(context)

    private fun start(context: Context, intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)

    fun openNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (start(context, intent)) return true
        return start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    fun openChannelSettings(context: Context): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, Notifications.CHANNEL_REMINDER)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return start(context, intent) || openNotificationSettings(context)
    }

    fun openExactAlarmSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${context.packageName}"))
        return start(context, intent) || openAppDetails(context)
    }

    fun openBatterySettings(context: Context): Boolean {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        if (start(context, direct)) return true
        if (start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return true
        return openAppDetails(context)
    }

    fun openAppDetails(context: Context): Boolean = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    /** 国产 ROM 的自启动管理页；找不到就返回 false，交给调用方提示用户手动找。 */
    fun openAutoStartSettings(context: Context): Boolean {
        val component = autoStartComponent(context) ?: return false
        return start(context, Intent().setComponent(component))
    }

    fun autoStartPageAvailable(context: Context): Boolean = autoStartComponent(context) != null

    private fun autoStartComponent(context: Context): ComponentName? {
        for (flat in AUTO_START_PAGES) {
            val parts = flat.split("/")
            val component = ComponentName(parts[0], parts[1])
            if (context.packageManager.resolveActivity(Intent().setComponent(component), 0) != null) {
                return component
            }
        }
        return null
    }

    private val AUTO_START_PAGES = listOf(
        // 小米 / Redmi
        "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
        // 华为 / 荣耀
        "com.huawei.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager/com.huawei.systemmanager.optimize.process.ProtectActivity",
        // OPPO / 一加 / realme
        "com.coloros.safecenter/com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.oppo.safe/com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.coloros.oppoguardelf/com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
        // vivo / iQOO
        "com.vivo.permissionmanager/com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure/com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        // 魅族
        "com.meizu.safe/com.meizu.safe.permission.SmartBGActivity",
        // 三星
        "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
    )
}
