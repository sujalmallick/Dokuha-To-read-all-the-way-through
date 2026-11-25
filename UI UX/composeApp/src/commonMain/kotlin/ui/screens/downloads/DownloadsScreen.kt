package ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.download.DownloadJob
import core.download.DownloadManager
import core.download.DownloadStatus
import core.source.SourceManga
import data.mock.MockDownloadItem
import data.mock.MockStorageInfo
import data.mock.PanelRepository
import kotlin.math.roundToInt
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.DownloadQueueCard
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelTab
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.maintab.MainTabScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.CoverGradients
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

class DownloadsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val downloadManager: DownloadManager = koinInject()

        val storageInfo by downloadManager.storageInfo.collectAsState()
        val queue by downloadManager.queue.collectAsState()
        val completed by downloadManager.completedDownloads.collectAsState()

        DownloadsScreenContent(
            storageInfo = storageInfo,
            queue = queue,
            completed = completed,
            onPauseToggle = { job ->
                if (job.status == DownloadStatus.PAUSED) downloadManager.resume(job.id)
                else downloadManager.pause(job.id)
            },
            onCancel = { job -> downloadManager.cancel(job.id) },
            onPauseAll = { downloadManager.pauseAll() },
            onResumeAll = { downloadManager.resumeAll() },
            onClearCompleted = { downloadManager.clearCompleted() },
            onDeleteCompleted = { job ->
                downloadManager.deleteDownloadedChapter(job.manga.url, job.chapter.url)
            },
            onOpenManga = { manga ->
                navigator.push(
                    TitleDetailsScreen(
                        mangaUrl = manga.url,
                        initialTitle = manga.title,
                        initialThumbnailUrl = manga.thumbnailUrl,
                        sourceId = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                    )
                )
            },
            onTabSelected = { tab ->
                navigator.replaceAll(MainTabScreen(tab))
            }
        )
    }
}

