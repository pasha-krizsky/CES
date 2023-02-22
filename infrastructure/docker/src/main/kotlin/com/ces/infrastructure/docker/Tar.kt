package com.ces.infrastructure.docker

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_STAR
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun compress(tarName: String, vararg filesToCompress: File) {
    tarArchiveOutputStream(tarName).use { out ->
        filesToCompress.forEach {
            addCompressed(out, it)
        }
    }
}

private fun tarArchiveOutputStream(name: String): TarArchiveOutputStream {
    val stream = TarArchiveOutputStream(FileOutputStream(name))
    stream.setBigNumberMode(BIGNUMBER_STAR)
    stream.setLongFileMode(LONGFILE_GNU)
    stream.setAddPaxHeadersForNonAsciiNames(true)
    return stream
}

private fun addCompressed(out: TarArchiveOutputStream, file: File) {
    val entry = file.name
    out.putArchiveEntry(TarArchiveEntry(file, entry))
    FileInputStream(file).use { `in` -> IOUtils.copy(`in`, out) }
    out.closeArchiveEntry()
}