package ui.screens.webtoonreader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import core.source.SourcePage
import data.reader.ReaderColorFilters
import data.settings.ReaderColorFilter
import ui.theme.TextPrimary
import ui.theme.TextSecondary

/**
 * Authentic Manga Page renderer.
 * Loads remote manga page via Coil 3. If missing or failed, presents an explicit error card
 * with a retry action instead of procedurally faking comic panels.
 */
@Composable
fun MangaPageCanvas(
    pageIndex: Int,
    totalPages: Int,
    chapterTitle: String = "Chapter 42",
    isRtl: Boolean = true,
    cropBorders: Boolean = false,
    page: SourcePage? = null,
    colorFilter: ReaderColorFilter = ReaderColorFilter.DEFAULT,
    modifier: Modifier = Modifier
) {
    val pageNumber = pageIndex + 1

    val imageUrl = page?.imageUrl ?: page?.url
    val isRemoteImage = !imageUrl.isNullOrBlank() &&
        (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))

    val canvasBg = remember(colorFilter) { ReaderColorFilters.getCanvasBackgroundColor(colorFilter) }
    val appliedFilter = remember(colorFilter) { ReaderColorFilters.toColorFilter(colorFilter) }

    var retryKey by remember { mutableIntStateOf(0) }
    var loadError by remember(retryKey, imageUrl) { mutableStateOf<String?>(null) }

    if (isRemoteImage) {
        val platformContext = LocalPlatformContext.current
        val imageRequest = remember(imageUrl, page?.headers, retryKey) {
            ImageRequest.Builder(platformContext)
                .data(imageUrl)
                .apply {
                    val headers = page?.headers
                    val networkHeaders = NetworkHeaders.Builder().apply {
                        add("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                        headers?.forEach { (k, v) -> add(k, v) }
                    }.build()
                    httpHeaders(networkHeaders)
                }
                .build()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(canvasBg)
                .padding(if (cropBorders) 0.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loadError != null) {
                MangaPageErrorCard(
                    pageNumber = pageNumber,
                    errorMessage = loadError,
                    onRetry = {
                        loadError = null
                        retryKey++
                    }
                )
            } else {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Page $pageNumber",
                    contentScale = ContentScale.Fit,
                    colorFilter = appliedFilter,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Error -> {
                                println("[MangaPageCanvas] Page $pageNumber FAILED: ${state.result.throwable.message}")
                                loadError = state.result.throwable.message ?: "Failed to load page"
                            }
                            is AsyncImagePainter.State.Success -> {
                                println("[MangaPageCanvas] Page $pageNumber SUCCESS: ${state.result.image.width}x${state.result.image.height}")
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(canvasBg)
                .padding(if (cropBorders) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            MangaPageErrorCard(
                pageNumber = pageNumber,
                errorMessage = "No image source available",
                onRetry = null
            )
        }
    }
}

@Composable
private fun MangaPageErrorCard(
    pageNumber: Int,
    errorMessage: String?,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        if (onRetry != null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = "Failed to load page $pageNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = errorMessage ?: "Image could not be retrieved",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tap to Retry",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
