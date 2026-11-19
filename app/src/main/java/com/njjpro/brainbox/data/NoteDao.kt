package com.njjpro.brainbox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): Note?

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL ORDER BY reminderAt ASC")
    fun observeReminders(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE reminderActive = 1 AND reminderAt IS NOT NULL")
    suspend fun activeReminders(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)
}

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Expense>): List<Long>

    @Delete
    suspend fun delete(expense: Expense)
}
