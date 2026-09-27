package com.divanshgandhi.attendai.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                staffId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                selfieFileName TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                accuracyMeters REAL,
                FOREIGN KEY(staffId) REFERENCES staff(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_staffId ON attendance(staffId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_selfieFileName ON attendance(selfieFileName)")
    }
}
