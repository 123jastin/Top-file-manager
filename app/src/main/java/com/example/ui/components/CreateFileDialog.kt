package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreateFileDialog(
    folderName: String,
    onConfirm: (fileName: String, initialContent: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("NewDocument.txt") }
    var fileContent by remember { mutableStateOf("") }
    var selectedExtension by remember { mutableStateOf(".txt") }

    val extensionOptions = listOf(".txt", ".md", ".json", ".csv", ".xml", ".html")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("create_file_dialog"),
        title = {
            Text(
                text = "Add File in '$folderName'",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("File Type:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    extensionOptions.forEach { ext ->
                        FilterChip(
                            selected = fileName.endsWith(ext, ignoreCase = true) || selectedExtension == ext,
                            onClick = {
                                selectedExtension = ext
                                val base = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
                                fileName = "$base$ext"
                            },
                            label = { Text(ext) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { fileContent = it },
                    label = { Text("Initial Text Content (Optional)") },
                    placeholder = { Text("Write content or leave blank...") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onConfirm(fileName.trim(), fileContent)
                    }
                }
            ) {
                Text("Create File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
