package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.VaultEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FileManagerRepository(application, UserPreferencesRepository(application))

    val vaultPinHash: StateFlow<String?> = repository.userPrefs.vaultPinHash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vaultFiles: StateFlow<List<VaultEntity>> = repository.vaultFilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setupPin(pin: String) {
        val hash = repository.vaultEngine.hashPin(pin)
        viewModelScope.launch {
            repository.userPrefs.setVaultPinHash(hash)
        }
    }

    fun verifyPin(pin: String): Boolean {
        val currentHash = vaultPinHash.value ?: return false
        val enteredHash = repository.vaultEngine.hashPin(pin)
        return currentHash == enteredHash
    }

    fun lockFileIntoVault(filePath: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val file = File(filePath)
            val res = repository.vaultEngine.lockFileIntoVault(file, pin)
            res.fold(
                onSuccess = { onResult(true, "File encrypted and moved to Secure Vault") },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Failed") }
            )
        }
    }

    fun restoreVaultFile(vaultEntity: VaultEntity, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.vaultEngine.unlockFileFromVault(vaultEntity, pin)
            res.fold(
                onSuccess = { onResult(true, "File decrypted and restored") },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Failed") }
            )
        }
    }

    fun deleteVaultFile(vaultEntity: VaultEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val encFile = File(vaultEntity.encryptedPath)
                if (encFile.exists()) encFile.delete()
                repository.vaultDao.deleteById(vaultEntity.id)
                onResult(true, "Item deleted from Vault")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed")
            }
        }
    }
}
