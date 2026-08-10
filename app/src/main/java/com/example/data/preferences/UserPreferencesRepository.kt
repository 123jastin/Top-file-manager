package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val COMPRESSION_LEVEL = stringPreferencesKey("compression_level")
        val ADS_ENABLED = booleanPreferencesKey("ads_enabled")
        val PRO_ENTITLEMENT = booleanPreferencesKey("pro_entitlement")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val value = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        runCatching { ThemeMode.valueOf(value) }.getOrDefault(ThemeMode.DARK)
    }

    val accentColor: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        val value = prefs[Keys.ACCENT_COLOR] ?: AccentColor.ELECTRIC_BLUE.name
        runCatching { AccentColor.valueOf(value) }.getOrDefault(AccentColor.ELECTRIC_BLUE)
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { prefs ->
        val value = prefs[Keys.VIEW_MODE] ?: ViewMode.LIST.name
        runCatching { ViewMode.valueOf(value) }.getOrDefault(ViewMode.LIST)
    }

    val sortOption: Flow<FileSortOption> = context.dataStore.data.map { prefs ->
        val fieldStr = prefs[Keys.SORT_FIELD] ?: SortField.NAME.name
        val ascending = prefs[Keys.SORT_ASCENDING] ?: true
        val field = runCatching { SortField.valueOf(fieldStr) }.getOrDefault(SortField.NAME)
        FileSortOption(field = field, ascending = ascending)
    }

    val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_HIDDEN_FILES] ?: false
    }

    val confirmDelete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_DELETE] ?: true
    }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTIC_FEEDBACK] ?: true
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_ENABLED] ?: true
    }

    val vaultPinHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.VAULT_PIN_HASH]
    }

    val compressionLevel: Flow<CompressionLevel> = context.dataStore.data.map { prefs ->
        val value = prefs[Keys.COMPRESSION_LEVEL] ?: CompressionLevel.NORMAL.name
        runCatching { CompressionLevel.valueOf(value) }.getOrDefault(CompressionLevel.NORMAL)
    }

    val isProUser: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRO_ENTITLEMENT] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { prefs -> prefs[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { prefs -> prefs[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setSortOption(sortOption: FileSortOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_FIELD] = sortOption.field.name
            prefs[Keys.SORT_ASCENDING] = sortOption.ascending
        }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SHOW_HIDDEN_FILES] = show }
    }

    suspend fun setConfirmDelete(confirm: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.CONFIRM_DELETE] = confirm }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setVaultPinHash(hash: String) {
        context.dataStore.edit { prefs -> prefs[Keys.VAULT_PIN_HASH] = hash }
    }

    suspend fun setCompressionLevel(level: CompressionLevel) {
        context.dataStore.edit { prefs -> prefs[Keys.COMPRESSION_LEVEL] = level.name }
    }

    suspend fun setProUser(isPro: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PRO_ENTITLEMENT] = isPro }
    }
}
