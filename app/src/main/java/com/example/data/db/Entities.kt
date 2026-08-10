package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val deletedTimestamp: Long,
    val fileSize: Long,
    val isDirectory: Boolean
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val path: String,
    val name: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_files")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val encryptedPath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val addedTimestamp: Long
)
