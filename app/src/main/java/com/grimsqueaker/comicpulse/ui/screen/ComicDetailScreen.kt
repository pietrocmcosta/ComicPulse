package com.grimsqueaker.comicpulse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.grimsqueaker.comicpulse.util.DownloadHelper
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import com.grimsqueaker.comicpulse.R
import com.grimsqueaker.comicpulse.data.model.ComicBookDetail
import com.grimsqueaker.comicpulse.ui.state.ComicDetailUiState
import com.grimsqueaker.comicpulse.ui.viewmodel.ComicDetailViewModel

private val DarkBackground = Color(0xFF0C0C10)
private val CardBackground = Color(0xFF161622)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonViolet = Color(0xFF9B51E0)
private val TextWhite = Color(0xFFF0F0F5)
private val TextMuted = Color(0xFF9E9EB2)

@Composable
fun ComicDetailScreen(
    url: String,
    coverUrl: String,
    viewModel: ComicDetailViewModel,
    onBackClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Load detail page on composition
    LaunchedEffect(url) {
        viewModel.loadComicDetail(url)
    }

    val uiState by viewModel.uiState.collectAsState()

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is ComicDetailUiState.Loading -> {
                    DetailContentScreen(
                        detail = null,
                        coverUrl = coverUrl,
                        detailUrl = url,
                        onDownloadClick = onDownloadClick
                    )
                }
                is ComicDetailUiState.Success -> {
                    DetailContentScreen(
                        detail = state.detail,
                        coverUrl = coverUrl,
                        detailUrl = url,
                        onDownloadClick = onDownloadClick
                    )
                }
                is ComicDetailUiState.Error -> {
                    ErrorStateScreen(
                        message = state.message,
                        onRetry = { viewModel.loadComicDetail(url) }
                    )
                }
            }

            // High-fidelity Floating Back Button with glassmorphic backing
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .size(48.dp)
                    .background(color = Color(0x7F000000), shape = CircleShape)
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DetailContentScreen(
    detail: ComicBookDetail?,
    coverUrl: String,
    detailUrl: String,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // High-res Cover Image section with bottom fade gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = "Cover Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Image Error",
                            tint = NeonViolet,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )

            // Dynamic bottom-fading gradient to transition into description text smoothly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, DarkBackground),
                            startY = 400f
                        )
                    )
            )
        }

        // Main Text Metadata & Plots
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (detail == null) {
                // Shimmer for title and content while loading
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(28.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush()))
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush()))
                Spacer(modifier = Modifier.height(28.dp))
                Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
            } else {
                // Full Comic Title
                Text(
                    text = detail.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextWhite,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // plot header
                Text(
                    text = stringResource(R.string.detail_plot_summary),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Body plot summary
                Text(
                    text = detail.description,
                    fontSize = 15.sp,
                    color = Color(0xFFD0D0D5),
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Download options
                Text(
                    text = stringResource(R.string.detail_available_mirrors),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = NeonViolet
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (detail.downloadLinks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = CardBackground, shape = RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.detail_no_mirrors),
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val mainServerUrl = detail.downloadLinks["Main Server"]
                    val externalLinks = detail.downloadLinks.filterKeys { it != "Main Server" }
                    val context = LocalContext.current
                    
                    val storagePrefs = remember { com.grimsqueaker.comicpulse.data.local.StoragePreferences(context) }
                    val libraryUri by storagePrefs.libraryUriFlow.collectAsState(initial = null)
                    
                    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                storagePrefs.saveLibraryUri(uri.toString())
                            }
                        }
                    }

                    if (mainServerUrl != null) {
                        Button(
                            onClick = { 
                                if (libraryUri.isNullOrEmpty()) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.detail_select_folder_warning), android.widget.Toast.LENGTH_LONG).show()
                                    folderPickerLauncher.launch(null)
                                } else {
                                    DownloadHelper.startDirectDownload(
                                        context = context,
                                        url = mainServerUrl,
                                        title = detail.title,
                                        coverImageUrl = detail.coverImageUrl,
                                        description = detail.description,
                                        detailUrl = detailUrl
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.detail_download_now),
                                tint = DarkBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.detail_download_now),
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        if (externalLinks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.detail_external_mirrors),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.5.sp,
                                color = NeonViolet
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (externalLinks.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            externalLinks.forEach { (host, url) ->
                                DownloadHostButton(
                                    hostName = host,
                                    onClick = { onDownloadClick(url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium custom button supporting tailored color palettes based on cloud server host.
 */
@Composable
fun DownloadHostButton(
    hostName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Custom host color maps (gives sleek high-contrast premium theme UI)
    val (buttonColor, textColor) = when (hostName.uppercase()) {
        "MAIN SERVER" -> Pair(NeonCyan, Color.Black)
        "PIXELDRAIN" -> Pair(Color(0xFF8B5CF6), TextWhite) // Vibrant Purple
        "TERABOX" -> Pair(Color(0xFF00B0FF), Color.Black) // Sky Cyan
        "MEGA" -> Pair(Color(0xFFEF4444), TextWhite) // Deep Crimson
        "MEDIAFIRE" -> Pair(Color(0xFF3B82F6), TextWhite) // Marine Blue
        "ROOTZ" -> Pair(Color(0xFF10B981), Color.Black) // Emerald Green
        "VIKINGFILE" -> Pair(Color(0xFFEC4899), TextWhite) // Deep Pink
        "DATANODES" -> Pair(Color(0xFFF59E0B), Color.Black) // Warm Gold
        else -> Pair(Color(0xFF2D2D3A), TextWhite) // Default Glass
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "Open Externally",
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.detail_open_on_mirror, hostName),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = textColor
        )
    }
}

/**
 * Full page loading shimmer skeleton matching detail screen components.
 */
@Composable
fun DetailShimmerScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(shimmerBrush())
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush())
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush())
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush())
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush())
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush())
            )
        }
    }
}
