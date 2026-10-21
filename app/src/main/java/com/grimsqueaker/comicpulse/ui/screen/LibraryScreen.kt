package com.grimsqueaker.comicpulse.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.grimsqueaker.comicpulse.R
import com.grimsqueaker.comicpulse.data.local.LocalComic
import com.grimsqueaker.comicpulse.data.model.ComicBook
import com.grimsqueaker.comicpulse.ui.viewmodel.LibraryUiState
import com.grimsqueaker.comicpulse.ui.viewmodel.LibraryViewModel

private val DarkBackground = Color(0xFF0C0C10)
private val CardBackground = Color(0xFF161622)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonViolet = Color(0xFF9B51E0)
private val TextWhite = Color(0xFFF0F0F5)
private val TextMuted = Color(0xFF9E9EB2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onComicClick: (LocalComic) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewType by viewModel.viewTypeState.collectAsState()
    val context = LocalContext.current
    
    var selectedComics by remember { mutableStateOf(setOf<LocalComic>()) }
    val isSelectionMode = selectedComics.isNotEmpty()
    
    var showMatchDialogForComic by remember { mutableStateOf<LocalComic?>(null) }
    var showLayoutMenu by remember { mutableStateOf(false) }

    // Safely launch the Android SAF Folder Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.setLibraryFolder(uri)
        }
    }

    LaunchedEffect(uiState) {
        selectedComics = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = stringResource(R.string.library_selected_count, selectedComics.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NeonCyan
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.library_title_prefix),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = stringResource(R.string.library_title_suffix),
                                fontWeight = FontWeight.Light,
                                fontSize = 24.sp,
                                color = TextWhite
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedComics = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear_selection), tint = TextWhite)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Match Metadata Button: Show ONLY when exactly one item is selected
                        if (selectedComics.size == 1) {
                            IconButton(onClick = {
                                showMatchDialogForComic = selectedComics.first()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_match_metadata), tint = NeonCyan)
                            }
                        }
                        IconButton(onClick = {
                            viewModel.deleteComics(selectedComics)
                            selectedComics = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_selected), tint = Color.Red)
                        }
                    } else {
                        if (uiState is LibraryUiState.Success) {
                            // Layout Switcher Dropdown
                            Box {
                                IconButton(onClick = { showLayoutMenu = true }) {
                                    Icon(Icons.Default.List, contentDescription = stringResource(R.string.cd_layout_view), tint = TextWhite)
                                }
                                DropdownMenu(
                                    expanded = showLayoutMenu,
                                    onDismissRequest = { showLayoutMenu = false },
                                    modifier = Modifier.background(CardBackground)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.library_view_grid), color = if (viewType == "GRID") NeonCyan else TextWhite) },
                                        onClick = {
                                            viewModel.saveViewType("GRID")
                                            showLayoutMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.library_view_list), color = if (viewType == "LIST") NeonCyan else TextWhite) },
                                        onClick = {
                                            viewModel.saveViewType("LIST")
                                            showLayoutMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.library_view_detailed), color = if (viewType == "DETAIL") NeonCyan else TextWhite) },
                                        onClick = {
                                            viewModel.saveViewType("DETAIL")
                                            showLayoutMenu = false
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.loadLibrary() }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_rescan_library), tint = TextWhite)
                            }
                            IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_change_folder), tint = TextWhite)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                is LibraryUiState.NoFolderSelected -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Folder",
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.library_no_folder_title),
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.library_no_folder_desc),
                            color = TextMuted,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.btn_choose_folder), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is LibraryUiState.Success -> {
                    if (state.comics.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.library_empty_folder),
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    } else {
                        // Render layout dynamically based on chosen viewType flow
                        when (viewType) {
                            "GRID" -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.comics) { comic ->
                                        LocalComicCard(
                                            comic = comic,
                                            isSelected = selectedComics.contains(comic),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedComics = if (selectedComics.contains(comic)) {
                                                        selectedComics - comic
                                                    } else {
                                                        selectedComics + comic
                                                    }
                                                } else {
                                                    onComicClick(comic)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    selectedComics = setOf(comic)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            "LIST" -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.comics) { comic ->
                                        LocalComicListRow(
                                            comic = comic,
                                            isSelected = selectedComics.contains(comic),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedComics = if (selectedComics.contains(comic)) {
                                                        selectedComics - comic
                                                    } else {
                                                        selectedComics + comic
                                                    }
                                                } else {
                                                    onComicClick(comic)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    selectedComics = setOf(comic)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            "DETAIL" -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.comics) { comic ->
                                        LocalComicDetailRow(
                                            comic = comic,
                                            isSelected = selectedComics.contains(comic),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedComics = if (selectedComics.contains(comic)) {
                                                        selectedComics - comic
                                                    } else {
                                                        selectedComics + comic
                                                    }
                                                } else {
                                                    onComicClick(comic)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    selectedComics = setOf(comic)
                                                }
                                            },
                                            onReadClick = { onComicClick(comic) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is LibraryUiState.Error -> {
                    ErrorStateScreen(
                        message = state.message,
                        onRetry = { folderPickerLauncher.launch(null) }
                    )
                }
            }
        }
    }

    // Render the interactive Magnifying Glass match metadata popup
    showMatchDialogForComic?.let { comic ->
        MatchMetadataDialog(
            comic = comic,
            viewModel = viewModel,
            onDismiss = {
                showMatchDialogForComic = null
                selectedComics = emptySet()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalComicCard(
    comic: LocalComic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isSelected) BorderStroke(2.dp, NeonCyan) else null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!comic.coverPath.isNullOrEmpty()) {
                AsyncImage(
                    model = java.io.File(comic.coverPath),
                    contentDescription = comic.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Comic",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (comic.isCbr) "CBR" else "CBZ",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(6.dp)
            ) {
                Text(
                    text = comic.name,
                    color = TextWhite,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalComicListRow(
    comic: LocalComic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(1.5.dp, NeonCyan) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp, 85.dp)
                    .background(Color(0xFF0F0F14), RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (!comic.coverPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = java.io.File(comic.coverPath),
                        contentDescription = comic.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Cover",
                        tint = TextMuted,
                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                    )
                }
                
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.name,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF232332), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (comic.isCbr) "CBR" else "CBZ",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${comic.size / (1024 * 1024)} MB",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalComicDetailRow(
    comic: LocalComic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (isSelected) BorderStroke(2.dp, NeonCyan) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp, 140.dp)
                    .background(Color(0xFF0F0F14), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (!comic.coverPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = java.io.File(comic.coverPath),
                        contentDescription = comic.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Cover",
                        tint = TextMuted,
                        modifier = Modifier.align(Alignment.Center).size(36.dp)
                    )
                }
                
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.name,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF232332), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (comic.isCbr) "CBR" else "CBZ",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = comic.description ?: stringResource(R.string.library_default_comic_desc),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = onReadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.btn_read), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchMetadataDialog(
    comic: LocalComic,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(comic.name.substringBeforeLast(".")) }
    var results by remember { mutableStateOf<List<ComicBook>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    var selectedMatchedBook by remember { mutableStateOf<ComicBook?>(null) }
    
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, NeonViolet),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            val book = selectedMatchedBook
            if (book == null) {
                // Phase 1: Search Online Comics List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.match_title),
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.match_subtitle, comic.name),
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.match_search_label), color = TextMuted) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F0F14),
                                unfocusedContainerColor = Color(0xFF0F0F14),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = NeonCyan,
                                focusedIndicatorColor = NeonCyan,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    searchError = null
                                    viewModel.searchComicsForMatch(
                                        query = searchQuery,
                                        onResult = {
                                            results = it
                                            isSearching = false
                                        },
                                        onError = {
                                            searchError = it
                                            isSearching = false
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_execute_search), tint = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (searchError != null) {
                            Text(
                                text = searchError ?: "",
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (results == null) {
                            Text(
                                text = stringResource(R.string.match_search_prompt),
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (results!!.isEmpty()) {
                            Text(
                                text = stringResource(R.string.match_no_results),
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(results!!) { resultBook ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMatchedBook = resultBook
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                        ) {
                                            AsyncImage(
                                                model = resultBook.coverImageUrl,
                                                contentDescription = "Cover",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(45.dp, 60.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = resultBook.title,
                                                    color = TextWhite,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${resultBook.year ?: "N/A"} | ${resultBook.size ?: "N/A"}",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.btn_cancel), color = NeonCyan)
                    }
                }
            } else {
                // Phase 2: Select Cover Image (Online or Local Comic Page)
                var previewPages by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
                var isLoadingPages by remember { mutableStateOf(true) }
                
                LaunchedEffect(comic) {
                    isLoadingPages = true
                    previewPages = viewModel.getComicPreviewPages(comic)
                    isLoadingPages = false
                }
                
                var selectedIsOnline by remember { mutableStateOf(true) }
                var selectedSource by remember { mutableStateOf(book.coverImageUrl) }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.match_select_cover_title),
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.match_select_cover_subtitle),
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.match_online_cover_option),
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = if (selectedIsOnline) BorderStroke(2.dp, NeonCyan) else BorderStroke(1.dp, Color(0xFF232332)),
                            modifier = Modifier
                                .size(90.dp, 125.dp)
                                .clickable {
                                    selectedIsOnline = true
                                    selectedSource = book.coverImageUrl
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = book.coverImageUrl,
                                    contentDescription = "Online Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selectedIsOnline) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x66000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = stringResource(R.string.match_local_page_options),
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        if (isLoadingPages) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        } else if (previewPages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.match_unable_extract_pages),
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(previewPages) { pageFile ->
                                    val isSelected = !selectedIsOnline && selectedSource == pageFile.absolutePath
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isSelected) BorderStroke(2.dp, NeonCyan) else BorderStroke(1.dp, Color(0xFF232332)),
                                        modifier = Modifier
                                            .size(90.dp, 125.dp)
                                            .clickable {
                                                selectedIsOnline = false
                                                selectedSource = pageFile.absolutePath
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = pageFile,
                                                contentDescription = "Comic Page",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0x66000000)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { selectedMatchedBook = null }) {
                            Text(stringResource(R.string.btn_back), color = NeonCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.toast_matching_metadata), Toast.LENGTH_SHORT).show()
                                viewModel.matchComicMetadataWithCustomCover(
                                    comic = comic,
                                    matchedBook = book,
                                    isOnlineCover = selectedIsOnline,
                                    coverSource = selectedSource
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.btn_apply_cover), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
