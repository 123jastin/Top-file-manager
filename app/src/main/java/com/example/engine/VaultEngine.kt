package com.example.engine

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.VaultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultEngine(private val context: Context, private val fileEngine: FileEngine) {

    private val db = AppDatabase.getDatabase(context)
    private val vaultDao = db.vaultDao()

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    suspend fun lockFileIntoVault(file: File, pin: String): Result<VaultEntity> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) throw IllegalArgumentException("Source file does not exist")

            val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(pin, salt)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            val encryptedFile = File(fileEngine.vaultFolder, "${System.currentTimeMillis()}_${file.name}.enc")

            FileOutputStream(encryptedFile).use { fos ->
                fos.write(salt)
                fos.write(iv)

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) fos.write(output)
                    }
                    val finalOutput = cipher.doFinal()
                    if (finalOutput != null) fos.write(finalOutput)
                }
            }

            val mimeType = fileEngine.getMimeType(file)
            val vaultEntity = VaultEntity(
                originalPath = file.absolutePath,
                encryptedPath = encryptedFile.absolutePath,
                fileName = file.name,
                mimeType = mimeType,
                fileSize = file.length(),
                addedTimestamp = System.currentTimeMillis()
            )

            val id = vaultDao.insertVaultFile(vaultEntity)
            file.deleteRecursively()

            vaultEntity.copy(id = id)
        }
    }

    suspend fun unlockFileFromVault(vaultEntity: VaultEntity, pin: String, restoreDir: File? = null): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val encFile = File(vaultEntity.encryptedPath)
            if (!encFile.exists()) throw IllegalStateException("Encrypted file missing")

            val targetParent = restoreDir ?: File(vaultEntity.originalPath).parentFile ?: EnvironmentExternal()
            targetParent.mkdirs()
            val restoredFile = File(targetParent, vaultEntity.fileName)

            FileInputStream(encFile).use { fis ->
                val salt = ByteArray(16)
                if (fis.read(salt) != 16) throw IllegalStateException("Invalid encrypted file header")
                val iv = ByteArray(16)
                if (fis.read(iv) != 16) throw IllegalStateException("Invalid encrypted file IV")

                val secretKey = deriveKey(pin, salt)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                FileOutputStream(restoredFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) fos.write(output)
                    }
                    val finalOutput = cipher.doFinal()
                    if (finalOutput != null) fos.write(finalOutput)
                }
            }

            vaultDao.deleteById(vaultEntity.id)
            encFile.delete()

            restoredFile
        }
    }

    private fun EnvironmentExternal(): File {
        return android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    }
}
