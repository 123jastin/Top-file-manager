package com.example.engine

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.data.db.AppDatabase
import com.example.data.db.RecycleBinEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileEngine(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val recycleBinDao = db.recycleBinDao()

    val recycleBinFolder: File
        get() {
            val dir = File(context.filesDir, "recycle_bin")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val vaultFolder: File
        get() {
            val dir = File(context.filesDir, "vault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getStorageLocations(): List<StorageLocation> {
        val locations = mutableListOf<StorageLocation>()

        // Internal Storage using real StatFs values
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage != null && primaryStorage.exists()) {
            val (total, free) = try {
                val stat = StatFs(primaryStorage.path)
                val blockSize = stat.blockSizeLong
                val totalBytes = stat.blockCountLong * blockSize
                val freeBytes = stat.availableBlocksLong * blockSize
                Pair(totalBytes, freeBytes)
            } catch (e: Exception) {
                Pair(primaryStorage.totalSpace, primaryStorage.freeSpace)
            }
            val used = (total - free).coerceAtLeast(0L)

            locations.add(
                StorageLocation(
                    id = "internal",
                    name = "Internal Storage",
                    path = primaryStorage.absolutePath,
                    totalBytes = total,
                    freeBytes = free,
                    usedBytes = used,
                    type = StorageType.INTERNAL
                )
            )
        }

        // Secondary / SD Card / USB OTG
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs.filterNotNull().forEachIndexed { index, file ->
            if (index > 0) { // Index 0 is internal storage app dir
                val root = getStorageRoot(file)
                if (root != null && root.exists()) {
                    val total = root.totalSpace
                    val free = root.freeSpace
                    val used = total - free
                    val isUsb = root.name.lowercase().contains("usb") || root.path.lowercase().contains("usb")
                    locations.add(
                        StorageLocation(
                            id = if (isUsb) "usb_$index" else "sdcard_$index",
                            name = if (isUsb) "USB Storage $index" else "SD Card $index",
                            path = root.absolutePath,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = used,
                            type = if (isUsb) StorageType.USB_OTG else StorageType.SD_CARD
                        )
                    )
                }
            }
        }

        // Fallback if primaryStorage was not accessible directly
        if (locations.isEmpty()) {
            val internalFallback = context.filesDir
            locations.add(
                StorageLocation(
                    id = "app_internal",
                    name = "Internal Storage",
                    path = internalFallback.absolutePath,
                    totalBytes = internalFallback.totalSpace,
                    freeBytes = internalFallback.freeSpace,
                    usedBytes = internalFallback.totalSpace - internalFallback.freeSpace,
                    type = StorageType.INTERNAL
                )
            )
        }

        return locations
    }

    private fun getStorageRoot(file: File): File? {
        var current: File? = file
        while (current != null && current.parentFile != null) {
            val parent = current.parentFile
            if (parent?.name == "storage" || parent?.name == "mnt") {
                return current
            }
            current = parent
        }
        return file
    }

    suspend fun listFiles(
        dirPath: String,
        showHidden: Boolean = false,
        sortOption: FileSortOption = FileSortOption(),
        filterCategory: CategoryType? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()

        var items = files.asSequence()
            .filter { showHidden || !it.isHidden }
            .map { file -> createFileItem(file) }
            .toList()

        if (filterCategory != null && filterCategory != CategoryType.ALL) {
            items = items.filter { item ->
                if (item.isDirectory) true
                else matchesCategory(item, filterCategory)
            }
        }

        sortFileItems(items, sortOption)
    }

    fun createFileItem(file: File): FileItem {
        val isDir = file.isDirectory
        val extension = if (isDir) "" else file.extension.lowercase()
        val mime = if (isDir) "resource/folder" else getMimeType(file)
        
        var fileCount = 0
        var folderCount = 0
        if (isDir) {
            val children = file.listFiles()
            if (children != null) {
                fileCount = children.count { !it.isDirectory }
                folderCount = children.count { it.isDirectory }
            }
        }

        return FileItem(
            path = file.absolutePath,
            name = file.name,
            extension = extension,
            size = if (isDir) getFolderSizeFast(file) else file.length(),
            lastModified = file.lastModified(),
            isDirectory = isDir,
            mimeType = mime,
            childFileCount = fileCount,
            childFolderCount = folderCount,
            isHidden = file.name.startsWith(".")
        )
    }

    private fun getFolderSizeFast(file: File, maxDepth: Int = 2, currentDepth: Int = 0): Long {
        if (currentDepth > maxDepth) return 0L
        val children = file.listFiles() ?: return 0L
        var total = 0L
        for (child in children) {
            total += if (child.isDirectory) {
                getFolderSizeFast(child, maxDepth, currentDepth + 1)
            } else {
                child.length()
            }
        }
        return total
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        if (ext == "apk") return "application/vnd.android.package-archive"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return mime ?: "*/*"
    }

    fun sortFileItems(items: List<FileItem>, sortOption: FileSortOption): List<FileItem> {
        val comparator = Comparator<FileItem> { f1, f2 ->
            if (sortOption.foldersFirst) {
                if (f1.isDirectory && !f2.isDirectory) return@Comparator -1
                if (!f1.isDirectory && f2.isDirectory) return@Comparator 1
            }

            val result = when (sortOption.field) {
                SortField.NAME -> f1.name.compareTo(f2.name, ignoreCase = true)
                SortField.DATE -> f1.lastModified.compareTo(f2.lastModified)
                SortField.SIZE -> f1.size.compareTo(f2.size)
                SortField.TYPE -> f1.extension.compareTo(f2.extension, ignoreCase = true)
            }

            if (sortOption.ascending) result else -result
        }

        return items.sortedWith(comparator)
    }

    fun matchesCategory(item: FileItem, category: CategoryType): Boolean {
        if (item.isDirectory) return false
        val ext = item.extension.lowercase()
        val mime = item.mimeType.lowercase()

        return when (category) {
            CategoryType.IMAGES -> mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
            CategoryType.VIDEOS -> mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv")
            CategoryType.AUDIO -> mime.startsWith("audio/") || ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a")
            CategoryType.DOCUMENTS -> mime.contains("pdf") || mime.contains("text") || mime.contains("word") || mime.contains("sheet") || ext in listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml", "md", "html")
            CategoryType.DOWNLOADS -> item.path.lowercase().contains("download")
            CategoryType.ARCHIVES -> ext in listOf("zip", "7z", "tar", "gz", "bz2", "xz", "rar", "tgz")
            CategoryType.APKS -> ext == "apk"
            CategoryType.RECENT -> (System.currentTimeMillis() - item.lastModified) <= 7 * 24 * 60 * 60 * 1000L
            CategoryType.VAULT -> item.isVault
            CategoryType.ALL -> true
        }
    }

    suspend fun getFilesByCategory(
        category: CategoryType,
        showHidden: Boolean = false,
        sortOption: FileSortOption = FileSortOption()
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (category == CategoryType.ALL) return@withContext emptyList()
        val itemsMap = mutableMapOf<String, FileItem>()

        // 1. Query Android MediaStore ContentResolver for fast indexing across device
        runCatching {
            val uriAndColumn = when (category) {
                CategoryType.IMAGES -> Pair(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA
                )
                CategoryType.VIDEOS -> Pair(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA
                )
                CategoryType.AUDIO -> Pair(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.DATA
                )
                else -> Pair(
                    MediaStore.Files.getContentUri("external"),
                    MediaStore.Files.FileColumns.DATA
                )
            }

            context.contentResolver.query(
                uriAndColumn.first,
                arrayOf(uriAndColumn.second),
                null, null, null
            )?.use { cursor ->
                val columnIndex = cursor.getColumnIndex(uriAndColumn.second)
                if (columnIndex != -1) {
                    while (cursor.moveToNext()) {
                        val filePath = cursor.getString(columnIndex) ?: continue
                        val file = File(filePath)
                        if (file.exists() && file.isFile && (showHidden || !file.isHidden)) {
                            val item = createFileItem(file)
                            if (matchesCategory(item, category)) {
                                itemsMap[file.absolutePath] = item
                            }
                        }
                    }
                }
            }
        }

        // 2. Perform direct storage file system walk to catch non-indexed files / folders
        val storageRoot = Environment.getExternalStorageDirectory()
        if (storageRoot != null && storageRoot.exists()) {
            fun walk(dir: File, depth: Int) {
                if (depth > 8 || itemsMap.size > 2000) return
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (!showHidden && f.name.startsWith(".")) continue
                    if (f.isFile) {
                        if (!itemsMap.containsKey(f.absolutePath)) {
                            val item = createFileItem(f)
                            if (matchesCategory(item, category)) {
                                itemsMap[f.absolutePath] = item
                            }
                        }
                    } else if (f.isDirectory) {
                        if (f.name.equals("Android", ignoreCase = true) && depth == 0) continue
                        walk(f, depth + 1)
                    }
                }
            }

            walk(storageRoot, 0)
        }

        val sortedList = itemsMap.values.toList()
        sortFileItems(sortedList, sortOption)
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitized = folderName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
            if (sanitized.isEmpty()) throw IllegalArgumentException("Folder name cannot be empty")
            val newDir = File(parentPath, sanitized)
            if (newDir.exists()) throw IllegalStateException("Folder '$sanitized' already exists")
            val created = newDir.mkdirs()
            if (!created && !newDir.exists()) throw IllegalStateException("Failed to create folder")
            createFileItem(newDir)
        }
    }

    suspend fun renameFile(filePath: String, newName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) throw IllegalStateException("File does not exist")

            val parent = file.parentFile ?: throw IllegalStateException("Parent folder missing")
            val isDir = file.isDirectory
            
            val targetName = if (!isDir && file.extension.isNotEmpty() && !newName.contains(".")) {
                "$newName.${file.extension}"
            } else {
                newName
            }

            val targetFile = File(parent, targetName)
            if (targetFile.exists()) throw IllegalStateException("File with name '$targetName' already exists")

            val renamed = file.renameTo(targetFile)
            if (!renamed) throw IllegalStateException("Failed to rename file")

            createFileItem(targetFile)
        }
    }

    suspend fun moveToRecycleBin(filePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) throw IllegalStateException("File not found")

            val trashName = "${System.currentTimeMillis()}_${sourceFile.name}"
            val trashTarget = File(recycleBinFolder, trashName)

            val moved = if (sourceFile.renameTo(trashTarget)) {
                true
            } else {
                // Fallback copy then delete if cross-filesystem
                sourceFile.copyTo(trashTarget, overwrite = true)
                sourceFile.deleteRecursively()
            }

            if (moved) {
                recycleBinDao.insertItem(
                    RecycleBinEntity(
                        originalPath = sourceFile.absolutePath,
                        trashPath = trashTarget.absolutePath,
                        fileName = sourceFile.name,
                        deletedTimestamp = System.currentTimeMillis(),
                        fileSize = if (sourceFile.isDirectory) getFolderSizeFast(sourceFile, 10) else sourceFile.length(),
                        isDirectory = sourceFile.isDirectory
                    )
                )
                true
            } else false
        }
    }

    suspend fun restoreFromRecycleBin(item: RecycleBinEntity, alternatePath: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) throw IllegalStateException("Trash file no longer exists")

            val destinationPath = alternatePath ?: item.originalPath
            val destFile = File(destinationPath)

            destFile.parentFile?.mkdirs()

            val restored = if (trashFile.renameTo(destFile)) {
                true
            } else {
                trashFile.copyTo(destFile, overwrite = true)
                trashFile.deleteRecursively()
            }

            if (restored) {
                recycleBinDao.deleteById(item.id)
                true
            } else false
        }
    }

    suspend fun deletePermanently(filePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (file.exists()) {
                file.deleteRecursively()
            } else true
        }
    }

    suspend fun searchFiles(
        rootPath: String,
        query: String,
        filterCategory: CategoryType? = null,
        maxResults: Int = 200
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()

        val results = mutableListOf<FileItem>()
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()

        val q = query.lowercase().trim()

        fun walk(dir: File, depth: Int) {
            if (results.size >= maxResults || depth > 8) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (results.size >= maxResults) break
                val matchesName = f.name.lowercase().contains(q)

                if (matchesName) {
                    val item = createFileItem(f)
                    if (filterCategory == null || filterCategory == CategoryType.ALL || matchesCategory(item, filterCategory)) {
                        results.add(item)
                    }
                }

                if (f.isDirectory && !f.name.startsWith(".")) {
                    walk(f, depth + 1)
                }
            }
        }

        walk(root, 0)
        results
    }

    suspend fun getRecentFiles(limit: Int = 30): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory() ?: return@withContext emptyList()
        val recentItems = mutableListOf<FileItem>()

        fun scan(dir: File, depth: Int) {
            if (recentItems.size >= limit * 2 || depth > 4) return
            val files = dir.listFiles() ?: return

            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isFile) {
                    val item = createFileItem(f)
                    if (matchesCategory(item, CategoryType.RECENT)) {
                        recentItems.add(item)
                    }
                } else if (f.isDirectory) {
                    scan(f, depth + 1)
                }
            }
        }

        scan(root, 0)
        recentItems.sortedByDescending { it.lastModified }.take(limit)
    }
}
