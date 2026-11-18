package com.njjpro.brainbox.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.njjpro.brainbox.data.AppDatabase
import com.njjpro.brainbox.data.REPEAT_NONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext

        if (intent.getBooleanExtra(EXTRA_TEST, false)) {
            Log.i(TAG, "收到测试提醒广播")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Notifications.showTest(appContext)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L) return
        Log.i(TAG, "提醒触发 id=$noteId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(appContext).noteDao()
                val note = dao.byId(noteId) ?: run {
                    Log.w(TAG, "提醒 id=$noteId 对应的记录已不存在")
                    return@launch
                }
                if (!note.reminderActive || note.reminderAt == null) {
                    Log.w(TAG, "提醒 id=$noteId 已被关闭，跳过")
                    return@launch
                }

                Notifications.showReminder(appContext, note)

                if (note.repeatRule == REPEAT_NONE) {
                    dao.upsert(note.copy(reminderActive = false))
                } else {
                    val next = ReminderScheduler.rollForward(note)
                    val updated = next.copy(reminderActive = true)
                    dao.upsert(updated)
                    ReminderScheduler.schedule(appContext, updated)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "处理提醒 id=$noteId 失败", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BrainboxReminder"
        const val EXTRA_NOTE_ID = "noteId"
        const val EXTRA_TEST = "test"
    }
}
