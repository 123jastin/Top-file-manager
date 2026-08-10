package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileItem

@Composable
fun SelectionActionBar(
    selectedFiles: Set<FileItem>,
    totalFilesCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopyClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCompressClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val totalSize = selectedFiles.sumOf { if (it.isDirectory) 0L else it.size }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selection_action_bar"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClearSelection) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close selection")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${selectedFiles.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalSize > 0) {
                            Text(
                                text = "Total: ${formatFileSize(totalSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                TextButton(onClick = onSelectAll) {
                    Text(
                        text = if (selectedFiles.size == totalFilesCount) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = onCopyClick
                )
                ActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Move",
                    onClick = onMoveClick
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = onDeleteClick
                )
                ActionButton(
                    icon = Icons.Default.FolderZip,
                    label = "Compress",
                    onClick = onCompressClick
                )
                ActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = onShareClick
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag("action_$label")
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
