package com.example.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DuplicateGroup
import com.example.data.model.FileItem
import com.example.ui.components.formatFileSize
import com.example.ui.viewmodel.ToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Analyzer, 1: Duplicates, 2: Checksum, 3: Apps

    val breakdown by viewModel.breakdown.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val checksumResult by viewModel.checksumResult.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var checksumInputPath by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.runStorageAnalyzer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Tools & Utilities", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = selectedTab == 0, onClick = {
                    selectedTab = 0
                    viewModel.runStorageAnalyzer()
                }, text = { Text("Analyzer") })
                Tab(selected = selectedTab == 1, onClick = {
                    selectedTab = 1
                    viewModel.scanDuplicates()
                }, text = { Text("Duplicates") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Checksum") })
                Tab(selected = selectedTab == 3, onClick = {
                    selectedTab = 3
                    viewModel.loadInstalledApps()
                }, text = { Text("App Manager") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scanning storage...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> StorageAnalyzerView(breakdown = breakdown, largeFiles = largeFiles, onDelete = { path ->
                        viewModel.deleteFile(path) {
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            viewModel.runStorageAnalyzer()
                        }
                    })
                    1 -> DuplicateFinderView(duplicateGroups = duplicateGroups, onDelete = { path ->
                        viewModel.deleteFile(path) {
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            viewModel.scanDuplicates()
                        }
                    })
                    2 -> ChecksumView(
                        inputPath = checksumInputPath,
                        onInputPathChange = { checksumInputPath = it },
                        checksumResult = checksumResult,
                        onCalculate = { viewModel.calculateChecksum(checksumInputPath) }
                    )
                    3 -> AppManagerView(apps = installedApps)
                }
            }
        }
    }
}

@Composable
private fun StorageAnalyzerView(
    breakdown: List<com.example.data.model.StorageBreakdown>,
    largeFiles: List<FileItem>,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Storage Breakdown by Type", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(item.colorHex))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            Text("${item.fileCount} items • ${formatFileSize(item.bytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Text("Large Files (>50 MB)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(largeFiles, key = { it.path }) { file ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatFileSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(file.path) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateFinderView(
    duplicateGroups: List<DuplicateGroup>,
    onDelete: (String) -> Unit
) {
    if (duplicateGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No duplicate files found", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(duplicateGroups, key = { it.checksum }) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Duplicate Group (${group.files.size} copies, ${formatFileSize(group.size)} each)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        group.files.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { onDelete(file.path) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete copy", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecksumView(
    inputPath: String,
    onInputPathChange: (String) -> Unit,
    checksumResult: com.example.data.model.ChecksumResult?,
    onCalculate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = inputPath,
            onValueChange = onInputPathChange,
            label = { Text("File Absolute Path") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checksum_path_input"),
            placeholder = { Text("/storage/emulated/0/Download/file.pdf") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCalculate,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checksum_calc_btn")
        ) {
            Text("Calculate MD5, SHA-1, SHA-256, SHA-512")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (checksumResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Checksum Results (${checksumResult.durationMs} ms)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("MD5:\n${checksumResult.md5}", style = MaterialTheme.typography.bodySmall)
                    Text("SHA-1:\n${checksumResult.sha1}", style = MaterialTheme.typography.bodySmall)
                    Text("SHA-256:\n${checksumResult.sha256}", style = MaterialTheme.typography.bodySmall)
                    Text("SHA-512:\n${checksumResult.sha512}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AppManagerView(apps: List<com.example.engine.InstalledAppItem>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Android, contentDescription = null, tint = Color(0xFF84CC16), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${app.packageName} • v${app.versionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatFileSize(app.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
