package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentColor
import com.example.data.model.CompressionLevel
import com.example.data.model.ThemeMode
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val confirmDelete by viewModel.confirmDelete.collectAsState()
    val isProUser by viewModel.isProUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Themes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top File Manager",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Full Version • All Features Unlocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Theme Mode
            Text("Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Accent Color Palette", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AccentColor.entries.forEach { color ->
                            val colorValue = Color(color.colorHex)
                            val isSelected = accentColor == color
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setAccentColor(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Behavioral Settings
            Text("General Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Hidden Files & Folders", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = showHiddenFiles,
                            onCheckedChange = { viewModel.setShowHiddenFiles(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confirm Before Delete", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = confirmDelete,
                            onCheckedChange = { viewModel.setConfirmDelete(it) }
                        )
                    }
                }
            }
        }
    }
}
