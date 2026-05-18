package com.example.comicaggregator.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.comicaggregator.data.parser.GetComicsParserImpl
import com.example.comicaggregator.data.repository.ComicRepositoryImpl
import com.example.comicaggregator.ui.screen.ComicDetailScreen
import com.example.comicaggregator.ui.screen.ComicListScreen
import com.example.comicaggregator.ui.screen.ComicReaderScreen
import com.example.comicaggregator.ui.screen.LibraryScreen
import com.example.comicaggregator.ui.viewmodel.ComicDetailViewModel
import com.example.comicaggregator.ui.viewmodel.ComicReaderViewModel
import com.example.comicaggregator.ui.viewmodel.ComicViewModel
import com.example.comicaggregator.ui.viewmodel.LibraryViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parser = GetComicsParserImpl()
        val repository = ComicRepositoryImpl(parser)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
                val isRootScreen = currentRoute == "feed" || currentRoute == "library" || currentRoute == "downloads"

                Scaffold(
                    bottomBar = {
                        if (isRootScreen) {
                            NavigationBar(
                                containerColor = Color(0xFF0C0C10),
                                contentColor = Color(0xFF9E9EB2)
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                                    label = { Text("Discover") },
                                    selected = currentRoute == "feed",
                                    onClick = {
                                        navController.navigate("feed") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00F2FE),
                                        selectedTextColor = Color(0xFF00F2FE),
                                        unselectedIconColor = Color(0xFF9E9EB2),
                                        unselectedTextColor = Color(0xFF9E9EB2),
                                        indicatorColor = Color(0xFF161622)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = "Library") },
                                    label = { Text("Library") },
                                    selected = currentRoute == "library",
                                    onClick = {
                                        navController.navigate("library") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00F2FE),
                                        selectedTextColor = Color(0xFF00F2FE),
                                        unselectedIconColor = Color(0xFF9E9EB2),
                                        unselectedTextColor = Color(0xFF9E9EB2),
                                        indicatorColor = Color(0xFF161622)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Downloads") },
                                    label = { Text("Downloads") },
                                    selected = currentRoute == "downloads",
                                    onClick = {
                                        navController.navigate("downloads") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00F2FE),
                                        selectedTextColor = Color(0xFF00F2FE),
                                        unselectedIconColor = Color(0xFF9E9EB2),
                                        unselectedTextColor = Color(0xFF9E9EB2),
                                        indicatorColor = Color(0xFF161622)
                                    )
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        NavHost(
                            navController = navController,
                            startDestination = "feed",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("feed") {
                                val viewModel: ComicViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return ComicViewModel(repository) as T
                                        }
                                    }
                                )
                                ComicListScreen(
                                    viewModel = viewModel,
                                    onComicClick = { comic ->
                                        val encodedUrl = URLEncoder.encode(
                                            comic.detailUrl,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        val encodedCover = URLEncoder.encode(
                                            comic.coverImageUrl,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        navController.navigate("detail/$encodedUrl?cover=$encodedCover")
                                    }
                                )
                            }

                            composable("downloads") {
                                val downloadsViewModel: com.example.comicaggregator.ui.viewmodel.DownloadsViewModel = viewModel()
                                com.example.comicaggregator.ui.screen.DownloadsScreen(viewModel = downloadsViewModel)
                            }

                            composable("library") {
                                val libraryViewModel: LibraryViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return LibraryViewModel(application, repository) as T
                                        }
                                    }
                                )
                                LibraryScreen(
                                    viewModel = libraryViewModel,
                                    onComicClick = { comic ->
                                        val encodedUri = URLEncoder.encode(
                                            comic.uri.toString(),
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        val encodedTitle = URLEncoder.encode(
                                            comic.name,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        navController.navigate("reader?uri=$encodedUri&isCbr=${comic.isCbr}&title=$encodedTitle")
                                    }
                                )
                            }

                                composable(
                                    route = "reader?uri={uri}&isCbr={isCbr}&title={title}",
                                    arguments = listOf(
                                        navArgument("uri") { type = NavType.StringType; defaultValue = "" },
                                        navArgument("isCbr") { type = NavType.BoolType; defaultValue = false },
                                        navArgument("title") { type = NavType.StringType; defaultValue = "Comic" }
                                    )
                                ) { backStackEntry ->
                                    val decodedUri = backStackEntry.arguments?.getString("uri") ?: ""
                                    val isCbr = backStackEntry.arguments?.getBoolean("isCbr") ?: false
                                    val title = backStackEntry.arguments?.getString("title") ?: "Comic"

                                val readerViewModel: ComicReaderViewModel = viewModel()

                                LaunchedEffect(decodedUri) {
                                    readerViewModel.loadComic(decodedUri, isCbr)
                                }

                                ComicReaderScreen(
                                    title = title,
                                    viewModel = readerViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "detail/{detailUrl}?cover={coverUrl}",
                                arguments = listOf(
                                    navArgument("detailUrl") { type = NavType.StringType },
                                    navArgument("coverUrl") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) { backStackEntry ->
                                val decodedUrl = backStackEntry.arguments?.getString("detailUrl") ?: ""
                                val decodedCover = backStackEntry.arguments?.getString("coverUrl") ?: ""

                                val detailViewModel: ComicDetailViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return ComicDetailViewModel(repository) as T
                                        }
                                    }
                                )

                                ComicDetailScreen(
                                    url = decodedUrl,
                                    coverUrl = decodedCover,
                                    viewModel = detailViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onDownloadClick = { downloadUrl ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "No application found to handle this link.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
