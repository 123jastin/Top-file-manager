package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailsDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val file = File(fileItem.path)
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("details_dialog"),
        title = {
            Text(
                text = "Properties",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailItem("Name", fileItem.name)
                DetailItem("Type", if (fileItem.isDirectory) "Directory Folder" else "${fileItem.extension.uppercase()} File (${fileItem.mimeType})")
                DetailItem("Size", if (fileItem.isDirectory) "${formatFileSize(fileItem.size)} (approx)" else "${formatFileSize(fileItem.size)} (${fileItem.size} bytes)")
                if (fileItem.isDirectory) {
                    DetailItem("Contents", "${fileItem.childFileCount} files, ${fileItem.childFolderCount} subfolders")
                }
                DetailItem("Modified", dateFormat.format(Date(fileItem.lastModified)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(fileItem.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Path", fileItem.path))
                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Path")
                    }
                }

                DetailItem("Read/Write Permissions", "Readable: ${file.canRead()}, Writable: ${file.canWrite()}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
