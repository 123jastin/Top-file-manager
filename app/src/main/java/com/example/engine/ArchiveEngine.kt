package com.example.engine

import com.example.data.model.CompressionLevel
import com.example.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*

class ArchiveEngine {

    suspend fun createZipArchive(
        files: List<File>,
        outputZipFile: File,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        onProgress: (currentFile: String, processedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            outputZipFile.parentFile?.mkdirs()

            // Calculate total bytes
            var totalBytes = 0L
            fun calculateBytes(file: File) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { calculateBytes(it) }
                } else {
                    totalBytes += file.length()
                }
            }
            files.forEach { calculateBytes(it) }

            var processedBytes = 0L

            FileOutputStream(outputZipFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        zos.setLevel(compressionLevel.zipLevel)

                        for (file in files) {
                            addFileToZip(zos, file, file.parentFile ?: file, { currentFileName, deltaBytes ->
                                processedBytes += deltaBytes
                                onProgress(currentFileName, processedBytes, totalBytes)
                            })
                        }
                    }
                }
            }

            outputZipFile
        }
    }

    private fun addFileToZip(
        zos: ZipOutputStream,
        file: File,
        baseDir: File,
        onBytesWritten: (fileName: String, delta: Long) -> Unit
    ) {
        val entryName = baseDir.toURI().relativize(file.toURI()).path
        if (file.isDirectory) {
            val dirEntryName = if (entryName.endsWith("/")) entryName else "$entryName/"
            val entry = ZipEntry(dirEntryName)
            zos.putNextEntry(entry)
            zos.closeEntry()

            file.listFiles()?.forEach { child ->
                addFileToZip(zos, child, baseDir, onBytesWritten)
            }
        } else {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)

            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (bis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                        onBytesWritten(file.name, count.toLong())
                    }
                }
            }
            zos.closeEntry()
        }
    }

    suspend fun extractZipArchive(
        zipFile: File,
        targetDir: File,
        onProgress: (currentEntry: String, extractedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            targetDir.mkdirs()
            val totalBytes = zipFile.length()
            var processedBytes = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    val newFile = File(targetDir, currentEntry.name)

                    // Security check against zip slip
                    val canonicalDestDir = targetDir.canonicalPath
                    val canonicalDestFile = newFile.canonicalPath
                    if (!canonicalDestFile.startsWith(canonicalDestDir)) {
                        throw SecurityException("Zip entry is outside target directory: ${currentEntry.name}")
                    }

                    if (currentEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            BufferedOutputStream(fos).use { bos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    bos.write(buffer, 0, len)
                                    processedBytes += len
                                    onProgress(currentEntry.name, processedBytes, totalBytes)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                }
            }
            targetDir
        }
    }

    suspend fun previewZipContents(zipFile: File): List<ZipEntryPreview> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ZipEntryPreview>()
        runCatching {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    list.add(
                        ZipEntryPreview(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory,
                            crc = entry.crc
                        )
                    )
                }
            }
        }
        list
    }
}

data class ZipEntryPreview(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val crc: Long
)
