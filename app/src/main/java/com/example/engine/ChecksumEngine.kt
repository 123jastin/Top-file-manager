package com.example.engine

import com.example.data.model.ChecksumResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ChecksumEngine {

    suspend fun calculateChecksums(
        file: File,
        onProgress: (progressRatio: Float) -> Unit = {}
    ): Result<ChecksumResult> = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.currentTimeMillis()
            val md5Digest = MessageDigest.getInstance("MD5")
            val sha1Digest = MessageDigest.getInstance("SHA-1")
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            val sha512Digest = MessageDigest.getInstance("SHA-512")

            val totalSize = file.length()
            var readBytes = 0L

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md5Digest.update(buffer, 0, bytesRead)
                    sha1Digest.update(buffer, 0, bytesRead)
                    sha256Digest.update(buffer, 0, bytesRead)
                    sha512Digest.update(buffer, 0, bytesRead)

                    readBytes += bytesRead
                    if (totalSize > 0) {
                        onProgress(readBytes.toFloat() / totalSize.toFloat())
                    }
                }
            }

            val endTime = System.currentTimeMillis()

            ChecksumResult(
                filePath = file.absolutePath,
                md5 = bytesToHex(md5Digest.digest()),
                sha1 = bytesToHex(sha1Digest.digest()),
                sha256 = bytesToHex(sha256Digest.digest()),
                sha512 = bytesToHex(sha512Digest.digest()),
                durationMs = endTime - startTime
            )
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
