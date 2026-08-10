package com.example.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.data.model.ApkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ApkEngine(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun getApkInfo(apkFile: File): ApkInfo? {
        if (!apkFile.exists() || apkFile.isDirectory) return null
        return try {
            val pkgInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (pkgInfo != null) {
                val appInfo = pkgInfo.applicationInfo ?: return null
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath
                val appName = pm.getApplicationLabel(appInfo).toString()
                val pkgName = pkgInfo.packageName ?: "unknown"
                val versionName = pkgInfo.versionName ?: "1.0"
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }

                val isInstalled = try {
                    pm.getPackageInfo(pkgName, 0)
                    true
                } catch (e: Exception) {
                    false
                }

                ApkInfo(
                    appName = if (appName.isNotBlank()) appName else apkFile.nameWithoutExtension,
                    packageName = pkgName,
                    versionName = versionName,
                    versionCode = versionCode,
                    isInstalled = isInstalled,
                    size = apkFile.length(),
                    apkPath = apkFile.absolutePath
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getInstalledUserApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<InstalledAppItem>()
        try {
            val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            for (pkg in installedPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val sourceDir = appInfo.sourceDir ?: ""
                    val apkFile = File(sourceDir)
                    val size = if (apkFile.exists()) apkFile.length() else 0L

                    apps.add(
                        InstalledAppItem(
                            appName = appName,
                            packageName = pkg.packageName ?: "",
                            versionName = pkg.versionName ?: "1.0",
                            size = size,
                            sourceDir = sourceDir
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // handle
        }
        apps.sortedBy { it.appName.lowercase() }
    }
}

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val size: Long,
    val sourceDir: String
)
