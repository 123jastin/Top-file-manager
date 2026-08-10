package com.example.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaPreviewDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
    onDeleteFile: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val ext = remember(fileItem.extension) { fileItem.extension.lowercase() }
    val mime = remember(fileItem.mimeType) { fileItem.mimeType.lowercase() }

    val isImage = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    val isVideo = mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "mov", "avi")
    val isAudio = mime.startsWith("audio/") || ext in listOf("mp3", "wav", "flac", "m4a", "aac", "ogg")

    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(fileItem.path) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(fileItem.lastModified))

    AlertDialog(
        onDismissRequest = {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isImage -> "Image Preview"
                            isVideo -> "Video Preview"
                            isAudio -> "Audio Player & Preview"
                            else -> "Media File"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Preview Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isImage || isVideo) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(fileItem.path))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = fileItem.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isVideo) Icons.Default.VideoFile else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            )

                            if (isVideo) {
                                Surface(
                                    onClick = {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        onOpenExternal()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Video", color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        } else if (isAudio) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            try {
                                                if (isPlayingAudio) {
                                                    mediaPlayer?.pause()
                                                    isPlayingAudio = false
                                                } else {
                                                    if (mediaPlayer == null) {
                                                        mediaPlayer = MediaPlayer().apply {
                                                            setDataSource(fileItem.path)
                                                            prepare()
                                                            setOnCompletionListener {
                                                                isPlayingAudio = false
                                                            }
                                                        }
                                                    }
                                                    mediaPlayer?.start()
                                                    isPlayingAudio = true
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot play audio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingAudio) "Pause" else "Play",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isPlayingAudio) "Playing Audio Preview..." else "Tap to Play Audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // File Details Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Size: ${formatFileSize(fileItem.size)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Modified: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Path: ${fileItem.path}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    onOpenExternal()
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open App")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            val file = File(fileItem.path)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = fileItem.mimeType.ifEmpty { "*/*" }
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share File"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                }
                TextButton(
                    onClick = {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onDeleteFile(fileItem)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    )
}
