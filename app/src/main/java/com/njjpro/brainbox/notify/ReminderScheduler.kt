package com.njjpro.brainbox.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.njjpro.brainbox.MainActivity
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.REPEAT_DAILY
import com.njjpro.brainbox.data.REPEAT_MONTHLY
import com.njjpro.brainbox.data.REPEAT_NONE
import com.njjpro.brainbox.data.REPEAT_WEEKLY
import java.time.Instant
import java.time.ZoneId

object ReminderScheduler {

    private const val TAG = "BrainboxReminder"
    private const val ACTION_REMINDER = "com.njjpro.brainbox.action.REMINDER"
    private const val ACTION_TEST = "com.njjpro.brainbox.action.TEST"
    const val TEST_REQUEST_CODE = 0x7FFF0001

    private fun pendingIntent(context: Context, noteId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("brainbox://reminder/$noteId")
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 闹钟图标被点击时打开 App 并跳到这条记录。 */
    private fun showIntent(context: Context, noteId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getActivity(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    fun nextAlarmTime(context: Context): Long? {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return null
        return manager.nextAlarmClock?.triggerTime
    }

    /**
     * 排一个提醒。优先用 [AlarmManager.setAlarmClock]：
     * 它是系统里优先级最高的闹钟，Doze 下依然准点，多数国产 ROM 的省电清理也会跳过它。
     */
    fun schedule(context: Context, note: Note) {
        val at = note.reminderAt ?: return
        if (!note.reminderActive) {
            cancel(context, note.id)
            return
        }
        if (at <= System.currentTimeMillis()) return

        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, note.id)
        val exact = canScheduleExact(context)

        if (exact) {
            try {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(context, note.id)), pi)
                Log.i(TAG, "setAlarmClock id=${note.id} at=$at")
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "setAlarmClock 被拒绝，降级", e)
            }
        }

        try {
            if (exact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
            Log.i(TAG, "setExact/allowWhileIdle id=${note.id} exact=$exact at=$at")
        } catch (e: SecurityException) {
            Log.w(TAG, "精确定时被拒绝，降级为不精确", e)
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    /** 10 秒后触发一次测试提醒，用来验证「闹钟 → 广播 → 通知」整条链路。 */
    fun scheduleTest(context: Context, delayMillis: Long = 10_000L) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TEST
            data = Uri.parse("brainbox://test")
            putExtra(ReminderReceiver.EXTRA_TEST, true)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = System.currentTimeMillis() + delayMillis
        try {
            if (canScheduleExact(context)) {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(context, 0L)), pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
            Log.i(TAG, "测试提醒已排 at=$at")
        } catch (e: SecurityException) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent(context, noteId))
    }

    /** 计算下一次触发时间；未知规则直接返回原值。 */
    fun nextOccurrence(fromMillis: Long, repeatRule: String): Long {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(fromMillis).atZone(zone)
        val next = when (repeatRule) {
            REPEAT_DAILY -> time.plusDays(1)
            REPEAT_WEEKLY -> time.plusWeeks(1)
            REPEAT_MONTHLY -> time.plusMonths(1)
            else -> return fromMillis
        }
        return next.toInstant().toEpochMilli()
    }

    /** 把已经过期的重复提醒推进到未来。 */
    fun rollForward(note: Note): Note {
        var at = note.reminderAt ?: return note
        if (note.repeatRule == REPEAT_NONE || at > System.currentTimeMillis()) return note
        var guard = 0
        while (at <= System.currentTimeMillis() && guard < 500) {
            at = nextOccurrence(at, note.repeatRule)
            guard++
        }
        return note.copy(reminderAt = at)
    }
}
