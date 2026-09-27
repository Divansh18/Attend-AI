package com.divanshgandhi.attendai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert suspend fun insert(attendance: AttendanceEntity): Long

    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatest(staffId: Long): Flow<AttendanceEntity?>

    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY timestamp DESC, id DESC")
    fun observeHistory(staffId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT selfieFileName FROM attendance")
    suspend fun selfieFileNames(): List<String>
}
