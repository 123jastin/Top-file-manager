package com.example.engine

import com.example.data.model.CompressionLevel
import com.example.data.model.FileConflictOption
import com.example.data.model.TaskProgress
import com.example.data.model.TaskType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class OperationQueueManager(
    private val fileEngine: FileEngine,
    private val archiveEngine: ArchiveEngine,
    private val checksumEngine: ChecksumEngine
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tasks = MutableStateFlow<Map<String, TaskProgress>>(emptyMap())
    val tasks: StateFlow<Map<String, TaskProgress>> = _tasks.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    fun startCopyOperation(
        sourceFiles: List<File>,
        targetDir: File,
        conflictOption: FileConflictOption = FileConflictOption.RENAME_AUTO,
        onComplete: (Boolean) -> Unit = {}
    ): String {
        val taskId = UUID.randomUUID().toString()
        val title = "Copying ${sourceFiles.size} file(s) to ${targetDir.name}"

        updateTask(
            TaskProgress(
                id = taskId,
                type = TaskType.COPY,
                title = title,
                totalBytes = sourceFiles.sumOf { if (it.isDirectory) 1000000L else it.length() }
            )
        )

        val job = scope.launch {
            try {
                targetDir.mkdirs()
                val totalBytes = sourceFiles.sumOf { if (it.isDirectory) 1000000L else it.length() }
                var transferredBytes = 0L
                val startTime = System.currentTimeMillis()

                for (source in sourceFiles) {
                    if (!isActiveTask(taskId)) break

                    val dest = resolveDestination(source, targetDir, conflictOption)
                    if (dest == null) continue // Skip if conflict skipped

                    copyRecursive(source, dest, taskId) { bytesCopied, currentName ->
                        transferredBytes += bytesCopied
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        val speed = if (elapsedSec > 0) (transferredBytes / elapsedSec).toLong() else 0L
                        val remainingBytes = (totalBytes - transferredBytes).coerceAtLeast(0L)
                        val etaMs = if (speed > 0) (remainingBytes * 1000) / speed else 0L
                        val percentage = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                        updateTask(
                            getTask(taskId)?.copy(
                                currentFileName = currentName,
                                transferredBytes = transferredBytes,
                                totalBytes = totalBytes,
                                progressPercentage = percentage,
                                speedBytesPerSec = speed,
                                estimatedTimeMs = etaMs
                            ) ?: return@copyRecursive
                        )
                    }
                }

                updateTask(
                    getTask(taskId)?.copy(
                        progressPercentage = 1f,
                        isCompleted = true
                    ) ?: return@launch
                )
                onComplete(true)
            } catch (e: Exception) {
                updateTask(
                    getTask(taskId)?.copy(
                        error = e.localizedMessage ?: "Copy failed",
                        isCompleted = true
                    ) ?: return@launch
                )
                onComplete(false)
            }
        }

        activeJobs[taskId] = job
        return taskId
    }

    fun startMoveOperation(
        sourceFiles: List<File>,
        targetDir: File,
        conflictOption: FileConflictOption = FileConflictOption.RENAME_AUTO,
        onComplete: (Boolean) -> Unit = {}
    ): String {
        val taskId = UUID.randomUUID().toString()
        val title = "Moving ${sourceFiles.size} file(s) to ${targetDir.name}"

        updateTask(
            TaskProgress(
                id = taskId,
                type = TaskType.MOVE,
                title = title,
                totalBytes = sourceFiles.sumOf { if (it.isDirectory) 1000000L else it.length() }
            )
        )

        val job = scope.launch {
            try {
                targetDir.mkdirs()
                val totalBytes = sourceFiles.sumOf { if (it.isDirectory) 1000000L else it.length() }
                var transferredBytes = 0L
                val startTime = System.currentTimeMillis()

                for (source in sourceFiles) {
                    if (!isActiveTask(taskId)) break

                    val dest = resolveDestination(source, targetDir, conflictOption)
                    if (dest == null) continue

                    // Try fast atomic rename first
                    if (source.renameTo(dest)) {
                        transferredBytes += if (source.isDirectory) 1000000L else source.length()
                        val percentage = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                        updateTask(
                            getTask(taskId)?.copy(
                                currentFileName = source.name,
                                transferredBytes = transferredBytes,
                                totalBytes = totalBytes,
                                progressPercentage = percentage
                            ) ?: return@launch
                        )
                    } else {
                        // Fallback copy + delete
                        copyRecursive(source, dest, taskId) { bytesCopied, currentName ->
                            transferredBytes += bytesCopied
                            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                            val speed = if (elapsedSec > 0) (transferredBytes / elapsedSec).toLong() else 0L
                            val remainingBytes = (totalBytes - transferredBytes).coerceAtLeast(0L)
                            val etaMs = if (speed > 0) (remainingBytes * 1000) / speed else 0L
                            val percentage = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                            updateTask(
                                getTask(taskId)?.copy(
                                    currentFileName = currentName,
                                    transferredBytes = transferredBytes,
                                    totalBytes = totalBytes,
                                    progressPercentage = percentage,
                                    speedBytesPerSec = speed,
                                    estimatedTimeMs = etaMs
                                ) ?: return@copyRecursive
                            )
                        }
                        source.deleteRecursively()
                    }
                }

                updateTask(
                    getTask(taskId)?.copy(
                        progressPercentage = 1f,
                        isCompleted = true
                    ) ?: return@launch
                )
                onComplete(true)
            } catch (e: Exception) {
                updateTask(
                    getTask(taskId)?.copy(
                        error = e.localizedMessage ?: "Move failed",
                        isCompleted = true
                    ) ?: return@launch
                )
                onComplete(false)
            }
        }

        activeJobs[taskId] = job
        return taskId
    }

    fun startCompressOperation(
        sourceFiles: List<File>,
        outputZipFile: File,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        onComplete: (Boolean) -> Unit = {}
    ): String {
        val taskId = UUID.randomUUID().toString()
        updateTask(
            TaskProgress(
                id = taskId,
                type = TaskType.COMPRESS,
                title = "Compressing ${sourceFiles.size} item(s) to ${outputZipFile.name}"
            )
        )

        val startTime = System.currentTimeMillis()
        val job = scope.launch {
            val result = archiveEngine.createZipArchive(sourceFiles, outputZipFile, compressionLevel) { currentFileName, processedBytes, totalBytes ->
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                val speed = if (elapsedSec > 0) (processedBytes / elapsedSec).toLong() else 0L
                val remainingBytes = (totalBytes - processedBytes).coerceAtLeast(0L)
                val etaMs = if (speed > 0) (remainingBytes * 1000) / speed else 0L
                val percentage = if (totalBytes > 0) (processedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                updateTask(
                    getTask(taskId)?.copy(
                        currentFileName = currentFileName,
                        transferredBytes = processedBytes,
                        totalBytes = totalBytes,
                        progressPercentage = percentage,
                        speedBytesPerSec = speed,
                        estimatedTimeMs = etaMs
                    ) ?: return@createZipArchive
                )
            }

            result.fold(
                onSuccess = {
                    updateTask(getTask(taskId)?.copy(progressPercentage = 1f, isCompleted = true) ?: return@launch)
                    onComplete(true)
                },
                onFailure = { err ->
                    updateTask(getTask(taskId)?.copy(error = err.localizedMessage ?: "Compression failed", isCompleted = true) ?: return@launch)
                    onComplete(false)
                }
            )
        }

        activeJobs[taskId] = job
        return taskId
    }

    fun startExtractOperation(
        zipFile: File,
        targetDir: File,
        onComplete: (Boolean) -> Unit = {}
    ): String {
        val taskId = UUID.randomUUID().toString()
        updateTask(
            TaskProgress(
                id = taskId,
                type = TaskType.EXTRACT,
                title = "Extracting ${zipFile.name} to ${targetDir.name}"
            )
        )

        val startTime = System.currentTimeMillis()
        val job = scope.launch {
            val result = archiveEngine.extractZipArchive(zipFile, targetDir) { currentEntry, extractedBytes, totalBytes ->
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                val speed = if (elapsedSec > 0) (extractedBytes / elapsedSec).toLong() else 0L
                val remainingBytes = (totalBytes - extractedBytes).coerceAtLeast(0L)
                val etaMs = if (speed > 0) (remainingBytes * 1000) / speed else 0L
                val percentage = if (totalBytes > 0) (extractedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                updateTask(
                    getTask(taskId)?.copy(
                        currentFileName = currentEntry,
                        transferredBytes = extractedBytes,
                        totalBytes = totalBytes,
                        progressPercentage = percentage,
                        speedBytesPerSec = speed,
                        estimatedTimeMs = etaMs
                    ) ?: return@extractZipArchive
                )
            }

            result.fold(
                onSuccess = {
                    updateTask(getTask(taskId)?.copy(progressPercentage = 1f, isCompleted = true) ?: return@launch)
                    onComplete(true)
                },
                onFailure = { err ->
                    updateTask(getTask(taskId)?.copy(error = err.localizedMessage ?: "Extraction failed", isCompleted = true) ?: return@launch)
                    onComplete(false)
                }
            )
        }

        activeJobs[taskId] = job
        return taskId
    }

    private fun resolveDestination(source: File, targetDir: File, option: FileConflictOption): File? {
        val target = File(targetDir, source.name)
        if (!target.exists()) return target

        return when (option) {
            FileConflictOption.REPLACE -> {
                target.deleteRecursively()
                target
            }
            FileConflictOption.SKIP -> null
            FileConflictOption.KEEP_BOTH, FileConflictOption.RENAME_AUTO -> {
                var counter = 1
                val base = source.nameWithoutExtension
                val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                var candidate: File
                do {
                    candidate = File(targetDir, "${base}_($counter)$ext")
                    counter++
                } while (candidate.exists())
                candidate
            }
        }
    }

    private fun copyRecursive(
        source: File,
        dest: File,
        taskId: String,
        onProgress: (bytesRead: Long, currentName: String) -> Unit
    ) {
        if (!isActiveTask(taskId)) return

        if (source.isDirectory) {
            dest.mkdirs()
            source.listFiles()?.forEach { child ->
                val childDest = File(dest, child.name)
                copyRecursive(child, childDest, taskId, onProgress)
            }
        } else {
            dest.parentFile?.mkdirs()
            FileInputStream(source).use { fis ->
                FileOutputStream(dest).use { fos ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActiveTask(taskId)) break
                        fos.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), source.name)
                    }
                }
            }
        }
    }

    fun cancelTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTask(
            getTask(taskId)?.copy(
                isCancelled = true,
                isCompleted = true
            ) ?: return
        )
    }

    fun clearCompletedTasks() {
        _tasks.value = _tasks.value.filterValues { !it.isCompleted }
    }

    private fun isActiveTask(taskId: String): Boolean {
        val t = getTask(taskId)
        return t != null && !t.isCancelled && !t.isCompleted
    }

    private fun getTask(taskId: String): TaskProgress? = _tasks.value[taskId]

    private fun updateTask(taskProgress: TaskProgress) {
        val current = _tasks.value.toMutableMap()
        current[taskProgress.id] = taskProgress
        _tasks.value = current
    }
}
