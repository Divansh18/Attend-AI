package com.divanshgandhi.attendai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [StaffEntity::class], version = 1, exportSchema = true)
@TypeConverters(EmbeddingConverters::class)
abstract class AttendAiDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
}
