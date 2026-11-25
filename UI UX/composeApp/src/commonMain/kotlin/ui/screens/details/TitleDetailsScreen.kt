package ui.screens.details

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.flow.flowOf
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import core.download.DownloadManager
import core.download.DownloadStatus
import data.mock.PanelRepository
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ChapterRowItem
import ui.components.PanelCircleIconButton
import ui.components.PanelButton
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.nonGlassPress
import ui.glass.rememberPanelGlassState
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import ui.theme.TextSecondary

data class TitleDetailsScreen(
    val mangaUrl: String = "manga/the-last-horizon",
    val initialTitle: String = "",
    val initialThumbnailUrl: String? = null,
    val sourceId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val downloadManager: DownloadManager = koinInject()
        val scope = rememberCoroutineScope()
        var refreshTrigger by remember { mutableIntStateOf(0) }
        var isLoadingDetails by remember(mangaUrl, sourceId, refreshTrigger) { mutableStateOf(true) }
        var isLoadingChapters by remember(mangaUrl, sourceId, refreshTrigger) { mutableStateOf(true) }

        val initialSourceId = remember(sourceId, mangaUrl) {
            sourceId?.toLongOrNull()
                ?: repository.resolveSource(sourceId, mangaUrl, null)?.id
                ?: 0L
        }

        val manga by remember(mangaUrl, sourceId, refreshTrigger) {
            repository.getMangaDetails(mangaUrl, sourceId, initialTitle)
                .onEach { isLoadingDetails = false }
        }.collectAsState(
            initial = if (initialTitle.isNotBlank()) {
                SourceManga(
                    url = mangaUrl,
                    title = initialTitle,
                    thumbnailUrl = initialThumbnailUrl,
                    sourceId = initialSourceId
                )
            } else {
                null
            }
        )

        val chapters by remember(mangaUrl, sourceId, refreshTrigger) {
            repository.getChapters(mangaUrl, sourceId)
                .onEach { isLoadingChapters = false }
        }.collectAsState(initial = emptyList())

        val library by remember { repository.getLibrary("All") }.collectAsState(initial = emptyList())
        val isInLibrary = library.any { it.url == mangaUrl }

        val currentManga = manga
        val adaptationManga by remember(currentManga) {
            if (currentManga != null) repository.getAdaptation(currentManga) else flowOf(null)
        }.collectAsState(initial = null)

        TitleDetailsScreenContent(
            manga = manga,
            chapters = chapters,
            adaptationManga = adaptationManga,
            isLoadingDetails = isLoadingDetails,
            isLoadingChapters = isLoadingChapters,
            downloadManager = downloadManager,
            isInLibrary = isInLibrary,
            onRefresh = { refreshTrigger++ },
            onToggleLibrary = {
                val targetManga = manga ?: SourceManga(
                    url = mangaUrl,
                    title = initialTitle.ifBlank { "Unknown" },
                    thumbnailUrl = initialThumbnailUrl,
                    sourceId = initialSourceId
                )
                scope.launch {
                    repository.toggleFavorite(targetManga, !isInLibrary)
                }
            },
            onBack = { navigator.pop() },
            onReadChapter = { chapter ->
                val effectiveSourceId = manga?.sourceId?.takeIf { it != 0L }?.toString()
                    ?: sourceId
                    ?: initialSourceId.takeIf { it != 0L }?.toString()
                if (manga?.isNovel == true) {
                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name, mangaUrl, effectiveSourceId))
                } else {
                    navigator.push(WebtoonReaderScreen(mangaUrl, chapter.url, chapter.name, effectiveSourceId))
                }
            },
            onOpenAdaptation = { target ->
                navigator.push(
                    TitleDetailsScreen(
                        mangaUrl = target.url,
                        initialTitle = target.title,
                        initialThumbnailUrl = target.thumbnailUrl,
                        sourceId = if (target.sourceId != 0L) target.sourceId.toString() else null
                    )
                )
            },
            onDownloadChapter = { chapter ->
                val targetManga = manga ?: SourceManga(
                    url = mangaUrl,
                    title = initialTitle.ifBlank { "Unknown" },
                    thumbnailUrl = initialThumbnailUrl,
                    sourceId = initialSourceId
                )
                repository.enqueueDownload(targetManga, chapter)
            },
            onDownloadAll = {
                val targetManga = manga ?: SourceManga(
                    url = mangaUrl,
                    title = initialTitle.ifBlank { "Unknown" },
                    thumbnailUrl = initialThumbnailUrl,
                    sourceId = initialSourceId
                )
                chapters.forEach { ch ->
                    repository.enqueueDownload(targetManga, ch)
                }
            }
        )
    }
}

