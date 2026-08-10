package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CompressionLevel

@Composable
fun CompressDialog(
    selectedCount: Int,
    onCompressConfirm: (zipName: String, level: CompressionLevel) -> Unit,
    onDismiss: () -> Unit
) {
    var zipName by remember { mutableStateOf("Archive_${System.currentTimeMillis() / 1000}") }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("compress_dialog"),
        title = { Text("Compress $selectedCount File(s)") },
        text = {
            Column {
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Compression Level:", style = MaterialTheme.typography.labelMedium)

                CompressionLevel.entries.forEach { level ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedLevel == level),
                            onClick = { selectedLevel = level }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = level.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (zipName.isNotBlank()) {
                        onCompressConfirm(zipName.trim(), selectedLevel)
                    }
                }
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
