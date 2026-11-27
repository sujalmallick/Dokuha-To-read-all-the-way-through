package ui.screens.webtoonreader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.reader_tap_to_retry
import ui.theme.GlassBorderStrong
import ui.theme.PanelSurface
import ui.theme.PanelSurfaceSubtle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import core.extensions.js.CloudflareChallengeHeuristics
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
    onZoomStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pageNumber = pageIndex + 1

    val imageUrl = page?.imageUrl ?: page?.url
    val hasValidImage = !imageUrl.isNullOrBlank() && (
        imageUrl.startsWith("http://", ignoreCase = true) ||
        imageUrl.startsWith("https://", ignoreCase = true) ||
        imageUrl.startsWith("file:", ignoreCase = true) ||
        imageUrl.startsWith("/")
    )

    val canvasBg = remember(colorFilter) { ReaderColorFilters.getCanvasBackgroundColor(colorFilter) }
    val appliedFilter = remember(colorFilter) { ReaderColorFilters.toColorFilter(colorFilter) }

    var retryKey by remember { mutableIntStateOf(0) }
    var loadError by remember(retryKey, imageUrl) { mutableStateOf<String?>(null) }

    if (hasValidImage) {
        val platformContext = LocalPlatformContext.current
        val defaultUa = remember { CloudflareChallengeHeuristics.defaultReaderUserAgent() }
        val imageRequest = remember(imageUrl, page?.headers, retryKey, defaultUa) {
            buildReaderImageRequest(
                context = platformContext,
                imageUrl = imageUrl,
                headers = page?.headers,
                defaultUserAgent = defaultUa
            )
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
                var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
                var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
                var containerSize by remember { mutableStateOf(IntSize.Zero) }

                LaunchedEffect(scale) {
                    onZoomStateChanged(scale > 1.02f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(imageUrl) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.25f
                                    }
                                }
                            )
                        }
                        .pointerInput(imageUrl) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val isMultiTouch = event.changes.size >= 2
                                    val isZoomed = scale > 1.02f
                                    if (isMultiTouch || isZoomed) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                        val maxX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                        val maxY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                        val newOffset = if (newScale <= 1.02f) {
                                            Offset.Zero
                                        } else {
                                            Offset(
                                                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                            )
                                        }
                                        scale = newScale
                                        offset = newOffset
                                        event.changes.forEach { change ->
                                            if (change.positionChanged()) change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Page $pageNumber",
                        contentScale = ContentScale.Fit,
                        colorFilter = appliedFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        onState = { state ->
                            when (state) {
                                is AsyncImagePainter.State.Error -> {
                                    loadError = state.result.throwable.message ?: "Failed to load page"
                                }
                                else -> {}
                            }
                        }
                    )
                }
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

internal fun computeReaderDecodeSize(
    maxWidthPx: Int = 1440,
    maxHeightPx: Int = 2880
): Size = Size(maxWidthPx.coerceIn(320, 2048), maxHeightPx.coerceIn(480, 4096))

internal fun formatImageLoadError(url: String?, throwableMessage: String?): String {
    val msg = throwableMessage.orEmpty()
    val isCleartext = !url.isNullOrBlank() &&
        url.startsWith("http://", ignoreCase = true) &&
        (msg.contains("cleartext", ignoreCase = true) || msg.contains("not permitted", ignoreCase = true))
    return if (isCleartext) {
        "HTTP image blocked by network security policy (HTTPS required)"
    } else if (msg.isNotBlank()) {
        msg
    } else {
        "Image could not be retrieved"
    }
}

internal fun buildReaderImageRequest(
    context: PlatformContext,
    imageUrl: String,
    headers: Map<String, String>?,
    defaultUserAgent: String,
    decodeSize: Size = computeReaderDecodeSize()
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(imageUrl)
        .size(decodeSize)
        .precision(Precision.INEXACT)
        .apply {
            val networkHeaders = NetworkHeaders.Builder().apply {
                add("User-Agent", defaultUserAgent)
                headers?.forEach { (k, v) -> add(k, v) }
            }.build()
            httpHeaders(networkHeaders)
        }
        .build()
}

@Composable
internal fun MangaPageErrorCard(
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
            .background(PanelSurface.copy(alpha = 0.94f))
            .border(1.dp, GlassBorderStrong, RoundedCornerShape(16.dp))
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
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = errorMessage ?: "Image could not be retrieved",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val retryLabel = stringResource(Res.string.reader_tap_to_retry)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelSurfaceSubtle)
                    .border(1.dp, GlassBorderStrong, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = retryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}
