package com.njjpro.brainbox.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.njjpro.brainbox.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机、应用更新、时间变化后重建所有闹钟，否则提醒会永久丢失。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> Unit
            else -> return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(appContext).noteDao()
                dao.activeReminders().forEach { note ->
                    val fixed = ReminderScheduler.rollForward(note)
                    if (fixed != note) dao.upsert(fixed)
                    ReminderScheduler.schedule(appContext, fixed)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
