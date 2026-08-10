package com.example.engine

import android.os.Environment
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class StorageAnalyzerEngine(private val fileEngine: FileEngine) {

    suspend fun analyzeStorageBreakdown(rootPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: ""): List<StorageBreakdown> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()

        var imageBytes = 0L; var imageCount = 0
        var videoBytes = 0L; var videoCount = 0
        var audioBytes = 0L; var audioCount = 0
        var docBytes = 0L; var docCount = 0
        var archiveBytes = 0L; var archiveCount = 0
        var apkBytes = 0L; var apkCount = 0
        var otherBytes = 0L; var otherCount = 0

        fun scan(dir: File, depth: Int) {
            if (depth > 8) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isFile) {
                    val len = f.length()
                    val item = fileEngine.createFileItem(f)

                    when {
                        fileEngine.matchesCategory(item, CategoryType.IMAGES) -> {
                            imageBytes += len; imageCount++
                        }
                        fileEngine.matchesCategory(item, CategoryType.VIDEOS) -> {
                            videoBytes += len; videoCount++
                        }
                        fileEngine.matchesCategory(item, CategoryType.AUDIO) -> {
                            audioBytes += len; audioCount++
                        }
                        fileEngine.matchesCategory(item, CategoryType.DOCUMENTS) -> {
                            docBytes += len; docCount++
                        }
                        fileEngine.matchesCategory(item, CategoryType.ARCHIVES) -> {
                            archiveBytes += len; archiveCount++
                        }
                        fileEngine.matchesCategory(item, CategoryType.APKS) -> {
                            apkBytes += len; apkCount++
                        }
                        else -> {
                            otherBytes += len; otherCount++
                        }
                    }
                } else if (f.isDirectory) {
                    scan(f, depth + 1)
                }
            }
        }

        scan(root, 0)

        listOf(
            StorageBreakdown("Images", imageBytes, imageCount, 0xFF3B82F6),
            StorageBreakdown("Videos", videoBytes, videoCount, 0xFF8B5CF6),
            StorageBreakdown("Audio", audioBytes, audioCount, 0xFF10B981),
            StorageBreakdown("Documents", docBytes, docCount, 0xFFF59E0B),
            StorageBreakdown("Archives", archiveBytes, archiveCount, 0xFFEC4899),
            StorageBreakdown("APKs", apkBytes, apkCount, 0xFF06B6D4),
            StorageBreakdown("Other", otherBytes, otherCount, 0xFF64748B)
        )
    }

    suspend fun findLargeFiles(
        rootPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: "",
        minSizeBytes: Long = 100 * 1024 * 1024L
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()

        val largeFiles = mutableListOf<FileItem>()

        fun scan(dir: File, depth: Int) {
            if (depth > 8) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isFile && f.length() >= minSizeBytes) {
                    largeFiles.add(fileEngine.createFileItem(f))
                } else if (f.isDirectory) {
                    scan(f, depth + 1)
                }
            }
        }

        scan(root, 0)
        largeFiles.sortedByDescending { it.size }
    }

    suspend fun findDuplicates(
        rootPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: ""
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()

        // Step 1: Group files by file size
        val sizeMap = mutableMapOf<Long, MutableList<File>>()

        fun scan(dir: File, depth: Int) {
            if (depth > 7) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isFile && f.length() > 50 * 1024L) { // Only files > 50KB
                    val list = sizeMap.getOrPut(f.length()) { mutableListOf() }
                    list.add(f)
                } else if (f.isDirectory) {
                    scan(f, depth + 1)
                }
            }
        }

        scan(root, 0)

        // Step 2: For size groups with >= 2 files, calculate SHA-256 checksum to verify duplicate content
        val candidateGroups = sizeMap.filter { it.value.size >= 2 }
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((size, files) in candidateGroups) {
            val hashMap = mutableMapOf<String, MutableList<FileItem>>()

            for (file in files) {
                val hash = calculateQuickSha256(file)
                if (hash != null) {
                    val item = fileEngine.createFileItem(file)
                    val list = hashMap.getOrPut(hash) { mutableListOf() }
                    list.add(item)
                }
            }

            for ((hash, items) in hashMap) {
                if (items.size >= 2) {
                    duplicateGroups.add(DuplicateGroup(checksum = hash, size = size, files = items))
                }
            }
        }

        duplicateGroups.sortedByDescending { it.size * it.files.size }
    }

    private fun calculateQuickSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(32768)
                val bytesRead = fis.read(buffer)
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    suspend fun findEmptyFolders(rootPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: ""): List<FileItem> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()

        val emptyDirs = mutableListOf<FileItem>()

        fun scan(dir: File, depth: Int) {
            if (depth > 8) return
            val children = dir.listFiles() ?: return

            if (children.isEmpty()) {
                emptyDirs.add(fileEngine.createFileItem(dir))
            } else {
                for (child in children) {
                    if (child.isDirectory && !child.name.startsWith(".")) {
                        scan(child, depth + 1)
                    }
                }
            }
        }

        scan(root, 0)
        emptyDirs
    }
}
