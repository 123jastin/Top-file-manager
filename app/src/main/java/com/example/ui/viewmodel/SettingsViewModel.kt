package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccentColor
import com.example.data.model.CompressionLevel
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.monetization.EntitlementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application)
    val entitlementManager = EntitlementManager(prefs)

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val accentColor: StateFlow<AccentColor> = prefs.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.ELECTRIC_BLUE)

    val showHiddenFiles: StateFlow<Boolean> = prefs.showHiddenFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val confirmDelete: StateFlow<Boolean> = prefs.confirmDelete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticFeedback: StateFlow<Boolean> = prefs.hapticFeedback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationEnabled: StateFlow<Boolean> = prefs.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val compressionLevel: StateFlow<CompressionLevel> = prefs.compressionLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompressionLevel.NORMAL)

    val isProUser: StateFlow<Boolean> = entitlementManager.isProUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { prefs.setAccentColor(color) }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch { prefs.setShowHiddenFiles(show) }
    }

    fun setConfirmDelete(confirm: Boolean) {
        viewModelScope.launch { prefs.setConfirmDelete(confirm) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { prefs.setHapticFeedback(enabled) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationEnabled(enabled) }
    }

    fun setCompressionLevel(level: CompressionLevel) {
        viewModelScope.launch { prefs.setCompressionLevel(level) }
    }

    fun toggleProUser() {
        viewModelScope.launch {
            if (isProUser.value) {
                entitlementManager.resetProStatus()
            } else {
                entitlementManager.upgradeToPro()
            }
        }
    }
}
