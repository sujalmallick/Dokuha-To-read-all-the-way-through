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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import core.logging.AppCoroutineExceptionHandler
import core.logging.launchSafe
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
        val trackerRepository: data.tracker.TrackerRepository = koinInject()
        val scope = rememberCoroutineScope { AppCoroutineExceptionHandler }
        val snackbarHostState = remember { SnackbarHostState() }
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
        val readChapterUrls by remember(mangaUrl) {
            repository.getReadChapterUrls(mangaUrl)
        }.collectAsState(initial = emptySet())

        TitleDetailsScreenContent(
            manga = manga,
            chapters = chapters,
            readChapterUrls = readChapterUrls,
            adaptationManga = adaptationManga,
            isLoadingDetails = isLoadingDetails,
            isLoadingChapters = isLoadingChapters,
            downloadManager = downloadManager,
            isInLibrary = isInLibrary,
            trackerRepository = trackerRepository,
            onRefresh = { refreshTrigger++ },
            onMarkChaptersRead = { urls, isRead ->
                scope.launchSafe(
                    tag = "TitleDetailsScreen",
                    onError = {
                        scope.launchSafe("TitleDetailsScreen", onError = {}) {
                            snackbarHostState.showSnackbar("Could not update chapter status. Please try again.")
                        }
                    }
                ) {
                    repository.markChaptersRead(urls, isRead)
                }
            },
            onToggleLibrary = {
                val targetManga = manga ?: SourceManga(
                    url = mangaUrl,
                    title = initialTitle.ifBlank { "Unknown" },
                    thumbnailUrl = initialThumbnailUrl,
                    sourceId = initialSourceId
                )
                scope.launchSafe(
                    tag = "TitleDetailsScreen",
                    onError = {
                        scope.launchSafe("TitleDetailsScreen", onError = {}) {
                            snackbarHostState.showSnackbar("Could not update library status. Please try again.")
                        }
                    }
                ) {
                    repository.toggleFavorite(targetManga, !isInLibrary)
                }
            },
            snackbarHostState = snackbarHostState,
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

enum class ChapterFilterMode(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    DOWNLOADED("Downloaded")
}

@Composable
fun TitleDetailsScreenContent(
    manga: SourceManga?,
    chapters: List<SourceChapter>,
    readChapterUrls: Set<String> = emptySet(),
    adaptationManga: SourceManga? = null,
    isLoadingDetails: Boolean = false,
    isLoadingChapters: Boolean = false,
    isInLibrary: Boolean = false,
    onRefresh: () -> Unit = {},
    onMarkChaptersRead: (List<String>, Boolean) -> Unit = { _, _ -> },
    onToggleLibrary: () -> Unit = {},
    onBack: () -> Unit,
    onReadChapter: (SourceChapter) -> Unit,
    onOpenAdaptation: (SourceManga) -> Unit = {},
    onDownloadChapter: (SourceChapter) -> Unit = {},
    onDownloadAll: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    downloadManager: DownloadManager = koinInject(),
    trackerRepository: data.tracker.TrackerRepository? = null
) {
    val glassState = rememberPanelGlassState()
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showBatchDownloadSheet by remember { mutableStateOf(false) }
    var showCatchUpSheet by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var chapterFilter by remember { mutableStateOf(ChapterFilterMode.ALL) }
    var sortAscending by remember { mutableStateOf(false) }

    val downloadQueue by downloadManager.queue.collectAsState()
    val completedDownloads by downloadManager.completedDownloads.collectAsState()

    val displayManga = manga ?: SourceManga(
        url = "",
        title = if (isLoadingDetails) "Loading..." else "Title Details",
        description = "",
        genres = emptyList(),
        status = MangaStatus.UNKNOWN
    )

    val chronologicalChapters = remember(chapters) { chapters.asReversed() }
    val lastReadChronoIndex = remember(chronologicalChapters, readChapterUrls) {
        chronologicalChapters.indexOfLast { it.url in readChapterUrls }
    }
    val ctaTargetChapter = remember(chronologicalChapters, lastReadChronoIndex) {
        if (chronologicalChapters.isEmpty()) null
        else if (lastReadChronoIndex >= 0) {
            chronologicalChapters.getOrNull(lastReadChronoIndex + 1)
                ?: chronologicalChapters[lastReadChronoIndex]
        } else {
            chronologicalChapters.first()
        }
    }

    val displayedChapters = remember(chapters, chapterFilter, sortAscending, readChapterUrls, completedDownloads) {
        val filtered = when (chapterFilter) {
            ChapterFilterMode.ALL -> chapters
            ChapterFilterMode.UNREAD -> chapters.filter { it.url !in readChapterUrls }
            ChapterFilterMode.DOWNLOADED -> chapters.filter {
                downloadManager.isChapterDownloaded(displayManga.url, it.url)
            }
        }
        if (sortAscending) filtered.reversed() else filtered
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
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
                        .widthIn(max = 820.dp)
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

                    // Primary CTA Row: "Start Reading" / "Continue Reading Ch. X" & "Recap"
                    val ctaLabel = when {
                        ctaTargetChapter == null -> "No Chapters"
                        lastReadChronoIndex >= 0 -> {
                            val chLabel = if (ctaTargetChapter.chapterNumber > 0f) {
                                "Ch. ${ctaTargetChapter.chapterNumber.toString().removeSuffix(".0")}"
                            } else {
                                ctaTargetChapter.name
                            }
                            "Continue $chLabel"
                        }
                        else -> "Start Reading"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PanelButton(
                            text = ctaLabel,
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                if (ctaTargetChapter != null) {
                                    onReadChapter(ctaTargetChapter)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        // Feature 5: "Previously On..." Catch-up Button
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, GlassBorderStrong, RoundedCornerShape(12.dp))
                                .clickable { showCatchUpSheet = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
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
                            onClick = { showTrackSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (chapters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val firstChapter = chapters.lastOrNull() ?: chapters.first()
                        val latestChapter = chapters.first()
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryColor.copy(alpha = 0.14f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .clickable { onReadChapter(firstChapter) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "First Chapter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryColor.copy(alpha = 0.14f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .clickable { onReadChapter(latestChapter) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Last Chapter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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

            // Chapter Filter & Sort Bar
            if (chapters.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 820.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChapterFilterMode.entries.forEach { mode ->
                                val selected = chapterFilter == mode
                                val primaryColor = MaterialTheme.colorScheme.primary
                                Box(
                                    modifier = Modifier
                                        .clip(CapsuleShape)
                                        .background(
                                            if (selected) primaryColor.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) primaryColor else GlassBorder,
                                            CapsuleShape
                                        )
                                        .clickable { chapterFilter = mode }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        color = if (selected) primaryColor else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, GlassBorder, CapsuleShape)
                                .clickable { sortAscending = !sortAscending }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (sortAscending) "Oldest" else "Newest",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
            items(displayedChapters, key = { it.url }) { chapter ->
                val dateStr = if (chapter.dateUpload > 0L) "Uploaded" else "Recently"
                val isRead = chapter.url in readChapterUrls
                var showChapterMenu by remember(chapter.url) { mutableStateOf(false) }

                val isDownloaded = remember(chapter.url, completedDownloads) {
                    downloadManager.isChapterDownloaded(displayManga.url, chapter.url)
                }
                val chapterStatus = remember(chapter.url, downloadQueue, completedDownloads) {
                    downloadManager.getChapterStatus(displayManga.url, chapter.url)
                }
                val chapterProgress = remember(chapter.url, downloadQueue) {
                    downloadQueue.firstOrNull { it.chapter.url == chapter.url }?.progress
                }

                Box(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    ChapterRowItem(
                        title = chapter.name,
                        date = dateStr,
                        isRead = isRead,
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
                        onOptionsClick = { showChapterMenu = true }
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = showChapterMenu,
                        onDismissRequest = { showChapterMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (isRead) "Mark as unread" else "Mark as read") },
                            onClick = {
                                showChapterMenu = false
                                onMarkChaptersRead(listOf(chapter.url), !isRead)
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Mark previous as read") },
                            onClick = {
                                showChapterMenu = false
                                val idxInDesc = chapters.indexOfFirst { it.url == chapter.url }
                                if (idxInDesc >= 0) {
                                    val previousUrls = chapters.drop(idxInDesc).map { it.url }
                                    onMarkChaptersRead(previousUrls, true)
                                }
                            }
                        )
                    }
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
                contentDescription = "Back",
                size = 40.dp,
                iconSize = 20.dp,
                glassState = glassState,
                hasDimming = true,
                onClick = onBack
            )
            PanelCircleIconButton(
                icon = if (isInLibrary) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isInLibrary) "Remove from library" else "Add to library",
                size = 40.dp,
                iconSize = 20.dp,
                tint = if (isInLibrary) MaterialTheme.colorScheme.primary else TextPrimary,
                glassState = glassState,
                hasDimming = true,
                onClick = onToggleLibrary
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
            val lastReadTitle = if (lastReadChronoIndex >= 0) {
                chronologicalChapters[lastReadChronoIndex].name
            } else {
                chronologicalChapters.firstOrNull()?.name ?: "Chapter 1"
            }
            PreviouslyOnBottomSheet(
                mangaTitle = displayManga.title,
                lastReadChapterTitle = lastReadTitle,
                onDismiss = { showCatchUpSheet = false },
                onResumeReading = {
                    ctaTargetChapter?.let(onReadChapter)
                }
            )
        }

        if (showTrackSheet && trackerRepository != null) {
            TrackModalSheet(
                manga = displayManga,
                latestReadChapterNumber = chapters
                    .filter { it.url in readChapterUrls }
                    .maxOfOrNull { it.chapterNumber.toInt().coerceAtLeast(0) } ?: 0,
                trackerRepository = trackerRepository,
                onDismiss = { showTrackSheet = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
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
    val cardShape = RoundedCornerShape(14.dp)
    val buttonInteraction = remember { MutableInteractionSource() }

    val isNovel = currentManga.isNovel
    val targetType = if (isNovel) "Illustrated Comic" else "Original Novel"
    val subtitle = if (isNovel) "Comic adaptation" else "Original light novel"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, GlassBorder, cardShape)
            .clickable(
                enabled = adaptationManga != null,
                interactionSource = buttonInteraction,
                indication = null,
                onClick = {
                    if (adaptationManga != null) {
                        onOpenAdaptation(adaptationManga)
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNovel) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.AutoStories,
                    contentDescription = "Adaptation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = adaptationManga?.title ?: targetType,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isNovel) "Read Comic" else "Read Novel",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (adaptationManga != null) MaterialTheme.colorScheme.primary else TextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (adaptationManga != null) MaterialTheme.colorScheme.primary else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
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
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECAP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    letterSpacing = 1.4.sp
                )

                Text(
                    text = "Close",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Previously on $mangaTitle",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Left off at $lastReadChapterTitle",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Pick up right where you paused in $lastReadChapterTitle. Your reading progress and chapter position are synced so you can jump back into the story immediately.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 23.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

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
            .defaultMinSize(minHeight = 64.dp)
            .nonGlassPress(interactionSource)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, GlassBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
    val cleanUrl = thumbnailUrl?.trim()
    val isRemote = !cleanUrl.isNullOrBlank() &&
        (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true))

    if (isRemote) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
            val isWide = maxWidth > 540.dp
            if (isWide) {
                // On tablets/iPads, render a dimmed atmospheric fill behind a crisp centered cover
                AsyncImage(
                    model = cleanUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB3070A10))
                )
                AsyncImage(
                    model = cleanUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .padding(top = 48.dp, bottom = 20.dp)
                )
            } else {
                AsyncImage(
                    model = cleanUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackModalSheet(
    manga: SourceManga,
    latestReadChapterNumber: Int,
    trackerRepository: data.tracker.TrackerRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val trackers by trackerRepository.trackers.collectAsState()
    var selectedService by remember { mutableStateOf(data.profile.TrackerService.ANILIST) }
    val activeAccount = trackers.firstOrNull { it.service == selectedService }
    var tokenInput by remember { mutableStateOf("") }
    var searchQuery by remember(manga.title) { mutableStateOf(manga.title) }
    var searchResults by remember { mutableStateOf<List<data.tracker.TrackSearchResult>>(emptyList()) }
    var currentBinding by remember(manga.url, selectedService) {
        mutableStateOf(trackerRepository.getBinding(manga.url, selectedService))
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ui.theme.PanelSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Track Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    data.profile.TrackerService.ANILIST to "AniList",
                    data.profile.TrackerService.MY_ANIME_LIST to "MyAnimeList"
                ).forEach { (service, label) ->
                    val isSelected = selectedService == service
                    val primary = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .background(if (isSelected) primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (isSelected) primary else GlassBorder, CapsuleShape)
                            .clickable { selectedService = service; statusMessage = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) primary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (activeAccount?.isConnected != true) {
                Text(
                    text = "Paste your ${if (selectedService == data.profile.TrackerService.ANILIST) "AniList" else "MyAnimeList"} OAuth access token to sync:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                androidx.compose.material3.OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    placeholder = { Text("Access token", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PanelButton(
                    text = if (isBusy) "Connecting..." else "Connect",
                    onClick = {
                        if (!isBusy) {
                            isBusy = true
                            scope.launch {
                                val res = trackerRepository.connectWithToken(selectedService, tokenInput)
                                isBusy = false
                                statusMessage = res.fold(
                                    onSuccess = { "Connected as ${it.username ?: "User"}" },
                                    onFailure = { it.message ?: "Connection failed" }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Connected as ${activeAccount.username ?: "User"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                val binding = currentBinding
                if (binding == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        PanelButton(
                            text = if (isBusy) "..." else "Search",
                            onClick = {
                                if (!isBusy) {
                                    isBusy = true
                                    scope.launch {
                                        val res = trackerRepository.searchMedia(selectedService, searchQuery)
                                        isBusy = false
                                        res.onSuccess { searchResults = it }
                                            .onFailure { statusMessage = it.message }
                                    }
                                }
                            }
                        )
                    }

                    searchResults.take(4).forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    val newBinding = data.tracker.TrackBinding(
                                        mangaUrl = manga.url,
                                        service = selectedService,
                                        remoteId = result.remoteId,
                                        remoteTitle = result.title,
                                        lastChapterRead = latestReadChapterNumber,
                                        totalChapters = result.totalChapters
                                    )
                                    trackerRepository.saveBinding(newBinding)
                                    currentBinding = newBinding
                                    searchResults = emptyList()
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bind",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = binding.remoteTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters: ${binding.lastChapterRead}${binding.totalChapters?.let { " / $it" } ?: ""}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        PanelButton(
                            text = "Use Read ($latestReadChapterNumber)",
                            variant = ui.components.ButtonVariant.OUTLINE,
                            onClick = {
                                currentBinding = binding.copy(lastChapterRead = latestReadChapterNumber)
                            }
                        )
                    }
                    PanelButton(
                        text = if (isBusy) "Syncing..." else "Sync Progress",
                        onClick = {
                            if (!isBusy) {
                                isBusy = true
                                scope.launch {
                                    val res = trackerRepository.syncProgress(binding)
                                    isBusy = false
                                    statusMessage = res.fold(
                                        onSuccess = {
                                            currentBinding = it
                                            "Synced chapter ${it.lastChapterRead}"
                                        },
                                        onFailure = { it.message ?: "Sync failed" }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
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
