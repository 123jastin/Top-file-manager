package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FileConflictOption

@Composable
fun ConflictDialog(
    fileName: String,
    onOptionSelected: (FileConflictOption, applyToAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var applyToAll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("conflict_dialog"),
        title = { Text("File Already Exists") },
        text = {
            Column {
                Text("A file named '$fileName' already exists in the destination.")
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply to all conflicts")
                }
            }
        },
        confirmButton = {
            Column {
                TextButton(onClick = { onOptionSelected(FileConflictOption.RENAME_AUTO, applyToAll) }) {
                    Text("Auto Rename")
                }
                TextButton(onClick = { onOptionSelected(FileConflictOption.REPLACE, applyToAll) }) {
                    Text("Replace")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onOptionSelected(FileConflictOption.SKIP, applyToAll) }) {
                Text("Skip")
            }
        }
    )
}
