package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaEngine(private val context: Context) {

    suspend fun rotateImage(inputFile: File, degrees: Float, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
                ?: throw IllegalArgumentException("Failed to decode image file")

            val matrix = Matrix().apply { postRotate(degrees) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val format = when (outputFile.extension.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            FileOutputStream(outputFile).use { fos ->
                rotatedBitmap.compress(format, 90, fos)
            }

            if (rotatedBitmap != bitmap) bitmap.recycle()
            rotatedBitmap.recycle()

            outputFile
        }
    }

    suspend fun convertImageFormat(
        inputFile: File,
        targetFormat: String, // "jpg", "png", "webp"
        quality: Int = 90,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
                ?: throw IllegalArgumentException("Failed to decode image file")

            val compressFormat = when (targetFormat.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(compressFormat, quality, fos)
            }
            bitmap.recycle()

            outputFile
        }
    }

    fun getVideoMetadata(videoFile: File): VideoMetadata? {
        if (!videoFile.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            VideoMetadata(
                durationMs = durationMs,
                width = width,
                height = height,
                rotation = rotation
            )
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun getAudioMetadata(audioFile: File): AudioMetadata? {
        if (!audioFile.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioFile.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: audioFile.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int
)

data class AudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)
