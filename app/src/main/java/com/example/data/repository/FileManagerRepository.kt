package com.example.data.repository

import android.content.Context
import com.example.data.db.*
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository(
    val context: Context,
    val userPrefs: UserPreferencesRepository
) {

    val db = AppDatabase.getDatabase(context)
    val recycleBinDao = db.recycleBinDao()
    val favoriteDao = db.favoriteDao()
    val bookmarkDao = db.bookmarkDao()
    val vaultDao = db.vaultDao()

    val fileEngine = FileEngine(context)
    val archiveEngine = ArchiveEngine()
    val checksumEngine = ChecksumEngine()
    val storageAnalyzerEngine = StorageAnalyzerEngine(fileEngine)
    val mediaEngine = MediaEngine(context)
    val apkEngine = ApkEngine(context)
    val vaultEngine = VaultEngine(context, fileEngine)
    val queueManager = OperationQueueManager(fileEngine, archiveEngine, checksumEngine)

    val favoritesFlow: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    val bookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val recycleBinFlow: Flow<List<RecycleBinEntity>> = recycleBinDao.getAllRecycleBinItems()
    val vaultFilesFlow: Flow<List<VaultEntity>> = vaultDao.getAllVaultFiles()

    suspend fun getStorageLocations(): List<StorageLocation> = withContext(Dispatchers.IO) {
        fileEngine.getStorageLocations()
    }

    suspend fun listDirectory(
        path: String,
        showHidden: Boolean,
        sortOption: FileSortOption,
        filterCategory: CategoryType? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val rawItems = fileEngine.listFiles(path, showHidden, sortOption, filterCategory)
        // Enrich with favorite status
        rawItems.map { item ->
            val isFav = favoriteDao.isFavorite(item.path)
            item.copy(isFavorite = isFav)
        }
    }

    suspend fun toggleFavorite(fileItem: FileItem) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(fileItem.path)
        if (isFav) {
            favoriteDao.removeFavorite(fileItem.path)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    path = fileItem.path,
                    name = fileItem.name,
                    isDirectory = fileItem.isDirectory
                )
            )
        }
    }

    suspend fun toggleBookmark(folderPath: String, folderName: String) = withContext(Dispatchers.IO) {
        bookmarkDao.addBookmark(BookmarkEntity(path = folderPath, name = folderName))
    }

    suspend fun removeBookmark(folderPath: String) = withContext(Dispatchers.IO) {
        bookmarkDao.removeBookmark(folderPath)
    }

    suspend fun createFolder(parentPath: String, name: String): Result<FileItem> {
        return fileEngine.createFolder(parentPath, name)
    }

    suspend fun createNewFile(parentPath: String, name: String, content: String = ""): Result<FileItem> {
        return fileEngine.createNewFile(parentPath, name, content)
    }

    suspend fun renameFile(filePath: String, newName: String): Result<FileItem> {
        return fileEngine.renameFile(filePath, newName)
    }

    suspend fun moveToRecycleBin(filePath: String): Result<Boolean> {
        return fileEngine.moveToRecycleBin(filePath)
    }

    suspend fun restoreRecycleBinItem(item: RecycleBinEntity, alternatePath: String? = null): Result<Boolean> {
        return fileEngine.restoreFromRecycleBin(item, alternatePath)
    }

    suspend fun deletePermanently(filePath: String): Result<Boolean> {
        return fileEngine.deletePermanently(filePath)
    }

    suspend fun emptyRecycleBin() = withContext(Dispatchers.IO) {
        val allItems = recycleBinDao.getAllRecycleBinItems()
        // Delete all physical files in trash directory
        fileEngine.recycleBinFolder.deleteRecursively()
        recycleBinDao.clearAll()
    }

    suspend fun calculateChecksum(file: File, onProgress: (Float) -> Unit): Result<ChecksumResult> {
        return checksumEngine.calculateChecksums(file, onProgress)
    }

    suspend fun searchFiles(query: String, rootPath: String, category: CategoryType?): List<FileItem> {
        return fileEngine.searchFiles(rootPath, query, category)
    }

    suspend fun getRecentFiles(limit: Int = 30): List<FileItem> {
        return fileEngine.getRecentFiles(limit)
    }

    suspend fun getCategoryFiles(category: CategoryType, showHidden: Boolean, sortOption: FileSortOption): List<FileItem> = withContext(Dispatchers.IO) {
        val rawItems = fileEngine.getFilesByCategory(category, showHidden, sortOption)
        rawItems.map { item ->
            val isFav = favoriteDao.isFavorite(item.path)
            item.copy(isFavorite = isFav)
        }
    }
}
