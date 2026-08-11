package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FileBrowserViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onOpenFile: (FileItem) -> Unit
) {
    val context = LocalContext.current

    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    val files by viewModel.files.collectAsState()
    val secondaryFiles by viewModel.secondaryFiles.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()

    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCategoryFilter by viewModel.activeCategoryFilter.collectAsState()

    val activeTasks by viewModel.repository.queueManager.tasks.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var fileToExtract by remember { mutableStateOf<FileItem?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileDetails by remember { mutableStateOf<FileItem?>(null) }
    var fileToVault by remember { mutableStateOf<FileItem?>(null) }
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var vaultPinInput by remember { mutableStateOf("") }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showProgressSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isDualPane by remember { mutableStateOf(false) }
    var mediaPreviewFile by remember { mutableStateOf<FileItem?>(null) }
    var fullScreenImageFile by remember { mutableStateOf<FileItem?>(null) }

    // File contextual action menu item
    var activeContextFile by remember { mutableStateOf<FileItem?>(null) }

    val activeTasksCount = activeTasks.values.count { !it.isCompleted }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files & folders...") },
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("browser_search_input"),
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    actions = {
                        // Queue operations trigger
                        if (activeTasksCount > 0) {
                            BadgeBox(count = activeTasksCount) {
                                IconButton(onClick = { showProgressSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Active Operations",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // View mode toggle button
                        IconButton(onClick = {
                            val next = when (viewMode) {
                                ViewMode.LIST -> ViewMode.GRID
                                ViewMode.GRID -> ViewMode.COMPACT
                                ViewMode.COMPACT -> ViewMode.LIST
                            }
                            viewModel.setViewMode(next)
                        }) {
                            val icon = when (viewMode) {
                                ViewMode.LIST -> Icons.Default.GridView
                                ViewMode.GRID -> Icons.Default.ViewList
                                ViewMode.COMPACT -> Icons.Default.ViewAgenda
                            }
                            Icon(imageVector = icon, contentDescription = "Toggle View")
                        }

                        // Sort Menu Trigger
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                        }

                        // Options Menu Trigger
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }

                        // Sort Menu Dropdown
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption(SortField.NAME, !sortOption.ascending))
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption.field == SortField.NAME) Icon(Icons.Default.Check, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date") },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption(SortField.DATE, !sortOption.ascending))
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption.field == SortField.DATE) Icon(Icons.Default.Check, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption(SortField.SIZE, !sortOption.ascending))
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption.field == SortField.SIZE) Icon(Icons.Default.Check, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Type") },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption(SortField.TYPE, !sortOption.ascending))
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption.field == SortField.TYPE) Icon(Icons.Default.Check, null)
                                }
                            )
                        }

                        // Options Dropdown Menu
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Folder") },
                                onClick = {
                                    showCreateFolderDialog = true
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("New File / Note") },
                                onClick = {
                                    showCreateFileDialog = true
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("New Tab") },
                                onClick = {
                                    viewModel.addTab()
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files") },
                                onClick = {
                                    scope.launch {
                                        viewModel.userPrefs.setShowHiddenFiles(!showHiddenFiles)
                                    }
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Visibility, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDualPane) "Single Pane View" else "Dual Pane View") },
                                onClick = {
                                    isDualPane = !isDualPane
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ViewColumn, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = {
                                    viewModel.refreshCurrent()
                                    showOptionsMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                // Multi-tab bar
                if (tabs.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isActive = tab.id == activeTabId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { viewModel.selectTab(tab.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tab.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (tabs.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.closeTab(tab.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Breadcrumb path bar
                if (currentTab != null) {
                    BreadcrumbBar(
                        currentPath = currentTab!!.currentPath,
                        canGoBack = currentTab!!.backStack.isNotEmpty(),
                        canGoForward = currentTab!!.forwardStack.isNotEmpty(),
                        onNavigateBack = { viewModel.navigateBack() },
                        onNavigateForward = { viewModel.navigateForward() },
                        onHomeClick = { viewModel.navigateTo(viewModel.repository.fileEngine.getStorageLocations().firstOrNull()?.path ?: "") },
                        onPathClick = { target -> viewModel.navigateTo(target) },
                        onRefresh = { viewModel.refreshCurrent() }
                    )
                }

                // Active Category Filter Banner
                if (activeCategoryFilter != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Category: ${activeCategoryFilter?.name?.lowercase()?.replaceFirstChar { it.uppercase() }} (${files.size} items)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearCategoryFilter() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Category Filter",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionActionBar(
                    selectedFiles = selectedFiles,
                    totalFilesCount = files.size,
                    onSelectAll = {
                        if (selectedFiles.size == files.size) viewModel.clearSelection() else viewModel.selectAll()
                    },
                    onClearSelection = { viewModel.clearSelection() },
                    onCopyClick = {
                        val currentPath = currentTab?.currentPath ?: ""
                        viewModel.copySelectedTo(currentPath)
                        Toast.makeText(context, "Copying items...", Toast.LENGTH_SHORT).show()
                    },
                    onMoveClick = {
                        val currentPath = currentTab?.currentPath ?: ""
                        viewModel.moveSelectedTo(currentPath)
                        Toast.makeText(context, "Moving items...", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteClick = {
                        viewModel.deleteSelected { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCompressClick = { showCompressDialog = true },
                    onShareClick = {
                        shareSelectedFiles(context, selectedFiles.toList())
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_create")
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "New Item"
                        )
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            onClick = {
                                showFabMenu = false
                                showCreateFolderDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("New File / Document") },
                            onClick = {
                                showFabMenu = false
                                showCreateFileDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, null) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (files.isEmpty()) {
                EmptyState(
                    query = searchQuery,
                    onCreateFolderClick = { showCreateFolderDialog = true },
                    onCreateFileClick = { showCreateFileDialog = true }
                )
            } else {
                if (isDualPane) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Pane
                        Box(modifier = Modifier.weight(1f)) {
                            FileGridOrList(
                                files = files,
                                viewMode = viewMode,
                                selectedFiles = selectedFiles,
                                isSelectionMode = isSelectionMode,
                                onFileClick = { file ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(file)
                                    } else if (file.isDirectory) {
                                        viewModel.navigateTo(file.path)
                                    } else if (file.extension in listOf("zip", "rar", "7z", "tar", "gz", "apk")) {
                                        fileToExtract = file
                                    } else {
                                        onOpenFile(file)
                                    }
                                },
                                onFileLongClick = { file -> viewModel.toggleSelection(file) },
                                onMenuClick = { file -> activeContextFile = file }
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Right Pane
                        Box(modifier = Modifier.weight(1f)) {
                            FileGridOrList(
                                files = secondaryFiles,
                                viewMode = viewMode,
                                selectedFiles = emptySet(),
                                isSelectionMode = false,
                                onFileClick = { file ->
                                    if (file.isDirectory) viewModel.setSecondaryPath(file.path) else onOpenFile(file)
                                },
                                onFileLongClick = { },
                                onMenuClick = { file -> activeContextFile = file }
                            )
                        }
                    }
                } else {
                    FileGridOrList(
                        files = files,
                        viewMode = viewMode,
                        selectedFiles = selectedFiles,
                        isSelectionMode = isSelectionMode,
                        onFileClick = { file ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(file)
                            } else if (file.isDirectory) {
                                viewModel.navigateTo(file.path)
                            } else {
                                onOpenFile(file)
                            }
                        },
                        onFileLongClick = { file -> viewModel.toggleSelection(file) },
                        onMenuClick = { file -> activeContextFile = file }
                    )
                }
            }

            // Context Menu Dropdown for specific File Item
            if (activeContextFile != null) {
                val file = activeContextFile!!
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { activeContextFile = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open with...") },
                        onClick = {
                            activeContextFile = null
                            if (file.isDirectory) viewModel.navigateTo(file.path) else onOpenFile(file)
                        },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) }
                    )

                    val fileExt = file.extension.lowercase()
                    val fileMime = file.mimeType.lowercase()
                    val isImg = fileMime.startsWith("image/") || fileExt in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
                    val isAudVid = fileMime.startsWith("audio/") || fileMime.startsWith("video/") || fileExt in listOf("mp3", "wav", "flac", "m4a", "aac", "mp4", "mkv", "webm", "avi")
                    val isZipArchive = fileExt in listOf("zip", "rar", "7z", "tar", "gz", "apk")

                    if (isZipArchive) {
                        DropdownMenuItem(
                            text = { Text("Extract / Unzip") },
                            onClick = {
                                activeContextFile = null
                                fileToExtract = file
                            },
                            leadingIcon = { Icon(Icons.Default.Unarchive, null) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Compress to ZIP") },
                        onClick = {
                            viewModel.clearSelection()
                            viewModel.toggleSelection(file)
                            showCompressDialog = true
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.FolderZip, null) }
                    )

                    if (isImg) {
                        DropdownMenuItem(
                            text = { Text("View Full Screen") },
                            onClick = {
                                activeContextFile = null
                                fullScreenImageFile = file
                            },
                            leadingIcon = { Icon(Icons.Default.Fullscreen, null) }
                        )
                    } else if (isAudVid) {
                        DropdownMenuItem(
                            text = { Text("Preview in App") },
                            onClick = {
                                activeContextFile = null
                                mediaPreviewFile = file
                            },
                            leadingIcon = { Icon(Icons.Default.PlayCircle, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            fileToRename = file
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (file.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                        onClick = {
                            viewModel.toggleFavorite(file)
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.Star, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Secure Vault") },
                        onClick = {
                            fileToVault = file
                            showVaultPinDialog = true
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.Security, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Recycle Bin") },
                        onClick = {
                            scope.launch {
                                viewModel.repository.moveToRecycleBin(file.path)
                                viewModel.refreshCurrent()
                            }
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Properties") },
                        onClick = {
                            fileDetails = file
                            activeContextFile = null
                        },
                        leadingIcon = { Icon(Icons.Default.Info, null) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                viewModel.createFolder(name) { success, msg, createdDir ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success && createdDir != null) {
                        viewModel.navigateTo(createdDir.path)
                    }
                }
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateFileDialog) {
        val folderName = currentTab?.name ?: "Folder"
        CreateFileDialog(
            folderName = folderName,
            onConfirm = { fileName, content ->
                viewModel.createNewFile(fileName, content) { success, msg, _ ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                showCreateFileDialog = false
            },
            onDismiss = { showCreateFileDialog = false }
        )
    }

    if (fileToExtract != null) {
        ExtractDialog(
            zipFileItem = fileToExtract!!,
            previewEntriesProvider = { file ->
                viewModel.repository.archiveEngine.previewZipContents(file)
            },
            onExtractConfirm = { targetDir ->
                val targetZip = fileToExtract!!
                fileToExtract = null
                viewModel.extractZip(File(targetZip.path), targetDir)
                Toast.makeText(context, "Extracting archive in background...", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { fileToExtract = null }
        )
    }

    if (fileToRename != null) {
        RenameDialog(
            initialName = fileToRename!!.name,
            isDirectory = fileToRename!!.isDirectory,
            onRenameConfirm = { newName ->
                viewModel.renameFile(fileToRename!!.path, newName) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                fileToRename = null
            },
            onDismiss = { fileToRename = null }
        )
    }

    if (fileDetails != null) {
        DetailsDialog(
            fileItem = fileDetails!!,
            onDismiss = { fileDetails = null }
        )
    }

    if (showVaultPinDialog && fileToVault != null) {
        AlertDialog(
            onDismissRequest = {
                showVaultPinDialog = false
                fileToVault = null
                vaultPinInput = ""
            },
            title = { Text("Move to Secure Vault") },
            text = {
                Column {
                    Text("Enter your Vault PIN to encrypt and lock '${fileToVault!!.name}'.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = vaultPinInput,
                        onValueChange = { if (it.length <= 6) vaultPinInput = it },
                        label = { Text("Vault PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = fileToVault!!
                        val pin = vaultPinInput
                        showVaultPinDialog = false
                        fileToVault = null
                        vaultPinInput = ""
                        scope.launch {
                            val res = viewModel.repository.vaultEngine.lockFileIntoVault(File(target.path), pin)
                            res.fold(
                                onSuccess = {
                                    viewModel.refreshCurrent()
                                    Toast.makeText(context, "File encrypted & moved to Secure Vault", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, err.localizedMessage ?: "Failed to lock file", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Encrypt & Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVaultPinDialog = false
                    fileToVault = null
                    vaultPinInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCompressDialog) {
        CompressDialog(
            selectedCount = selectedFiles.size,
            onCompressConfirm = { zipName, level ->
                viewModel.compressSelected(zipName, level)
                Toast.makeText(context, "Compressing in background...", Toast.LENGTH_SHORT).show()
                showCompressDialog = false
            },
            onDismiss = { showCompressDialog = false }
        )
    }

    if (showProgressSheet) {
        OperationProgressSheet(
            tasks = activeTasks.values.toList(),
            onCancelTask = { taskId -> viewModel.repository.queueManager.cancelTask(taskId) },
            onDismiss = { showProgressSheet = false }
        )
    }

    if (mediaPreviewFile != null) {
        MediaPreviewDialog(
            fileItem = mediaPreviewFile!!,
            onDismiss = { mediaPreviewFile = null },
            onOpenExternal = {
                val target = mediaPreviewFile!!
                mediaPreviewFile = null
                onOpenFile(target)
            },
            onDeleteFile = { target ->
                mediaPreviewFile = null
                scope.launch {
                    viewModel.repository.moveToRecycleBin(target.path)
                    viewModel.refreshCurrent()
                }
            }
        )
    }

    if (fullScreenImageFile != null) {
        FullScreenImageViewerDialog(
            fileItem = fullScreenImageFile!!,
            onDismiss = { fullScreenImageFile = null },
            onDeleteFile = { target ->
                fullScreenImageFile = null
                scope.launch {
                    viewModel.repository.moveToRecycleBin(target.path)
                    viewModel.refreshCurrent()
                }
            }
        )
    }
}

@Composable
private fun BadgeBox(count: Int, content: @Composable () -> Unit) {
    Box {
        content()
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FileGridOrList(
    files: List<FileItem>,
    viewMode: ViewMode,
    selectedFiles: Set<FileItem>,
    isSelectionMode: Boolean,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onMenuClick: (FileItem) -> Unit
) {
    when (viewMode) {
        ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    FileGridItem(
                        fileItem = item,
                        isSelected = selectedFiles.contains(item),
                        isSelectionMode = isSelectionMode,
                        onClick = { onFileClick(item) },
                        onLongClick = { onFileLongClick(item) }
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    FileListItem(
                        fileItem = item,
                        isSelected = selectedFiles.contains(item),
                        isSelectionMode = isSelectionMode,
                        onClick = { onFileClick(item) },
                        onLongClick = { onFileLongClick(item) },
                        onMenuClick = { onMenuClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    onCreateFolderClick: () -> Unit,
    onCreateFileClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (query.isNotBlank()) "No files found matching '$query'" else "This folder is empty",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.isBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreateFolderClick) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Folder")
                    }
                    OutlinedButton(onClick = onCreateFileClick) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New File")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun shareSelectedFiles(context: Context, files: List<FileItem>) {
    if (files.isEmpty()) return
    try {
        val uris = files.map { fileItem ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(fileItem.path)
            )
        }

        val intent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
                type = files.first().mimeType
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                type = "*/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Top File Manager"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
