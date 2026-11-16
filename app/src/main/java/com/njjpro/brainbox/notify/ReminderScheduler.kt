package com.njjpro.brainbox.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.REPEAT_DAILY
import com.njjpro.brainbox.data.REPEAT_MONTHLY
import com.njjpro.brainbox.data.REPEAT_WEEKLY
import java.time.Instant
import java.time.ZoneId

object ReminderScheduler {

    private const val ACTION_REMINDER = "com.njjpro.brainbox.action.REMINDER"

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

    fun schedule(context: Context, note: Note) {
        val at = note.reminderAt ?: return
        if (!note.reminderActive) {
            cancel(context, note.id)
            return
        }
        if (at <= System.currentTimeMillis()) return

        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, note.id)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
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
        if (note.repeatRule == "NONE" || at > System.currentTimeMillis()) return note
        var guard = 0
        while (at <= System.currentTimeMillis() && guard < 500) {
            at = nextOccurrence(at, note.repeatRule)
            guard++
        }
        return note.copy(reminderAt = at)
    }
}
