package com.example.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class BrowserTab(
    val id: String,
    val name: String,
    val currentPath: String,
    val backStack: List<String> = emptyList(),
    val forwardStack: List<String> = emptyList()
)

class FileBrowserViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FileManagerRepository(application, UserPreferencesRepository(application))
    val userPrefs = repository.userPrefs

    val viewMode: StateFlow<ViewMode> = userPrefs.viewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    val sortOption: StateFlow<FileSortOption> = userPrefs.sortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FileSortOption())

    val showHiddenFiles: StateFlow<Boolean> = userPrefs.showHiddenFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Tabs
    private val defaultPath = Environment.getExternalStorageDirectory()?.absolutePath ?: application.filesDir.absolutePath

    private val _tabs = MutableStateFlow(
        listOf(
            BrowserTab(id = "tab_1", name = "Internal Storage", currentPath = defaultPath)
        )
    )
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("tab_1")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val currentTab: StateFlow<BrowserTab?> = combine(_tabs, _activeTabId) { tabs, activeId ->
        tabs.find { it.id == activeId } ?: tabs.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dual Pane second pane path
    private val _secondaryPath = MutableStateFlow(
        File(defaultPath, "Pictures").takeIf { it.exists() }?.absolutePath ?: defaultPath
    )
    val secondaryPath: StateFlow<String> = _secondaryPath.asStateFlow()

    // File list for current active tab
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    // Secondary file list for dual pane right panel
    private val _secondaryFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val secondaryFiles: StateFlow<List<FileItem>> = _secondaryFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Selection
    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedFiles.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val activeCategoryFilter = MutableStateFlow<CategoryType?>(null)

    init {
        // Observe preferences & current tab to reload files reactively
        combine(currentTab, showHiddenFiles, sortOption, searchQuery, activeCategoryFilter) { tab, hidden, sort, query, cat ->
            if (tab != null) {
                loadDirectory(tab.currentPath, hidden, sort, query, cat)
            }
        }.launchIn(viewModelScope)

        _secondaryPath.onEach { path ->
            loadSecondaryDirectory(path)
        }.launchIn(viewModelScope)
    }

    fun navigateTo(path: String) {
        activeCategoryFilter.value = null
        val activeId = _activeTabId.value
        val tabList = _tabs.value.toMutableList()
        val index = tabList.indexOfFirst { it.id == activeId }
        if (index != -1) {
            val oldTab = tabList[index]
            if (oldTab.currentPath != path) {
                val newBackStack = oldTab.backStack + oldTab.currentPath
                val folderName = File(path).name.ifBlank { "Storage" }
                tabList[index] = oldTab.copy(
                    name = folderName,
                    currentPath = path,
                    backStack = newBackStack,
                    forwardStack = emptyList()
                )
                _tabs.value = tabList
                clearSelection()
            }
        }
    }

    fun navigateBack(): Boolean {
        val activeId = _activeTabId.value
        val tabList = _tabs.value.toMutableList()
        val index = tabList.indexOfFirst { it.id == activeId }
        if (index != -1) {
            val oldTab = tabList[index]
            if (oldTab.backStack.isNotEmpty()) {
                val previousPath = oldTab.backStack.last()
                val newBackStack = oldTab.backStack.dropLast(1)
                val newForwardStack = oldTab.forwardStack + oldTab.currentPath
                val folderName = File(previousPath).name.ifBlank { "Storage" }

                tabList[index] = oldTab.copy(
                    name = folderName,
                    currentPath = previousPath,
                    backStack = newBackStack,
                    forwardStack = newForwardStack
                )
                _tabs.value = tabList
                clearSelection()
                return true
            }
        }
        return false
    }

    fun navigateForward() {
        val activeId = _activeTabId.value
        val tabList = _tabs.value.toMutableList()
        val index = tabList.indexOfFirst { it.id == activeId }
        if (index != -1) {
            val oldTab = tabList[index]
            if (oldTab.forwardStack.isNotEmpty()) {
                val nextPath = oldTab.forwardStack.last()
                val newForwardStack = oldTab.forwardStack.dropLast(1)
                val newBackStack = oldTab.backStack + oldTab.currentPath
                val folderName = File(nextPath).name.ifBlank { "Storage" }

                tabList[index] = oldTab.copy(
                    name = folderName,
                    currentPath = nextPath,
                    backStack = newBackStack,
                    forwardStack = newForwardStack
                )
                _tabs.value = tabList
                clearSelection()
            }
        }
    }

    fun addTab(path: String = defaultPath) {
        val newId = "tab_${System.currentTimeMillis()}"
        val folderName = File(path).name.ifBlank { "Storage" }
        val newTab = BrowserTab(id = newId, name = folderName, currentPath = path)
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newId
        clearSelection()
    }

    fun closeTab(tabId: String) {
        if (_tabs.value.size <= 1) return
        val remaining = _tabs.value.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.last().id
        }
        clearSelection()
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        clearSelection()
    }

    fun setSecondaryPath(path: String) {
        _secondaryPath.value = path
    }

    private fun loadDirectory(
        path: String,
        hidden: Boolean,
        sort: FileSortOption,
        query: String,
        categoryFilter: CategoryType?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            if (query.isNotBlank()) {
                val results = repository.searchFiles(query, path, categoryFilter)
                _files.value = results
            } else if (categoryFilter != null && categoryFilter != CategoryType.ALL) {
                val categoryList = repository.getCategoryFiles(categoryFilter, hidden, sort)
                _files.value = categoryList
            } else {
                val list = repository.listDirectory(path, hidden, sort, categoryFilter)
                _files.value = list
            }
            _isLoading.value = false
        }
    }

    fun clearCategoryFilter() {
        activeCategoryFilter.value = null
    }

    private fun loadSecondaryDirectory(path: String) {
        viewModelScope.launch {
            val list = repository.listDirectory(path, showHiddenFiles.value, sortOption.value)
            _secondaryFiles.value = list
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(item: FileItem) {
        val set = _selectedFiles.value.toMutableSet()
        if (set.contains(item)) set.remove(item) else set.add(item)
        _selectedFiles.value = set
    }

    fun selectAll() {
        _selectedFiles.value = _files.value.toSet()
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { userPrefs.setViewMode(mode) }
    }

    fun setSortOption(sortOption: FileSortOption) {
        viewModelScope.launch { userPrefs.setSortOption(sortOption) }
    }

    fun toggleFavorite(fileItem: FileItem) {
        viewModelScope.launch {
            repository.toggleFavorite(fileItem)
            refreshCurrent()
        }
    }

    fun toggleBookmarkCurrent() {
        val tab = currentTab.value ?: return
        viewModelScope.launch {
            repository.toggleBookmark(tab.currentPath, tab.name)
        }
    }

    fun createFolder(name: String, targetPath: String? = null, onResult: (Boolean, String, FileItem?) -> Unit) {
        val path = targetPath ?: currentTab.value?.currentPath ?: return
        viewModelScope.launch {
            val res = repository.createFolder(path, name)
            res.fold(
                onSuccess = { createdDir ->
                    refreshCurrent()
                    onResult(true, "Folder created", createdDir)
                },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Failed", null) }
            )
        }
    }

    fun createNewFile(name: String, content: String = "", targetPath: String? = null, onResult: (Boolean, String, FileItem?) -> Unit) {
        val path = targetPath ?: currentTab.value?.currentPath ?: return
        viewModelScope.launch {
            val res = repository.createNewFile(path, name, content)
            res.fold(
                onSuccess = { createdFile ->
                    refreshCurrent()
                    onResult(true, "File created successfully", createdFile)
                },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Failed", null) }
            )
        }
    }

    fun renameFile(filePath: String, newName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.renameFile(filePath, newName)
            res.fold(
                onSuccess = {
                    refreshCurrent()
                    onResult(true, "Renamed successfully")
                },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Failed") }
            )
        }
    }

    fun deleteSelected(permanent: Boolean = false, onResult: (String) -> Unit) {
        val selected = _selectedFiles.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var count = 0
            for (item in selected) {
                val res = if (permanent) repository.deletePermanently(item.path) else repository.moveToRecycleBin(item.path)
                if (res.getOrDefault(false)) count++
            }
            clearSelection()
            refreshCurrent()
            onResult(if (permanent) "Permanently deleted $count item(s)" else "Moved $count item(s) to Recycle Bin")
        }
    }

    fun refreshCurrent() {
        val tab = currentTab.value ?: return
        loadDirectory(tab.currentPath, showHiddenFiles.value, sortOption.value, searchQuery.value, activeCategoryFilter.value)
        loadSecondaryDirectory(secondaryPath.value)
    }

    // Copy selected files to target path via Queue Manager
    fun copySelectedTo(targetPath: String) {
        val selected = _selectedFiles.value.map { File(it.path) }
        if (selected.isNotEmpty()) {
            repository.queueManager.startCopyOperation(selected, File(targetPath)) {
                refreshCurrent()
            }
            clearSelection()
        }
    }

    // Move selected files to target path via Queue Manager
    fun moveSelectedTo(targetPath: String) {
        val selected = _selectedFiles.value.map { File(it.path) }
        if (selected.isNotEmpty()) {
            repository.queueManager.startMoveOperation(selected, File(targetPath)) {
                refreshCurrent()
            }
            clearSelection()
        }
    }

    // Compress selected
    fun compressSelected(zipName: String, level: CompressionLevel) {
        val tab = currentTab.value ?: return
        val selected = _selectedFiles.value.map { File(it.path) }
        val outputZip = File(tab.currentPath, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
        if (selected.isNotEmpty()) {
            repository.queueManager.startCompressOperation(selected, outputZip, level) {
                refreshCurrent()
            }
            clearSelection()
        }
    }

    // Extract zip
    fun extractZip(zipFile: File, targetDir: File) {
        repository.queueManager.startExtractOperation(zipFile, targetDir) {
            refreshCurrent()
        }
    }
}
