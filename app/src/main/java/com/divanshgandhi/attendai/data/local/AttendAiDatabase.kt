package com.divanshgandhi.attendai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [StaffEntity::class, AttendanceEntity::class], version = 2, exportSchema = true)
@TypeConverters(EmbeddingConverters::class)
abstract class AttendAiDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun staffDao(): StaffDao
}
