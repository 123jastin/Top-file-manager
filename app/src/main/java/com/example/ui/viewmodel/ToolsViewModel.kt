package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileManagerRepository
import com.example.engine.InstalledAppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FileManagerRepository(application, UserPreferencesRepository(application))

    // Storage breakdown
    private val _breakdown = MutableStateFlow<List<StorageBreakdown>>(emptyList())
    val breakdown: StateFlow<List<StorageBreakdown>> = _breakdown.asStateFlow()

    // Large files
    private val _largeFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val largeFiles: StateFlow<List<FileItem>> = _largeFiles.asStateFlow()

    // Duplicates
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    // Empty folders
    private val _emptyFolders = MutableStateFlow<List<FileItem>>(emptyList())
    val emptyFolders: StateFlow<List<FileItem>> = _emptyFolders.asStateFlow()

    // Checksum
    private val _checksumResult = MutableStateFlow<ChecksumResult?>(null)
    val checksumResult: StateFlow<ChecksumResult?> = _checksumResult.asStateFlow()

    // APKs
    private val _apks = MutableStateFlow<List<ApkInfo>>(emptyList())
    val apks: StateFlow<List<ApkInfo>> = _apks.asStateFlow()

    // Installed apps
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun runStorageAnalyzer() {
        viewModelScope.launch {
            _isScanning.value = true
            val bd = repository.storageAnalyzerEngine.analyzeStorageBreakdown()
            _breakdown.value = bd

            val lf = repository.storageAnalyzerEngine.findLargeFiles(minSizeBytes = 50 * 1024 * 1024L)
            _largeFiles.value = lf

            _isScanning.value = false
        }
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            _isScanning.value = true
            val dups = repository.storageAnalyzerEngine.findDuplicates()
            _duplicateGroups.value = dups
            _isScanning.value = false
        }
    }

    fun scanEmptyFolders() {
        viewModelScope.launch {
            _isScanning.value = true
            val empty = repository.storageAnalyzerEngine.findEmptyFolders()
            _emptyFolders.value = empty
            _isScanning.value = false
        }
    }

    fun calculateChecksum(filePath: String) {
        viewModelScope.launch {
            _isScanning.value = true
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                val res = repository.calculateChecksum(file) { }
                _checksumResult.value = res.getOrNull()
            }
            _isScanning.value = false
        }
    }

    fun scanApks(rootPath: String) {
        viewModelScope.launch {
            _isScanning.value = true
            val files = repository.fileEngine.searchFiles(rootPath, ".apk", CategoryType.APKS)
            val list = files.mapNotNull { repository.apkEngine.getApkInfo(File(it.path)) }
            _apks.value = list
            _isScanning.value = false
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isScanning.value = true
            val apps = repository.apkEngine.getInstalledUserApps()
            _installedApps.value = apps
            _isScanning.value = false
        }
    }

    fun deleteFile(path: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.moveToRecycleBin(path)
            onComplete()
        }
    }
}