@Composable
fun DownloadsScreenContent(
    storageInfo: MockStorageInfo,
    queue: List<DownloadJob> = emptyList(),
    completed: List<DownloadJob> = emptyList(),
    initialDownloads: List<MockDownloadItem> = emptyList(),
    onPauseToggle: (DownloadJob) -> Unit = {},
    onCancel: (DownloadJob) -> Unit = {},
    onPauseAll: () -> Unit = {},
    onResumeAll: () -> Unit = {},
    onClearCompleted: () -> Unit = {},
    onDeleteCompleted: (DownloadJob) -> Unit = {},
    onOpenManga: (SourceManga) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()

    val effectiveQueue = remember(queue, initialDownloads) {
        if (queue.isNotEmpty() || initialDownloads.isEmpty()) queue
        else initialDownloads.map { item ->
            DownloadJob(
                id = item.id,
                manga = item.manga,
                chapter = item.chapter,
                progress = item.progress,
                downloadedBytes = item.downloadedBytes,
                totalBytes = item.totalBytes,
                etaSeconds = item.etaMinutes * 60,
                status = if (item.isPaused) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
            )
        }
    }

    val anyActive = effectiveQueue.any { it.status == DownloadStatus.DOWNLOADING }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Subtle ambient liquid glow orbs behind content for realistic glass refraction
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x18FFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = (size.width * 0.7f).coerceAtLeast(1f)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1038BDF8), // ultra-faint liquid cyan refraction
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.60f),
                    radius = (size.width * 0.8f).coerceAtLeast(1f)
                )
            )
        }

        // LAYER 1: Scrollable Content Layer (marked with glassSource)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 110.dp
            )
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    PanelCircleIconButton(
                        icon = Icons.Default.MoreHoriz,
                        size = 40.dp,
                        iconSize = 20.dp,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.18f),
                        onClick = { /* Menu */ }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Minimal Aesthetic Storage Breakdown Card (Liquid Glass)
            item {
                AestheticStorageWidget(storageInfo = storageInfo)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Downloading Queue Section Header with Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Queue (${effectiveQueue.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (effectiveQueue.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.75.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (anyActive) onPauseAll() else onResumeAll()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (anyActive) "Pause All" else "Resume All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (anyActive) TextSecondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Download Queue Items
            if (effectiveQueue.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Queue is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chapters selected for offline reading appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                itemsIndexed(effectiveQueue) { index, item ->
                    val downloadedMb = item.downloadedBytes / (1024 * 1024)
                    val totalMb = item.totalBytes / (1024 * 1024)
                    val etaMins = (item.etaSeconds / 60).coerceAtLeast(1)

                    val statusText = when (item.status) {
                        DownloadStatus.DOWNLOADING -> {
                            val speedMb = (item.speedBytesPerSec / (1024f * 1024f) * 10f).roundToInt() / 10f
                            "$downloadedMb MB of $totalMb MB • $speedMb MB/s • ${item.etaSeconds}s left"
                        }
                        DownloadStatus.QUEUED -> "$downloadedMb MB of $totalMb MB • Queued"
                        DownloadStatus.PAUSED -> "$downloadedMb MB of $totalMb MB • Paused"
                        DownloadStatus.ERROR -> "Error: ${item.error ?: "Download interrupted"}"
                        DownloadStatus.COMPLETED -> "$totalMb MB • Completed"
                    }

                    DownloadQueueCard(
                        title = item.manga.title,
                        chapter = item.chapter.name,
                        progress = item.progress,
                        downloadedMb = downloadedMb,
                        totalMb = totalMb,
                        etaMinutes = etaMins,
                        isPaused = item.status == DownloadStatus.PAUSED,
                        gradientIndex = index,
                        statusText = statusText,
                        onPauseToggle = { onPauseToggle(item) },
                        onCancel = { onCancel(item) },
                        onClick = { onOpenManga(item.manga) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Completed Downloads Section
            if (completed.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloaded (${completed.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.75.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .clickable(onClick = onClearCompleted)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(completed) { index, item ->
                    val totalMb = item.totalBytes / (1024 * 1024)
                    CompletedChapterCard(
                        title = item.manga.title,
                        chapter = item.chapter.name,
                        totalMb = totalMb,
                        gradientIndex = index,
                        onDelete = { onDeleteCompleted(item) },
                        onClick = { onOpenManga(item.manga) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // LAYER 2: Floating Liquid Glass Bottom Navigation Bar (Sibling Overlay)
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CompletedChapterCard(
    title: String,
    chapter: String,
    totalMb: Long,
    gradientIndex: Int,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .clip(coverShape)
                    .background(
                        brush = Brush.verticalGradient(
                            CoverGradients[gradientIndex.mod(CoverGradients.size)]
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), coverShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalMb MB • Ready for Offline",
                        fontSize = 11.sp,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            PanelCircleIconButton(
                icon = Icons.Default.Delete,
                size = 36.dp,
                iconSize = 16.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.16f),
                tint = TextSecondary,
                onClick = onDelete
            )
        }
    }
}

/**
 * Minimal, aesthetic storage breakdown widget with Liquid Glass container,
 * horizontal segmented liquid capsule bar, and refined typography.
 */
@Composable
fun AestheticStorageWidget(
    storageInfo: MockStorageInfo,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val usedGb = storageInfo.usedGb
    val totalGb = storageInfo.totalGb
    val freeGb = (totalGb - usedGb).coerceAtLeast(0f)
    val totalMb = totalGb * 1024f
    val mangaFraction = (storageInfo.mangaMb / totalMb).coerceIn(0f, 1f)
    val manhwaFraction = (storageInfo.manhwaMb / totalMb).coerceIn(0f, 1f)
    val novelsFraction = (storageInfo.novelsMb / totalMb).coerceIn(0f, 1f)
    val othersFraction = (storageInfo.othersMb / totalMb).coerceIn(0f, 1f)
    val freeFraction = (1f - (mangaFraction + manhwaFraction + novelsFraction + othersFraction)).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Storage label & Free Space Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${usedGb} GB used of ${totalGb.toInt()} GB",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Minimal Frosted Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${(freeGb * 10).roundToInt() / 10f} GB Free",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment horizontal liquid capsule bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Manga Segment (Crisp Luxe Platinum White)
                    if (mangaFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(mangaFraction.coerceAtLeast(0.04f))
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                    // Manhwa Segment (Titanium Slate)
                    if (manhwaFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(manhwaFraction.coerceAtLeast(0.03f))
                                .fillMaxHeight()
                                .background(Color(0xFF94A3B8))
                        )
                    }
                    // Novels Segment (Muted Titanium)
                    if (novelsFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(novelsFraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(Color(0xFF64748B))
                        )
                    }
                    // Free space remaining
                    Box(
                        modifier = Modifier
                            .weight(freeFraction.coerceAtLeast(0.05f))
                            .fillMaxHeight()
                            .background(Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimal Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendItem(
                    color = Color.White,
                    label = "Manga ${(storageInfo.mangaMb / 1024f * 10).roundToInt() / 10f} GB"
                )
                StorageLegendItem(
                    color = Color(0xFF94A3B8),
                    label = "Manhwa ${storageInfo.manhwaMb.toInt()} MB"
                )
                StorageLegendItem(
                    color = Color(0xFF64748B),
                    label = "Novels ${storageInfo.novelsMb.toInt()} MB"
                )
            }
        }
    }
}

@Composable
fun StorageLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview
@Composable
fun DownloadsScreenPreview() {
    PanelTheme {
        DownloadsScreenContent(
            storageInfo = MockStorageInfo(),
            queue = emptyList(),
            completed = emptyList(),
            onOpenManga = {},
            onTabSelected = {}
        )
    }
}
