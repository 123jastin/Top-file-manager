package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FileBrowserViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewScreen(
    categoryType: CategoryType,
    viewModel: FileBrowserViewModel,
    onBackClick: () -> Unit,
    onOpenFile: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var categoryFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sortOption by remember { mutableStateOf(FileSortOption()) }
    var showSortMenu by remember { mutableStateOf(false) }

    var fullScreenImageFile by remember { mutableStateOf<FileItem?>(null) }
    var activeContextFile by remember { mutableStateOf<FileItem?>(null) }
    var fileDetails by remember { mutableStateOf<FileItem?>(null) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileToExtract by remember { mutableStateOf<FileItem?>(null) }
    var compressTargetFile by remember { mutableStateOf<FileItem?>(null) }

    val categoryTitle = when (categoryType) {
        CategoryType.IMAGES -> "Images"
        CategoryType.VIDEOS -> "Videos"
        CategoryType.AUDIO -> "Audio Files"
        CategoryType.DOCUMENTS -> "Documents"
        CategoryType.DOWNLOADS -> "Downloads"
        CategoryType.ARCHIVES -> "Archives"
        CategoryType.APKS -> "APKs"
        CategoryType.RECENT -> "Recent Files"
        CategoryType.VAULT -> "Vault"
        CategoryType.ALL -> "All Files"
    }

    // Fast loading logic with instant cache
    LaunchedEffect(categoryType) {
        val cached = viewModel.repository.fileEngine.getCachedFilesByCategory(categoryType, sortOption)
        if (!cached.isNullOrEmpty()) {
            categoryFiles = cached
            isLoading = false
        } else {
            isLoading = true
        }

        // Fetch / Refresh in background
        scope.launch {
            val freshFiles = viewModel.repository.fileEngine.getFilesByCategory(categoryType, false, sortOption)
            categoryFiles = freshFiles
            isLoading = false
        }
    }

    val displayFiles = remember(categoryFiles, searchQuery, sortOption) {
        val list = if (searchQuery.isBlank()) categoryFiles else {
            categoryFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        viewModel.repository.fileEngine.sortFileItems(list, sortOption)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isLoading && displayFiles.isEmpty()) "Loading..." else "${displayFiles.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                actions = {
                    // View Mode Toggle
                    IconButton(onClick = {
                        viewMode = when (viewMode) {
                            ViewMode.GRID -> ViewMode.LIST
                            ViewMode.LIST -> ViewMode.GRID
                            ViewMode.COMPACT -> ViewMode.GRID
                        }
                    }) {
                        val icon = when (viewMode) {
                            ViewMode.GRID -> Icons.Default.ViewList
                            ViewMode.LIST -> Icons.Default.GridView
                            ViewMode.COMPACT -> Icons.Default.GridView
                        }
                        Icon(imageVector = icon, contentDescription = "Toggle View")
                    }

                    // Sort Menu
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = {
                                sortOption = FileSortOption(SortField.NAME, ascending = true)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Size") },
                            onClick = {
                                sortOption = FileSortOption(SortField.SIZE, ascending = false)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = {
                                sortOption = FileSortOption(SortField.DATE, ascending = false)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Type") },
                            onClick = {
                                sortOption = FileSortOption(SortField.TYPE, ascending = true)
                                showSortMenu = false
                            }
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            val reloaded = viewModel.repository.fileEngine.getFilesByCategory(categoryType, false, sortOption, forceRefresh = true)
                            categoryFiles = reloaded
                            isLoading = false
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search $categoryTitle...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            if (isLoading && displayFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scanning $categoryTitle...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (displayFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = when (categoryType) {
                                CategoryType.IMAGES -> Icons.Default.Image
                                CategoryType.VIDEOS -> Icons.Default.VideoFile
                                CategoryType.AUDIO -> Icons.Default.AudioFile
                                CategoryType.DOCUMENTS -> Icons.Default.Description
                                CategoryType.DOWNLOADS -> Icons.Default.Download
                                CategoryType.ARCHIVES -> Icons.Default.FolderZip
                                else -> Icons.Default.FolderOpen
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching files" else "No $categoryTitle found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Files saved on your device will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (viewMode) {
                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayFiles, key = { it.path }) { file ->
                                FileGridItem(
                                    fileItem = file,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = {
                                        val ext = file.extension.lowercase()
                                        val mime = file.mimeType.lowercase()
                                        val isImg = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
                                        val isZip = ext in listOf("zip", "rar", "7z", "tar", "gz", "apk")
                                        if (isImg) {
                                            fullScreenImageFile = file
                                        } else if (isZip) {
                                            fileToExtract = file
                                        } else {
                                            onOpenFile(file)
                                        }
                                    },
                                    onLongClick = { activeContextFile = file }
                                )
                            }
                        }
                    }
                    ViewMode.LIST, ViewMode.COMPACT -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayFiles, key = { it.path }) { file ->
                                FileListItem(
                                    fileItem = file,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = {
                                        val ext = file.extension.lowercase()
                                        val mime = file.mimeType.lowercase()
                                        val isImg = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
                                        val isZip = ext in listOf("zip", "rar", "7z", "tar", "gz", "apk")
                                        if (isImg) {
                                            fullScreenImageFile = file
                                        } else if (isZip) {
                                            fileToExtract = file
                                        } else {
                                            onOpenFile(file)
                                        }
                                    },
                                    onLongClick = { activeContextFile = file },
                                    onMenuClick = { activeContextFile = file }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Context Menu Dialog
    if (activeContextFile != null) {
        val file = activeContextFile!!
        AlertDialog(
            onDismissRequest = { activeContextFile = null },
            title = { Text(text = file.name, maxLines = 1) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Open / Open with...") },
                        leadingContent = { Icon(Icons.Default.OpenInNew, null) },
                        modifier = Modifier.clickable {
                            activeContextFile = null
                            onOpenFile(file)
                        }
                    )
                    val ext = file.extension.lowercase()
                    val mime = file.mimeType.lowercase()
                    val isImg = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
                    val isZip = ext in listOf("zip", "rar", "7z", "tar", "gz", "apk")

                    if (isZip) {
                        ListItem(
                            headlineContent = { Text("Extract / Unzip") },
                            leadingContent = { Icon(Icons.Default.Unarchive, null) },
                            modifier = Modifier.clickable {
                                activeContextFile = null
                                fileToExtract = file
                            }
                        )
                    }

                    ListItem(
                        headlineContent = { Text("Compress to ZIP") },
                        leadingContent = { Icon(Icons.Default.FolderZip, null) },
                        modifier = Modifier.clickable {
                            activeContextFile = null
                            compressTargetFile = file
                        }
                    )
                    if (isImg) {
                        ListItem(
                            headlineContent = { Text("View Full Screen") },
                            leadingContent = { Icon(Icons.Default.Fullscreen, null) },
                            modifier = Modifier.clickable {
                                activeContextFile = null
                                fullScreenImageFile = file
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Share") },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        modifier = Modifier.clickable {
                            activeContextFile = null
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(file.path)
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = file.mimeType.ifEmpty { "*/*" }
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share file"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Rename") },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable {
                            fileToRename = file
                            activeContextFile = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete") },
                        leadingContent = { Icon(Icons.Default.Delete, null) },
                        modifier = Modifier.clickable {
                            val target = file
                            activeContextFile = null
                            scope.launch {
                                viewModel.repository.moveToRecycleBin(target.path)
                                categoryFiles = categoryFiles.filter { it.path != target.path }
                                Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Details") },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        modifier = Modifier.clickable {
                            fileDetails = file
                            activeContextFile = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeContextFile = null }) {
                    Text("Close")
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
                    categoryFiles = categoryFiles.filter { it.path != target.path }
                }
            }
        )
    }

    if (fileDetails != null) {
        DetailsDialog(
            fileItem = fileDetails!!,
            onDismiss = { fileDetails = null }
        )
    }

    if (fileToRename != null) {
        val file = fileToRename!!
        RenameDialog(
            initialName = file.name,
            isDirectory = file.isDirectory,
            onRenameConfirm = { newName ->
                scope.launch {
                    val result = viewModel.repository.renameFile(file.path, newName)
                    if (result.isSuccess) {
                        val freshFiles = viewModel.repository.fileEngine.getFilesByCategory(categoryType, false, sortOption, forceRefresh = true)
                        categoryFiles = freshFiles
                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                fileToRename = null
            },
            onDismiss = { fileToRename = null }
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

    if (compressTargetFile != null) {
        val target = compressTargetFile!!
        CompressDialog(
            selectedCount = 1,
            onCompressConfirm = { zipName, level ->
                val parentDir = File(target.path).parentFile ?: File(target.path)
                val outputZip = File(parentDir, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
                viewModel.repository.queueManager.startCompressOperation(listOf(File(target.path)), outputZip, level) {
                    scope.launch {
                        val freshFiles = viewModel.repository.fileEngine.getFilesByCategory(categoryType, false, sortOption, forceRefresh = true)
                        categoryFiles = freshFiles
                    }
                }
                Toast.makeText(context, "Compressing in background...", Toast.LENGTH_SHORT).show()
                compressTargetFile = null
            },
            onDismiss = { compressTargetFile = null }
        )
    }
}
