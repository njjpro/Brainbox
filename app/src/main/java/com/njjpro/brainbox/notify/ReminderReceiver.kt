package com.njjpro.brainbox.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.njjpro.brainbox.data.AppDatabase
import com.njjpro.brainbox.data.REPEAT_NONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(appContext).noteDao()
                val note = dao.byId(noteId) ?: return@launch
                if (!note.reminderActive || note.reminderAt == null) return@launch

                Notifications.showReminder(appContext, note)

                if (note.repeatRule == REPEAT_NONE) {
                    dao.upsert(note.copy(reminderActive = false))
                } else {
                    val next = ReminderScheduler.rollForward(note)
                    val updated = next.copy(reminderActive = true)
                    dao.upsert(updated)
                    ReminderScheduler.schedule(appContext, updated)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
    }
}