@Composable
fun TitleDetailsScreenContent(
    manga: SourceManga?,
    chapters: List<SourceChapter>,
    adaptationManga: SourceManga? = null,
    isLoadingDetails: Boolean = false,
    isLoadingChapters: Boolean = false,
    isInLibrary: Boolean = false,
    onRefresh: () -> Unit = {},
    onToggleLibrary: () -> Unit = {},
    onBack: () -> Unit,
    onReadChapter: (SourceChapter) -> Unit,
    onOpenAdaptation: (SourceManga) -> Unit = {},
    onDownloadChapter: (SourceChapter) -> Unit = {},
    onDownloadAll: () -> Unit = {},
    modifier: Modifier = Modifier,
    downloadManager: DownloadManager = koinInject()
) {
    val glassState = rememberPanelGlassState()
    var isBookmarked by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showBatchDownloadSheet by remember { mutableStateOf(false) }
    var showCatchUpSheet by remember { mutableStateOf(false) }

    val downloadQueue by downloadManager.queue.collectAsState()
    val completedDownloads by downloadManager.completedDownloads.collectAsState()

    val displayManga = manga ?: SourceManga(
        url = "",
        title = if (isLoadingDetails) "Loading..." else "Title Details",
        description = "",
        genres = emptyList(),
        status = MangaStatus.UNKNOWN
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // LAYER 1: Scrolling Content Layer (Glass Sampling Source)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Hero Image Header with Backdrop Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    TitleHeroArtwork(
                        thumbnailUrl = displayManga.thumbnailUrl,
                        title = displayManga.title,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Vertical bottom gradient overlay into dark navy
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.4f to Color.Transparent,
                                    0.75f to PanelBackground.copy(alpha = 0.8f),
                                    1.0f to PanelBackground
                                )
                            )
                    )
                }
            }

            // Main Details Block
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Title
                    Text(
                        text = displayManga.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary text: Status (without fake hardcoded rating)
                    val statusText = when (displayManga.status) {
                        MangaStatus.ONGOING -> "Ongoing"
                        MangaStatus.COMPLETED -> "Completed"
                        MangaStatus.LICENSED -> "Licensed"
                        MangaStatus.PUBLISHING_FINISHED -> "Finished"
                        MangaStatus.CANCELLED -> "Cancelled"
                        MangaStatus.ON_HIATUS -> "On Hiatus"
                        MangaStatus.UNKNOWN -> "Unknown Status"
                    }
                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary text replacing genre chips
                    Text(
                        text = displayManga.genres.joinToString(" · "),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA Row: "Start Reading" & "Previously On... (Recap)"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PanelButton(
                            text = if (chapters.isNotEmpty()) "Start Reading" else "No Chapters",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                if (chapters.isNotEmpty()) {
                                    val firstChapter = chapters.lastOrNull() ?: chapters.first()
                                    onReadChapter(firstChapter)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Feature 5: "Previously On..." Catch-up Button
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, GlassBorderStrong, RoundedCornerShape(12.dp))
                                .clickable { showCatchUpSheet = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "Catch up",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recap",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Row (Add to Library, Download, Track)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionGlassButton(
                            icon = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
                            text = if (isInLibrary) "In Library" else "Add to Library",
                            onClick = onToggleLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        ActionGlassButton(
                            icon = Icons.Default.FileDownload,
                            text = "Download",
                            onClick = {
                                showBatchDownloadSheet = true
                                onDownloadAll()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ActionGlassButton(
                            icon = Icons.Default.FavoriteBorder,
                            text = "Track",
                            onClick = { /* Track */ },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feature 4: Comic ↔ Novel Adaptation Bridge Card
                    AdaptationBridgeCard(
                        currentManga = displayManga,
                        adaptationManga = adaptationManga,
                        onOpenAdaptation = onOpenAdaptation,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // About Section
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val description = displayManga.description?.takeIf { it.isNotBlank() }
                        ?: if (isLoadingDetails) "Loading synopsis..." else ""
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (description.length > 80) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDescriptionExpanded) "Show Less" else "More",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chapters Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoadingChapters && chapters.isEmpty()) "Chapters" else "Chapters (${chapters.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        PanelCircleIconButton(
                            icon = Icons.Default.FileDownload,
                            size = 36.dp,
                            iconSize = 18.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.16f),
                            onClick = { showBatchDownloadSheet = true }
                        )
                    }

                    if (chapters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val undownloaded = remember(chapters, downloadQueue, completedDownloads) {
                            chapters.filter { ch ->
                                val isDownloaded = downloadManager.isChapterDownloaded(displayManga.url, ch.url)
                                val status = downloadManager.getChapterStatus(displayManga.url, ch.url)
                                !isDownloaded && status == null
                            }
                        }

                        if (undownloaded.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickDownloadChip(
                                    label = "+5 Next",
                                    onClick = {
                                        downloadManager.enqueue(displayManga, undownloaded.take(5))
                                    }
                                )
                                if (undownloaded.size > 5) {
                                    QuickDownloadChip(
                                        label = "+10 Next",
                                        onClick = {
                                            downloadManager.enqueue(displayManga, undownloaded.take(10))
                                        }
                                    )
                                }
                                QuickDownloadChip(
                                    label = "All Unread (${undownloaded.size})",
                                    onClick = {
                                        downloadManager.enqueue(displayManga, undownloaded)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (isLoadingChapters && chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Loading chapters...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val retryInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .glassPress(retryInteraction)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, GlassBorder, CapsuleShape)
                                .clickable(
                                    interactionSource = retryInteraction,
                                    indication = null,
                                    onClick = onRefresh
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "No chapters found. Tap to reload",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Chapter List Items (Quiet translucent fill, non-glass content layer)
            items(chapters) { chapter ->
                val dateStr = when (chapter.name) {
                    "Ch. 210" -> "Jun 15, 2024"
                    "Ch. 209" -> "Jun 8, 2024"
                    "Ch. 208" -> "Jun 1, 2024"
                    "Ch. 207" -> "May 25, 2024"
                    else -> "Recently"
                }

                val isDownloaded = remember(chapter.url, completedDownloads) {
                    downloadManager.isChapterDownloaded(displayManga.url, chapter.url)
                }
                val chapterStatus = remember(chapter.url, downloadQueue, completedDownloads) {
                    downloadManager.getChapterStatus(displayManga.url, chapter.url)
                }
                val chapterProgress = remember(chapter.url, downloadQueue) {
                    downloadQueue.firstOrNull { it.chapter.url == chapter.url }?.progress
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ChapterRowItem(
                        title = chapter.name,
                        date = dateStr,
                        isRead = chapter.name == "Ch. 208" || chapter.name == "Ch. 207",
                        isDownloaded = isDownloaded,
                        downloadStatus = chapterStatus,
                        downloadProgress = chapterProgress,
                        onClick = { onReadChapter(chapter) },
                        onDownloadClick = {
                            if (!isDownloaded && chapterStatus == null) {
                                downloadManager.enqueue(displayManga, listOf(chapter))
                            } else if (chapterStatus == DownloadStatus.DOWNLOADING || chapterStatus == DownloadStatus.QUEUED) {
                                downloadManager.pause("${displayManga.url}::${chapter.url}")
                            } else if (chapterStatus == DownloadStatus.PAUSED) {
                                downloadManager.resume("${displayManga.url}::${chapter.url}")
                            } else if (chapterStatus == DownloadStatus.ERROR) {
                                downloadManager.retry("${displayManga.url}::${chapter.url}")
                            }
                        },
                        onOptionsClick = {}
                    )
                }
            }
        }

        // LAYER 2: Floating Top Navigation Bar (Back & Bookmark Overlays)
        // Hard Rule: Sibling overlay to LazyColumn, uses GlassVariant.Clear with 35% scrim over hero art
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelCircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40.dp,
                iconSize = 20.dp,
                glassState = glassState,
                hasDimming = true,
                onClick = onBack
            )
            PanelCircleIconButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                size = 40.dp,
                iconSize = 20.dp,
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else TextPrimary,
                glassState = glassState,
                hasDimming = true,
                onClick = { isBookmarked = !isBookmarked }
            )
        }

        // LAYER 3: Batch Download Bottom Sheet
        BatchDownloadSheet(
            visible = showBatchDownloadSheet,
            manga = displayManga,
            chapters = chapters,
            downloadManager = downloadManager,
            onDismiss = { showBatchDownloadSheet = false }
        )

        // LAYER 4: "Previously On..." Smart Catch-up Modal Sheet
        if (showCatchUpSheet) {
            PreviouslyOnBottomSheet(
                mangaTitle = displayManga.title,
                lastReadChapterTitle = chapters.firstOrNull()?.name ?: "Chapter 1",
                onDismiss = { showCatchUpSheet = false },
                onResumeReading = {
                    if (chapters.isNotEmpty()) {
                        val firstChapter = chapters.lastOrNull() ?: chapters.first()
                        onReadChapter(firstChapter)
                    }
                }
            )
        }
    }
}

/**
 * Adaptation Bridge Card: Connects Manga/Manhwa and Light Novels seamlessly.
 */
@Composable
fun AdaptationBridgeCard(
    currentManga: SourceManga,
    adaptationManga: SourceManga?,
    onOpenAdaptation: (SourceManga) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val buttonInteraction = remember { MutableInteractionSource() }

    val isNovel = currentManga.isNovel
    val targetType = if (isNovel) "Illustrated Comic" else "Original Webnovel"
    val subtitle = if (isNovel) {
        "Read the visual manhwa/comic adaptation"
    } else {
        "Read ahead in the original light novel"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF131926).copy(alpha = 0.9f),
                        Color(0xFF1A1728).copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, GlassBorderStrong, cardShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dual Badge Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isNovel) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.AutoStories,
                        contentDescription = "Adaptation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ADAPTATION BRIDGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isNovel) "Comic" else "Novel",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = adaptationManga?.title ?: targetType,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch Action Button
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = buttonInteraction,
                        indication = null,
                        onClick = {
                            if (adaptationManga != null) {
                                onOpenAdaptation(adaptationManga)
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isNovel) "Read Comic" else "Read Novel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * "Previously On..." Smart Catch-up Modal Sheet.
 * Provides a zero-spoiler synopsis of events up to the last read chapter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviouslyOnBottomSheet(
    mangaTitle: String,
    lastReadChapterTitle: String,
    onDismiss: () -> Unit,
    onResumeReading: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF50B0E17),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), CapsuleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ZERO-SPOILER RECAP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Close",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Previously on $mangaTitle",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Where you left off: $lastReadChapterTitle",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recap Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, GlassBorderStrong, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "🔹", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Following the raid on the northern gate, the protagonist successfully defended the core outpost against overwhelming forces.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "🔹", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New tactical allies were recruited, uncovering traces of a deeper conspiracy inside the imperial alliance.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "⚔️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cliffhanger: An unannounced rift awakened directly beneath the capital district as communications went dark.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Resume
            PanelButton(
                text = "Continue Reading",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    onDismiss()
                    onResumeReading()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Action button in details header (Add to Library, Download, Track).
 * Content layer: quiet translucent fill with hairline stroke.
 */
@Composable
fun ActionGlassButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .nonGlassPress(interactionSource)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, GlassBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean neutral dark glass hero artwork cover placeholder with title initial or icon.
 */
@Composable
fun TitleHeroArtwork(
    thumbnailUrl: String? = null,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val isRemote = !thumbnailUrl.isNullOrBlank() &&
        (thumbnailUrl.startsWith("http://", ignoreCase = true) || thumbnailUrl.startsWith("https://", ignoreCase = true))

    if (isRemote) {
        Box(modifier = modifier) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141926),
                            Color(0xFF0C101A),
                            Color(0xFF070A10)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val initial = title?.trim()?.firstOrNull()?.uppercase()
            if (initial != null) {
                Text(
                    text = initial,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.2f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickDownloadChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, CapsuleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
fun TitleDetailsScreenPreview() {
    PanelTheme {
        TitleDetailsScreenContent(
            manga = null,
            chapters = emptyList(),
            onBack = {},
            onReadChapter = {}
        )
    }
}
