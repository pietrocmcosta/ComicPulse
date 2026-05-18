package com.example.comicaggregator.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.comicaggregator.ui.viewmodel.ComicReaderViewModel
import com.example.comicaggregator.ui.viewmodel.ReaderUiState
import kotlin.math.abs

private val DarkBackground = Color(0xFF000000)
private val NeonCyan = Color(0xFF00F2FE)
private val OverlayColor = Color(0xDD0C0C12)
private val TextMuted = Color(0xFF9E9EB2)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicReaderScreen(
    title: String,
    viewModel: ComicReaderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isOverlayVisible by remember { mutableStateOf(false) }
    var contentScaleMode by remember { mutableStateOf(ContentScale.Fit) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Extracting archive...", color = Color.White)
                    }
                }
            }
            is ReaderUiState.Success -> {
                val pagerState = rememberPagerState(pageCount = { state.pages.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true // Enable swiping between pages
                ) { page ->
                    ComicPage(
                        file = state.pages[page],
                        contentScale = contentScaleMode,
                        onCenterTap = { isOverlayVisible = !isOverlayVisible }
                    )
                }

                // Overlay Controls
                if (isOverlayVisible) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OverlayColor)
                            .padding(top = 32.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bottom Bar (Page Indicator + FIT Modes)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(OverlayColor)
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Fit options
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FIT MODE:",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val modes = listOf(
                                "FIT SCREEN" to ContentScale.Fit,
                                "FIT WIDTH" to ContentScale.FillWidth,
                                "FIT HEIGHT" to ContentScale.FillHeight
                            )
                            
                            modes.forEach { (label, scale) ->
                                val isSelected = contentScaleMode == scale
                                Button(
                                    onClick = { contentScaleMode = scale },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) NeonCyan else Color(0xFF242430),
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "${pagerState.currentPage + 1} / ${state.pages.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is ReaderUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color.Red, modifier = Modifier.padding(32.dp))
                        Button(onClick = onBackClick) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles pinch-to-zoom, translation panning, double-tap zoom resets, and center-tap overlay triggers.
 */
@Composable
fun ComicPage(
    file: java.io.File,
    contentScale: ContentScale,
    onCenterTap: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom and panning states when the page file changes
    LaunchedEffect(file) {
        scale = 1f
        offset = Offset.Zero
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(file, scale) {
                    // Custom gesture detector that ONLY consumes horizontal swipe events when zoomed!
                    // This lets the parent HorizontalPager swipe normally when scale is 1f.
                    detectTransformGesturesCustom(
                        panZoomLock = true,
                        consumePan = scale > 1f
                    ) { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            // Dynamically clamp offsets to prevent dragging the comic page off the active screen bounds
                            val maxOffsetX = (widthPx * (scale - 1f)) / 2f
                            val maxOffsetY = (heightPx * (scale - 1f)) / 2f
                            offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
                .pointerInput(file) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = { tapOffset ->
                            val middleStart = widthPx * 0.25f
                            val middleEnd = widthPx * 0.75f
                            // Trigger center menu only when tap is within the horizontal center 50%
                            if (tapOffset.x in middleStart..middleEnd) {
                                onCenterTap()
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}

/**
 * Custom pointer transform gesture detector that allows conditional gesture consumption.
 * Extremely important to allow HorizontalPager swipes when scale == 1f!
 */
suspend fun PointerInputScope.detectTransformGesturesCustom(
    panZoomLock: Boolean = false,
    consumePan: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanGesture = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val rotationChange = event.calculateRotation()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation) * Math.PI.toFloat() * centroidSize / 180f
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectivePan = if (lockedToPanGesture) Offset.Zero else panChange
                    if (effectivePan != Offset.Zero ||
                        zoomChange != 1f ||
                        rotationChange != 0f
                    ) {
                        onGesture(centroid, effectivePan, zoomChange, rotationChange)
                    }
                    
                    // Consume changes conditionally (only if scale > 1f or multitouch zoom is active)
                    if (consumePan || event.changes.size > 1) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
