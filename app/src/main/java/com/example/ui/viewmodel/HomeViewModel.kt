package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.FavoriteEntity
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FileManagerRepository(application, UserPreferencesRepository(application))

    val favorites: StateFlow<List<FavoriteEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageLocations = MutableStateFlow<List<StorageLocation>>(emptyList())
    val storageLocations: StateFlow<List<StorageLocation>> = _storageLocations.asStateFlow()

    init {
        loadStorageLocations()
    }

    fun loadStorageLocations() {
        viewModelScope.launch {
            _storageLocations.value = repository.getStorageLocations()
        }
    }

    val categories = listOf(
        FileCategory(CategoryType.IMAGES, "Images"),
        FileCategory(CategoryType.VIDEOS, "Videos"),
        FileCategory(CategoryType.AUDIO, "Audio"),
        FileCategory(CategoryType.DOCUMENTS, "Documents"),
        FileCategory(CategoryType.DOWNLOADS, "Downloads"),
        FileCategory(CategoryType.ARCHIVES, "Archives"),
        FileCategory(CategoryType.APKS, "APKs"),
        FileCategory(CategoryType.RECENT, "Recent Files")
    )

    fun removeBookmark(path: String) {
        viewModelScope.launch {
            repository.removeBookmark(path)
        }
    }
}
