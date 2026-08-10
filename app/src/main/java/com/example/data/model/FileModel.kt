package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FileItem(
    val path: String,
    val name: String,
    val extension: String = "",
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val mimeType: String = "*/*",
    val childFileCount: Int = 0,
    val childFolderCount: Int = 0,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val isVault: Boolean = false,
    val thumbnailUri: String? = null
)

enum class StorageType {
    INTERNAL, SD_CARD, USB_OTG, SAF, NETWORK
}

@Immutable
data class StorageLocation(
    val id: String,
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val type: StorageType
) {
    val usedRatio: Float
        get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
}

enum class CategoryType {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, DOWNLOADS, ARCHIVES, APKS, RECENT, VAULT, ALL
}

@Immutable
data class FileCategory(
    val type: CategoryType,
    val displayName: String,
    val fileCount: Int = 0,
    val totalSize: Long = 0L
)

enum class SortField {
    NAME, DATE, SIZE, TYPE
}

@Immutable
data class FileSortOption(
    val field: SortField = SortField.NAME,
    val ascending: Boolean = true,
    val foldersFirst: Boolean = true
)

enum class ViewMode {
    LIST, COMPACT, GRID
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AccentColor(val colorHex: Long) {
    ELECTRIC_BLUE(0xFF3B82F6),
    EMERALD_GREEN(0xFF10B981),
    VIBRANT_CYAN(0xFF06B6D4),
    SUNSET_ORANGE(0xFFF97316),
    ROYAL_PURPLE(0xFF8B5CF6)
}

enum class FileConflictOption {
    REPLACE, SKIP, KEEP_BOTH, RENAME_AUTO
}

enum class CompressionLevel(val zipLevel: Int) {
    STORE(0),
    FAST(1),
    NORMAL(6),
    MAXIMUM(9)
}

enum class ArchiveFormat {
    ZIP, TAR, GZIP, BZIP2, XZ
}

enum class TaskType {
    COPY, MOVE, DELETE, COMPRESS, EXTRACT, CHECKSUM
}

@Immutable
data class TaskProgress(
    val id: String,
    val type: TaskType,
    val title: String,
    val currentFileName: String = "",
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercentage: Float = 0f,
    val speedBytesPerSec: Long = 0L,
    val estimatedTimeMs: Long = 0L,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val error: String? = null
)

@Immutable
data class DuplicateGroup(
    val checksum: String,
    val size: Long,
    val files: List<FileItem>
)

@Immutable
data class StorageBreakdown(
    val categoryName: String,
    val bytes: Long,
    val fileCount: Int,
    val colorHex: Long
)

@Immutable
data class ChecksumResult(
    val filePath: String,
    val md5: String,
    val sha1: String,
    val sha256: String,
    val sha512: String,
    val durationMs: Long
)

@Immutable
data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isInstalled: Boolean,
    val size: Long,
    val apkPath: String
)
