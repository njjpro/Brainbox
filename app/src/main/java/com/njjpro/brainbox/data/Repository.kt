package com.njjpro.brainbox.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {

    private val noteDao = AppDatabase.get(context).noteDao()
    private val expenseDao = AppDatabase.get(context).expenseDao()

    val notes: Flow<List<Note>> = noteDao.observeAll()
    val reminders: Flow<List<Note>> = noteDao.observeReminders()
    val expenses: Flow<List<Expense>> = expenseDao.observeAll()

    suspend fun noteById(id: Long): Note? = noteDao.byId(id)

    suspend fun saveNote(note: Note): Long = noteDao.upsert(note)

    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    suspend fun activeReminders(): List<Note> = noteDao.activeReminders()

    suspend fun saveReminderState(note: Note) = noteDao.upsert(note)

    suspend fun saveExpense(expense: Expense): Long = expenseDao.upsert(expense)

    suspend fun saveExpenses(items: List<Expense>) = expenseDao.upsertAll(items)

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)
}
