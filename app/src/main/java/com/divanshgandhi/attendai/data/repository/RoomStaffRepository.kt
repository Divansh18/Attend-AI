package com.divanshgandhi.attendai.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.divanshgandhi.attendai.data.local.EmbeddingConverters
import com.divanshgandhi.attendai.data.local.StaffDao
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.face.FaceMatcher
import com.divanshgandhi.attendai.face.TfliteFaceEmbedder
import java.util.Locale

class RoomStaffRepository(private val dao: StaffDao) : StaffRepository {
    override fun observeStaff() = dao.observeAll()
    override fun observeStaff(id: Long) = dao.observeById(id)

    override suspend fun addStaff(name: String, employeeId: String): Long {
        val cleanName = name.trim()
        val cleanId = employeeId.trim().uppercase(Locale.ROOT)
        require(cleanName.isNotEmpty()) { "Enter a name." }
        require(cleanId.isNotEmpty()) { "Enter an employee ID." }
        if (dao.employeeIdExists(cleanId)) throw DuplicateEmployeeIdException()
        try {
            return dao.insert(StaffEntity(name = cleanName, employeeId = cleanId))
        } catch (e: SQLiteConstraintException) {
            // The unique index also closes the race between simultaneous submissions.
            if (dao.employeeIdExists(cleanId)) throw DuplicateEmployeeIdException()
            throw e
        }
    }

    override suspend fun saveFaceEmbedding(staffId: Long, embedding: FloatArray) {
        require(embedding.size == 128) { "Expected a 128-dimensional face embedding." }
        val normalized = FaceMatcher.normalize(embedding)
        // Query array arguments otherwise expand into SQL lists; bind one explicit BLOB.
        val bytes = checkNotNull(EmbeddingConverters().toBytes(normalized))
        val updated = dao.updateEnrollment(staffId, bytes, TfliteFaceEmbedder.MODEL_SHA256, System.currentTimeMillis())
        if (updated == 0) throw StaffNotFoundException()
    }
}
