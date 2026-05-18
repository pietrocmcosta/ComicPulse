package com.example.comicaggregator.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comicaggregator.parser.ParserMetadata
import com.example.comicaggregator.parser.ParserRegistry
import kotlinx.coroutines.launch

// ─── Theme tokens (consistent with LibraryScreen) ───
private val DarkBackground = Color(0xFF0C0C10)
private val CardBackground = Color(0xFF161622)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonViolet = Color(0xFF9B51E0)
private val TextWhite = Color(0xFFF0F0F5)
private val TextMuted = Color(0xFF9E9EB2)
private val SwitchOff = Color(0xFF2A2A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    parserRegistry: ParserRegistry,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Snapshot the metadata list into mutable state so toggles are reactive
    var parserItems by remember { mutableStateOf(parserRegistry.getAllMetadata()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ─── Section: Scrapers / Parsers ───
            item {
                Text(
                    text = "COMIC SOURCES",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Enable or disable comic discovery sources. The Discover tab will aggregate results from all active sources.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(parserItems, key = { it.id }) { meta ->
                ParserCard(
                    metadata = meta,
                    onToggle = { newEnabled ->
                        coroutineScope.launch {
                            parserRegistry.setEnabled(meta.id, newEnabled)
                            // Update local snapshot
                            parserItems = parserRegistry.getAllMetadata()
                        }
                    }
                )
            }

            // ─── Section: About ───
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ABOUT",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ComicPulse", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.0.0", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "A premium, high-performance offline comic reader and downloader with plug-and-play parser support.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParserCard(
    metadata: ParserMetadata,
    onToggle: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (metadata.enabled) NeonCyan else Color(0xFF232332),
        label = "borderAnim"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (metadata.enabled) listOf(
                            NeonCyan.copy(alpha = 0.06f),
                            Color.Transparent
                        ) else listOf(Color.Transparent, Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (metadata.enabled) NeonCyan else SwitchOff)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = metadata.displayName,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Version capsule
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonViolet.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${metadata.version}",
                                color = NeonViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (metadata.isBuiltIn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BUILT-IN",
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = metadata.description,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = metadata.baseUrl,
                        color = NeonCyan.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = metadata.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SwitchOff,
                        uncheckedBorderColor = Color.Transparent,
                        checkedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
