package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileItem
import java.io.File

@Composable
fun FileThumbnail(
    fileItem: FileItem,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    val context = LocalContext.current
    val ext = remember(fileItem.extension) { fileItem.extension.lowercase() }
    val mime = remember(fileItem.mimeType) { fileItem.mimeType.lowercase() }

    val isImage = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    val isVideo = mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "mov", "avi")
    val isAudio = mime.startsWith("audio/") || ext in listOf("mp3", "wav", "flac", "m4a", "aac", "ogg")

    val isMedia = (isImage || isVideo || isAudio) && !fileItem.isDirectory

    if (isMedia) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(fileItem.path))
                    .crossfade(true)
                    .build(),
                contentDescription = fileItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            )

            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (isAudio) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier.background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = fileItem.name,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
