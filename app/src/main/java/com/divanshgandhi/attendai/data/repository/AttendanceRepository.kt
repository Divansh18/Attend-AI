package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.location.LocationFix
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun observeHistory(staffId: Long): Flow<List<AttendanceEntity>>
    fun observeLatest(staffId: Long): Flow<AttendanceEntity?>
    suspend fun record(staffId: Long, selfieJpeg: ByteArray, location: LocationFix): AttendanceEntity
}
