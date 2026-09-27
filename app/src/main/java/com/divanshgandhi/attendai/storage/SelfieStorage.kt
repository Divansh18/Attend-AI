package com.divanshgandhi.attendai.storage

import java.io.File
import java.util.UUID

/** Only called on Dispatchers.IO by the attendance repository. */
interface SelfieStorage {
    fun save(jpeg: ByteArray): String
    fun delete(fileName: String)
    fun removeUnreferenced(referenced: Set<String>)
}

class PrivateSelfieStorage(private val directory: File) : SelfieStorage {
    override fun save(jpeg: ByteArray): String {
        require(jpeg.isNotEmpty()) { "Selfie is empty." }
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create selfie directory." }
        val name = UUID.randomUUID().toString() + ".jpg"
        val temp = File(directory, "$name.tmp")
        val target = File(directory, name)
        try {
            temp.outputStream().use { it.write(jpeg); it.fd.sync() }
            check(temp.renameTo(target)) { "Cannot finalize selfie file." }
            return name
        } catch (e: Exception) {
            temp.delete(); target.delete()
            throw e
        }
    }
    override fun delete(fileName: String) {
        require(File(fileName).name == fileName) { "Expected a private file name." }
        val file = File(directory, fileName)
        check(!file.exists() || file.delete()) { "Could not remove unfinished selfie." }
    }
    override fun removeUnreferenced(referenced: Set<String>) {
        // This directory belongs exclusively to attendance; reconcile interrupted writes on next use.
        directory.listFiles()?.filter { it.isFile && it.name !in referenced }?.forEach { delete(it.name) }
    }
}
