package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.CategoryType
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.screens.*
import com.example.ui.theme.TopFileManagerTheme
import com.example.ui.viewmodel.*
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val browserViewModel: FileBrowserViewModel by viewModels()
    private val toolsViewModel: ToolsViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestStoragePermissions()

        setContent {
            val userPrefs = remember { UserPreferencesRepository(applicationContext) }
            val themeMode by userPrefs.themeMode.collectAsStateWithLifecycle(initialValue = com.example.data.model.ThemeMode.DARK)
            val accentColor by userPrefs.accentColor.collectAsStateWithLifecycle(initialValue = com.example.data.model.AccentColor.ELECTRIC_BLUE)
            val isPro by settingsViewModel.isProUser.collectAsStateWithLifecycle(initialValue = false)

            TopFileManagerTheme(themeMode = themeMode, accentColor = accentColor) {
                MainAppNavHost(
                    homeViewModel = homeViewModel,
                    browserViewModel = browserViewModel,
                    toolsViewModel = toolsViewModel,
                    vaultViewModel = vaultViewModel,
                    settingsViewModel = settingsViewModel,
                    isPro = isPro
                )
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
            )
        }
    }
}

sealed class NavScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavScreen("home", "Home", Icons.Default.Home)
    object Browser : NavScreen("browser", "Browser", Icons.Default.Folder)
    object Tools : NavScreen("tools", "Tools", Icons.Default.Handyman)
    object Vault : NavScreen("vault", "Vault", Icons.Default.Security)
    object Settings : NavScreen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppNavHost(
    homeViewModel: HomeViewModel,
    browserViewModel: FileBrowserViewModel,
    toolsViewModel: ToolsViewModel,
    vaultViewModel: VaultViewModel,
    settingsViewModel: SettingsViewModel,
    isPro: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val bottomNavItems = listOf(
        NavScreen.Home,
        NavScreen.Browser,
        NavScreen.Tools,
        NavScreen.Vault,
        NavScreen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            onClick = {
                                if (screen.route == NavScreen.Browser.route) {
                                    browserViewModel.activeCategoryFilter.value = null
                                }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(text = screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavScreen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToStorage = { path ->
                        browserViewModel.activeCategoryFilter.value = null
                        browserViewModel.navigateTo(path)
                        navController.navigate(NavScreen.Browser.route)
                    },
                    onNavigateToCategory = { category ->
                        val root = Environment.getExternalStorageDirectory()?.absolutePath ?: ""
                        browserViewModel.navigateTo(root)
                        browserViewModel.activeCategoryFilter.value = category
                        navController.navigate(NavScreen.Browser.route)
                    },
                    onNavigateToTools = { navController.navigate(NavScreen.Tools.route) },
                    onNavigateToVault = { navController.navigate(NavScreen.Vault.route) },
                    onNavigateToRecycleBin = { navController.navigate("recycle_bin") },
                    onSearchClick = {
                        browserViewModel.activeCategoryFilter.value = null
                        navController.navigate(NavScreen.Browser.route)
                    },
                    isPro = isPro
                )
            }

            composable(NavScreen.Browser.route) {
                FileBrowserScreen(
                    viewModel = browserViewModel,
                    onOpenFile = { fileItem ->
                        val ext = fileItem.extension.lowercase()
                        if (ext in listOf("txt", "json", "xml", "csv", "md", "html", "log")) {
                            val encoded = URLEncoder.encode(fileItem.path, StandardCharsets.UTF_8.toString())
                            navController.navigate("text_editor/$encoded")
                        } else {
                            openFileExternal(context, fileItem.path, fileItem.mimeType) { filePath ->
                                val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate("text_editor/$encoded")
                            }
                        }
                    }
                )
            }

            composable(NavScreen.Tools.route) {
                ToolsScreen(
                    viewModel = toolsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavScreen.Vault.route) {
                VaultScreen(
                    viewModel = vaultViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavScreen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("recycle_bin") {
                RecycleBinScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "text_editor/{filePath}",
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                TextEditorScreen(
                    filePath = decodedPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun openFileExternal(
    context: android.content.Context,
    filePath: String,
    rawMimeType: String,
    onFallbackText: ((String) -> Unit)? = null
) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val ext = file.extension.lowercase()
        var mimeType = rawMimeType.lowercase()

        // Precise MimeType mapping for Intent "Open With" app chooser
        if (mimeType.isEmpty() || mimeType == "*/*") {
            mimeType = when {
                ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> "image/*"
                ext in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv") -> "video/*"
                ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> "audio/*"
                ext == "pdf" -> "application/pdf"
                ext in listOf("doc", "docx") -> "application/msword"
                ext in listOf("xls", "xlsx") -> "application/vnd.ms-excel"
                ext in listOf("ppt", "pptx") -> "application/vnd.ms-powerpoint"
                ext in listOf("txt", "json", "xml", "csv", "md", "html", "log") -> "text/plain"
                ext == "apk" -> "application/vnd.android.package-archive"
                else -> "*/*"
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(intent, "Open with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val packageManager = context.packageManager
        val resolvedActivities = packageManager.queryIntentActivities(intent, 0)
        val externalApps = resolvedActivities.filter { it.activityInfo.packageName != context.packageName }

        if (externalApps.isNotEmpty()) {
            context.startActivity(chooserIntent)
        } else {
            // If no external app found, attempt built-in text editor for text/doc or show message
            if (ext in listOf("txt", "json", "xml", "csv", "md", "html", "log", "pdf", "doc", "docx") && onFallbackText != null) {
                onFallbackText(filePath)
            } else {
                context.startActivity(chooserIntent)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
