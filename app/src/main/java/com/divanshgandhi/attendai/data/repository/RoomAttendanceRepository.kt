package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.AttendanceDao
import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.location.LocationFix
import com.divanshgandhi.attendai.storage.SelfieStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RoomAttendanceRepository(private val dao: AttendanceDao, private val selfies: SelfieStorage) : AttendanceRepository {
    private val fileLock = Mutex()

    override fun observeHistory(staffId: Long) = dao.observeHistory(staffId)

    override fun observeLatest(staffId: Long) = dao.observeLatest(staffId).onStart {
        withContext(Dispatchers.IO) {
            fileLock.withLock { selfies.removeUnreferenced(dao.selfieFileNames().toSet()) }
        }
    }

    override suspend fun record(staffId: Long, selfieJpeg: ByteArray, location: LocationFix): AttendanceEntity {
        currentCoroutineContext().ensureActive()
        // Once this short disk commit starts, finish it or roll it back even if the screen closes.
        return withContext(Dispatchers.IO + NonCancellable) {
            fileLock.withLock {
                selfies.removeUnreferenced(dao.selfieFileNames().toSet())
                val name = selfies.save(selfieJpeg)
                try {
                    val row = AttendanceEntity(staffId = staffId, timestamp = System.currentTimeMillis(), selfieFileName = name,
                        latitude = location.latitude, longitude = location.longitude, accuracyMeters = location.accuracyMeters)
                    row.copy(id = dao.insert(row))
                } catch (e: Exception) {
                    try { selfies.delete(name) } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
                    throw e
                }
            }
        }
    }
}
