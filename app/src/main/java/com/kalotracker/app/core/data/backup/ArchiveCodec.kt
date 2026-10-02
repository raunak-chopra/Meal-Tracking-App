package com.kalotracker.app.core.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ArchiveContents(val backup: BackupFile, val photos: Map<String, ByteArray>)

object ArchiveCodec {
    const val MAX_ARCHIVE_BYTES = 200L * 1024 * 1024
    private const val MAX_PHOTO = 20 * 1024 * 1024
    fun photoName(id: String): String = "photos/" + MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray()).joinToString("") { "%02x".format(it) } + ".jpg"

    fun write(output: OutputStream, backup: BackupFile, readPhoto: (String) -> ByteArray?) {
        val manifest = BackupCodec.encode(backup).toByteArray(Charsets.UTF_8)
        require(manifest.size <= 30 * 1024 * 1024) { "Backup manifest is too large." }
        var total = manifest.size.toLong()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json")); zip.write(manifest); zip.closeEntry()
            backup.meals.forEach { m -> m.photoEntry?.let { entry ->
                require(entry == photoName(m.id))
                val bytes = readPhoto(m.id) ?: throw BackupFormatException("A meal photo is missing. Export was not completed.")
                total += bytes.size
                require(bytes.size <= MAX_PHOTO && total <= MAX_ARCHIVE_BYTES) { "Backup archive is too large." }
                zip.putNextEntry(ZipEntry(entry)); zip.write(bytes); zip.closeEntry()
            } }
        }
    }

    fun read(input: InputStream): ArchiveContents {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val safe = entry.name == "backup.json" || Regex("photos/[a-f0-9]{64}\\.jpg").matches(entry.name)
                if (!safe || entry.isDirectory || entries.containsKey(entry.name)) throw BackupFormatException("Unsafe or duplicate archive entry.")
                val max = if (entry.name == "backup.json") 30 * 1024 * 1024 else MAX_PHOTO
                val bytes = zip.readNBytesCompat(max + 1)
                total += bytes.size
                if (bytes.size > max || total > MAX_ARCHIVE_BYTES) throw BackupFormatException("Backup archive is too large.")
                entries[entry.name] = bytes
                zip.closeEntry()
            }
        }
        val manifest = entries.remove("backup.json") ?: throw BackupFormatException("Archive has no backup.json.")
        val backup = BackupCodec.decode(manifest.toString(Charsets.UTF_8))
        val names = backup.meals.mapNotNull { it.photoEntry }.toSet()
        if (names != entries.keys || backup.meals.any { it.photoEntry != null && it.photoEntry != photoName(it.id) })
            throw BackupFormatException("Photo manifest does not match the archive.")
        return ArchiveContents(backup, entries)
    }
    private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
