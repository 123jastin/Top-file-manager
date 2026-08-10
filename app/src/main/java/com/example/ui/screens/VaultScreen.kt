package com.example.ui.screens

import android.os.Environment
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.db.VaultEntity
import com.example.ui.components.formatFileSize
import com.example.ui.viewmodel.VaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vaultPinHash by viewModel.vaultPinHash.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }

    // Reset lock state on disposal
    DisposableEffect(Unit) {
        onDispose {
            isUnlocked = false
            enteredPin = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault (AES-256)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { showFilePicker = true }, modifier = Modifier.testTag("vault_add_file_icon")) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add File to Vault")
                        }
                        IconButton(onClick = {
                            isUnlocked = false
                            enteredPin = ""
                            Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.testTag("vault_lock_btn")) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Vault")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (isUnlocked) {
                ExtendedFloatingActionButton(
                    text = { Text("Add File to Vault") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { showFilePicker = true },
                    modifier = Modifier.testTag("vault_fab_add_file")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (vaultPinHash == null) {
                // Initial PIN setup screen
                PinSetupCard(onPinConfirm = { pin ->
                    viewModel.setupPin(pin)
                    enteredPin = pin
                    isUnlocked = true
                    Toast.makeText(context, "Vault PIN created successfully", Toast.LENGTH_SHORT).show()
                })
            } else if (!isUnlocked) {
                // PIN Verification Screen
                PinUnlockCard(
                    enteredPin = enteredPin,
                    onPinChange = { enteredPin = it },
                    onUnlock = {
                        if (viewModel.verifyPin(enteredPin)) {
                            isUnlocked = true
                        } else {
                            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                // Unlocked Vault View
                if (vaultFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your Secure Vault is Empty", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Encrypted files stored here cannot be accessed anywhere else.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showFilePicker = true },
                                modifier = Modifier.testTag("vault_empty_add_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add File to Vault")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vaultFiles, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.fileName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(formatFileSize(item.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        viewModel.restoreVaultFile(item, enteredPin) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Restore File to Storage")
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteVaultFile(item) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showFilePicker) {
                VaultFilePickerDialog(
                    onFileSelected = { filePath ->
                        showFilePicker = false
                        viewModel.lockFileIntoVault(filePath, enteredPin) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showFilePicker = false }
                )
            }
        }
    }
}

@Composable
private fun VaultFilePickerDialog(
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPath by remember {
        mutableStateOf(Environment.getExternalStorageDirectory()?.absolutePath ?: "")
    }

    val dir = File(currentPath)
    val files = remember(currentPath) {
        dir.listFiles()?.filter { !it.name.startsWith(".") }?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select File to Encrypt", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = dir.name.ifEmpty { "Storage" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                if (dir.parentFile != null && dir.absolutePath != Environment.getExternalStorageDirectory()?.absolutePath) {
                    TextButton(onClick = { currentPath = dir.parentFile!!.absolutePath }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(".. (Up One Level)")
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files, key = { it.absolutePath }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (file.isDirectory) {
                                        currentPath = file.absolutePath
                                    } else {
                                        onFileSelected(file.absolutePath)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PinSetupCard(onPinConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Create Secure Vault PIN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            label = { Text("6-Digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("vault_pin_setup_input")
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { if (pin.length >= 4) onPinConfirm(pin) },
            modifier = Modifier.fillMaxWidth().testTag("vault_pin_setup_btn")
        ) {
            Text("Set PIN & Unlock")
        }
    }
}

@Composable
private fun PinUnlockCard(
    enteredPin: String,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter Vault PIN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = enteredPin,
            onValueChange = { if (it.length <= 6) onPinChange(it) },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("vault_pin_unlock_input")
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().testTag("vault_pin_unlock_btn")
        ) {
            Text("Unlock Vault")
        }
    }
}
