package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryType
import com.example.data.model.StorageLocation
import com.example.ui.components.AdBanner
import com.example.ui.components.StorageCard
import com.example.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStorage: (String) -> Unit,
    onNavigateToCategory: (CategoryType) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onSearchClick: () -> Unit,
    isPro: Boolean
) {
    val favorites by viewModel.favorites.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val storageLocations by viewModel.storageLocations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Top File Manager",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Storage Overview & Quick Actions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick, modifier = Modifier.testTag("home_search")) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateToRecycleBin, modifier = Modifier.testTag("home_recycle_bin")) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Recycle Bin")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!isPro) {
                item {
                    AdBanner()
                }
            }

            // Storage Locations Section
            item {
                SectionHeader(title = "Storage Locations")
                Spacer(modifier = Modifier.height(8.dp))
                storageLocations.forEach { storage ->
                    StorageCard(
                        storageLocation = storage,
                        onClick = { onNavigateToStorage(storage.path) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Quick Actions Section
            item {
                SectionHeader(title = "Quick Actions")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        color = Color(0xFF3B82F6),
                        onClick = onSearchClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.Analytics,
                        label = "Scan Storage",
                        color = Color(0xFF10B981),
                        onClick = onNavigateToTools
                    )
                    QuickActionButton(
                        icon = Icons.Default.Security,
                        label = "Vault",
                        color = Color(0xFF8B5CF6),
                        onClick = onNavigateToVault
                    )
                    QuickActionButton(
                        icon = Icons.Default.Delete,
                        label = "Recycle Bin",
                        color = Color(0xFFEF4444),
                        onClick = onNavigateToRecycleBin
                    )
                }
            }

            // Categories Grid Section
            item {
                SectionHeader(title = "Categories")
                Spacer(modifier = Modifier.height(10.dp))

                val categories = listOf(
                    CategoryItem("Images", Icons.Default.Image, Color(0xFF10B981), CategoryType.IMAGES),
                    CategoryItem("Videos", Icons.Default.VideoFile, Color(0xFF8B5CF6), CategoryType.VIDEOS),
                    CategoryItem("Audio", Icons.Default.AudioFile, Color(0xFFEC4899), CategoryType.AUDIO),
                    CategoryItem("Documents", Icons.Default.Description, Color(0xFFF59E0B), CategoryType.DOCUMENTS),
                    CategoryItem("Downloads", Icons.Default.Download, Color(0xFF3B82F6), CategoryType.DOWNLOADS),
                    CategoryItem("Archives", Icons.Default.FolderZip, Color(0xFF06B6D4), CategoryType.ARCHIVES),
                    CategoryItem("APKs", Icons.Default.Android, Color(0xFF84CC16), CategoryType.APKS),
                    CategoryItem("Recent", Icons.Default.Schedule, Color(0xFF64748B), CategoryType.RECENT)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in categories.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CategoryCard(
                                category = categories[i],
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(categories[i].type) }
                            )
                            if (i + 1 < categories.size) {
                                CategoryCard(
                                    category = categories[i + 1],
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToCategory(categories[i + 1].type) }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Bookmarked Pinned Folders
            if (bookmarks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Pinned Folders")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarks, key = { it.path }) { bookmark ->
                            Card(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { onNavigateToStorage(bookmark.path) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bookmark.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Favorites Section
            if (favorites.isNotEmpty()) {
                item {
                    SectionHeader(title = "Favorites")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        favorites.take(5).forEach { fav ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToStorage(fav.path) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (fav.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = fav.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

private data class CategoryItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val type: CategoryType
)

@Composable
private fun CategoryCard(
    category: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("category_${category.title}")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(category.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = category.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
