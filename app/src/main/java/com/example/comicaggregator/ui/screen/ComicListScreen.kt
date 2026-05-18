package com.example.comicaggregator.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.comicaggregator.data.model.ComicBook
import com.example.comicaggregator.ui.state.ComicUiState
import com.example.comicaggregator.ui.viewmodel.ComicViewModel

// sleeker deep dark color system tokens (avoiding generic primary colors)
private val DarkBackground = Color(0xFF0C0C10)
private val CardBackground = Color(0xFF161622)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonViolet = Color(0xFF9B51E0)
private val TextWhite = Color(0xFFF0F0F5)
private val TextMuted = Color(0xFF9E9EB2)

/**
 * Main Premium Compose entry point for displaying the comic aggregator feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicListScreen(
    viewModel: ComicViewModel,
    onComicClick: (ComicBook) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "COMIC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = " PULSE",
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp,
                            color = TextWhite
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search comics...", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NeonCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.fetchComics(if (searchQuery.isNotBlank()) searchQuery else null)
                    }
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.fetchComics(null) 
                        }) {
                            Icon(Icons.Default.Clear, "Clear Search", tint = TextMuted)
                        }
                    } else {
                        Icon(Icons.Default.Search, "Search", tint = TextMuted)
                    }
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is ComicUiState.Loading -> {
                        ComicShimmerGrid()
                    }
                    is ComicUiState.Success -> {
                        ComicGrid(
                            comics = state.comics,
                            isFetchingMore = state.isFetchingMore,
                            isLastPage = state.isLastPage,
                            pageError = state.pageError,
                            searchQuery = state.searchQuery,
                            onComicClick = onComicClick,
                            onLoadNextPage = { viewModel.fetchNextPage() }
                        )
                    }
                    is ComicUiState.Error -> {
                        ErrorStateScreen(
                            message = state.message,
                            onRetry = { viewModel.fetchComics(if (searchQuery.isNotBlank()) searchQuery else null) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders the actual fetched list of comics in a clean, responsive double-column grid.
 * Integrates premium prefetching infinite scroll triggers and full-width footer states.
 */
@Composable
fun ComicGrid(
    comics: List<ComicBook>,
    isFetchingMore: Boolean,
    isLastPage: Boolean,
    pageError: String?,
    searchQuery: String?,
    onComicClick: (ComicBook) -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    // Elegant prefetching trigger: load next page when scrolled past the 4th-to-last item
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= (totalItemsCount - 4)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isFetchingMore && !isLastPage && pageError == null) {
            onLoadNextPage()
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(comics) { comic ->
            ComicCard(comic = comic, onClick = { onComicClick(comic) })
        }

        // Full-width pagination footers (occupying all columns in the 2-column grid layout)
        if (isFetchingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else if (pageError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .clickable { onLoadNextPage() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF5A5F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Failed to load more page items.",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap here to retry connection.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (isLastPage) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNullOrBlank()) "You've caught up with the latest releases!" else "No more results found.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * A beautiful, elevated, glassmorphic card for individual Comic items.
 */
@Composable
fun ComicCard(
    comic: ComicBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Elegant Coil cover image component with native transition handling
                SubcomposeAsyncImage(
                    model = comic.coverImageUrl,
                    contentDescription = comic.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(shimmerBrush()),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF252538), Color(0xFF12121A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Image Error",
                                    tint = NeonViolet,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cover Unreachable",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )

                // High-fidelity floating category tag
                if (!comic.category.isNullOrEmpty()) {
                    val categoryColor = when (comic.category.uppercase()) {
                        "DC COMICS" -> Color(0xFF007BFF) // DC Blue
                        "MARVEL" -> Color(0xFFE50914) // Marvel Red
                        else -> Color(0xCC00F2FE) // Elegant glass teal
                    }
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .background(color = categoryColor, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = comic.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Text Metadata Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Comic Title (max 2 lines with proper ellipsis to preserve layout)
                Text(
                    text = comic.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Year & Size metadata pills
                if (comic.year != null || comic.size != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        comic.year?.let {
                            MetadataPill(text = it, backgroundColor = Color(0x3300F2FE), textColor = NeonCyan)
                        }
                        comic.size?.let {
                            MetadataPill(text = it, backgroundColor = Color(0x339B51E0), textColor = NeonViolet)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A beautiful pill representing metadata (size, year).
 */
@Composable
fun MetadataPill(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * Premium skeleton shimmer loader displayed while downloading and parsing HTML.
 */
@Composable
fun ComicShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .background(shimmerBrush())
                    )
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush())
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush())
                        )
                    }
                }
            }
        }
    }
}

/**
 * A high-performance linear gradient shimmer brush in pure Jetpack Compose.
 */
@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        Color(0xFF1E1E28),
        Color(0xFF2E2E3A),
        Color(0xFF1E1E28)
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnimation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

/**
 * Sleek premium error display with descriptive details and high-contrast retry action.
 */
@Composable
fun ErrorStateScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF5A5F),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connection Disrupted",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextMuted,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry Icon",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retry Loading",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 15.sp
                )
            }
        }
    }
}
