package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.ZipEntryPreview
import com.example.data.model.FileItem
import java.io.File

@Composable
fun ExtractDialog(
    zipFileItem: FileItem,
    previewEntriesProvider: suspend (File) -> List<ZipEntryPreview>,
    onExtractConfirm: (targetDir: File) -> Unit,
    onDismiss: () -> Unit
) {
    val zipFile = File(zipFileItem.path)
    val defaultFolderName = zipFile.nameWithoutExtension.ifBlank { "Extracted" }
    val parentDir = zipFile.parentFile ?: File(zipFileItem.path)

    var createSubfolder by remember { mutableStateOf(true) }
    var folderNameInput by remember { mutableStateOf(defaultFolderName) }
    var entries by remember { mutableStateOf<List<ZipEntryPreview>>(emptyList()) }
    var isLoadingPreview by remember { mutableStateOf(true) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Extract Options, 1 = Preview Contents

    LaunchedEffect(zipFileItem) {
        isLoadingPreview = true
        entries = previewEntriesProvider(zipFile)
        isLoadingPreview = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("extract_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.FolderZip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column {
                Text(
                    text = "Extract ZIP Archive",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = zipFileItem.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Options") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Contents (${if (isLoadingPreview) "..." else entries.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == 0) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = createSubfolder,
                                onCheckedChange = { createSubfolder = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract into a new subfolder")
                        }

                        if (createSubfolder) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = folderNameInput,
                                onValueChange = { folderNameInput = it },
                                label = { Text("Subfolder Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Destination:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val destPath = if (createSubfolder && folderNameInput.isNotBlank()) {
                                    File(parentDir, folderNameInput.trim()).absolutePath
                                } else {
                                    parentDir.absolutePath
                                }
                                Text(
                                    text = destPath,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    if (isLoadingPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (entries.isEmpty()) {
                        Text(
                            text = "Archive is empty or could not be read.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            items(entries) { entry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetDir = if (createSubfolder && folderNameInput.isNotBlank()) {
                        File(parentDir, folderNameInput.trim())
                    } else {
                        parentDir
                    }
                    onExtractConfirm(targetDir)
                }
            ) {
                Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Extract Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
