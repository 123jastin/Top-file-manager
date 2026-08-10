package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, iconColor) = getFileIconAndColor(fileItem)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .testTag("file_grid_${fileItem.name}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FileThumbnail(
                    fileItem = fileItem,
                    icon = icon,
                    iconColor = iconColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    iconSize = 34.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (fileItem.isDirectory) "${fileItem.childFileCount} items" else formatFileSize(fileItem.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }

            if (fileItem.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
