package com.njjpro.brainbox.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.njjpro.brainbox.data.Expense
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.REPEAT_NONE
import com.njjpro.brainbox.data.Repository
import com.njjpro.brainbox.notify.ReminderScheduler
import com.njjpro.brainbox.update.CheckResult
import com.njjpro.brainbox.update.UpdateChecker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BrainboxViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Context = app.applicationContext
    private val repository = Repository(appContext)
    private val prefs = appContext.getSharedPreferences("brainbox_prefs", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    val notes: StateFlow<List<Note>> =
        repository.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenses: StateFlow<List<Expense>> =
        repository.expenses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentVersion: String = UpdateChecker.currentVersion(appContext)

    var updateState by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
        private set

    /** 启动时由 MainActivity 置位，用于跳转到通知对应的记录 */
    var pendingNoteId by mutableStateOf<Long?>(null)

    init {
        viewModelScope.launch {
            repository.activeReminders().forEach { note ->
                val fixed = ReminderScheduler.rollForward(note)
                if (fixed != note) repository.saveReminderState(fixed)
                ReminderScheduler.schedule(appContext, fixed)
            }
        }
        autoCheckIfDue()
    }

    // ---------------- 记录 ----------------

    fun saveNote(note: Note, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val base = note.copy(updatedAt = now)
            val newId = repository.saveNote(base)
            var saved = base.copy(id = if (base.id == 0L) newId else base.id)

            val reminderAt = saved.reminderAt
            if (reminderAt != null && reminderAt > now) {
                saved = saved.copy(reminderActive = true)
                repository.saveReminderState(saved)
                ReminderScheduler.schedule(appContext, saved)
            } else {
                saved = saved.copy(reminderActive = false)
                repository.saveReminderState(saved)
                ReminderScheduler.cancel(appContext, saved.id)
            }
            onSaved(saved.id)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, note.id)
            repository.deleteNote(note)
        }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setReminder(note: Note, atMillis: Long?, repeatRule: String = note.repeatRule) {
        viewModelScope.launch {
            val active = atMillis != null && atMillis > System.currentTimeMillis()
            val updated = note.copy(
                reminderAt = atMillis,
                repeatRule = if (repeatRule.isBlank()) REPEAT_NONE else repeatRule,
                reminderActive = active,
                updatedAt = System.currentTimeMillis(),
            )
            repository.saveReminderState(updated)
            if (active) ReminderScheduler.schedule(appContext, updated) else ReminderScheduler.cancel(appContext, note.id)
        }
    }

    fun disableReminder(note: Note) {
        setReminder(note, null, REPEAT_NONE)
    }

    fun snoozeReminder(note: Note, minutes: Int) {
        val target = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        setReminder(note, target, note.repeatRule)
    }

    // ---------------- 报销 ----------------

    fun saveExpense(expense: Expense) {
        viewModelScope.launch { repository.saveExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun toggleReimbursed(expense: Expense) {
        viewModelScope.launch { repository.saveExpense(expense.copy(reimbursed = !expense.reimbursed)) }
    }

    // ---------------- 更新 ----------------

    private fun autoCheckIfDue() {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        val due = System.currentTimeMillis() - last > TimeUnit.HOURS.toMillis(12)
        if (due) checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (updateState is UpdateUiState.Checking || updateState is UpdateUiState.Downloading) return
        updateState = UpdateUiState.Checking
        viewModelScope.launch {
            when (val result = UpdateChecker.check(currentVersion)) {
                is CheckResult.Newer -> updateState = UpdateUiState.Available(result.info)
                CheckResult.Latest -> updateState = UpdateUiState.UpToDate
                is CheckResult.Failed -> {
                    updateState = if (silent) UpdateUiState.Idle else UpdateUiState.Error(result.message)
                }
            }
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        }
    }

    fun downloadAndInstall() {
        val available = updateState as? UpdateUiState.Available ?: return
        updateState = UpdateUiState.Downloading(available.info, 0)
        viewModelScope.launch {
            runCatching {
                val file = UpdateChecker.download(appContext, available.info) { percent ->
                    mainHandler.post { updateState = UpdateUiState.Downloading(available.info, percent) }
                }
                updateState = UpdateUiState.Ready(available.info, file)
                if (!UpdateChecker.install(appContext, file)) {
                    updateState = UpdateUiState.Error("需要先允许「安装未知应用」权限")
                }
            }.onFailure { error ->
                updateState = UpdateUiState.Error(error.message ?: "下载失败")
            }
        }
    }

    fun dismissUpdate() {
        updateState = UpdateUiState.Idle
    }

    private companion object {
        const val KEY_LAST_CHECK = "last_update_check"
    }
}
