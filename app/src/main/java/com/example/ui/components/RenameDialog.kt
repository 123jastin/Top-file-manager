package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RenameDialog(
    initialName: String,
    isDirectory: Boolean,
    onRenameConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember {
        mutableStateOf(
            if (!isDirectory && initialName.contains(".")) initialName.substringBeforeLast(".") else initialName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("rename_dialog"),
        title = { Text(if (isDirectory) "Rename Folder" else "Rename File") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isDirectory && initialName.contains(".")) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extension (.${initialName.substringAfterLast(".")}) will be preserved automatically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRenameConfirm(newName.trim())
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
