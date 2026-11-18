package com.njjpro.brainbox.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.njjpro.brainbox.MainActivity
import com.njjpro.brainbox.R
import com.njjpro.brainbox.data.Note

object Notifications {

    private const val TAG = "BrainboxNotify"
    const val CHANNEL_REMINDER = "reminders"
    private const val TEST_NOTIFICATION_ID = 0x7FFF0001

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_REMINDER) == null) {
            val channel = NotificationChannel(
                CHANNEL_REMINDER,
                "定时提醒",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "到点提醒你去做某件事"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** 通知总开关是否打开，且运行时权限已授予。 */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** 系统设置里通知权限开着，但「定时提醒」这个渠道被单独关掉了。 */
    fun channelBlocked(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(CHANNEL_REMINDER) ?: return false
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    fun showReminder(context: Context, note: Note) {
        ensureChannels(context)
        if (!hasPermission(context)) {
            Log.w(TAG, "通知未开启，提醒 id=${note.id} 被丢弃")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            note.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = note.content.trim().ifBlank { note.categoryEnum.label }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(note.displayTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(note.id.toInt(), notification)
            Log.i(TAG, "已发提醒通知 id=${note.id}")
        } catch (e: SecurityException) {
            Log.w(TAG, "发通知被拒绝 id=${note.id}", e)
        }
    }

    /** 立即发一条测试通知；返回 false 表示通知权限没开。 */
    fun showTest(context: Context): Boolean {
        ensureChannels(context)
        if (!hasPermission(context)) return false

        val contentIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("测试通知")
            .setContentText("看到这条说明通知通道正常。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }
}
