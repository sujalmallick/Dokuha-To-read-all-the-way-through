package ui.screens.updates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import core.logging.AppCoroutineExceptionHandler
import core.logging.launchSafe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import ui.components.rememberCoverImageRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.HistoryGroup
import data.mock.MockUpdateGroup
import data.mock.PanelRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassSegmentedControl
import ui.components.HistoryItemCard
import ui.components.glass.GlassDialog
import ui.components.glass.GlassSnackbarHost
import ui.theme.LocalGlassTokens
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.components.UpdateItemCard
import ui.components.formatRelativeTime
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.maintab.MainTabScreen
import ui.screens.more.MoreScreen
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.CapsuleShape
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class UpdatesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val coroutineScope = rememberCoroutineScope { AppCoroutineExceptionHandler }
        val snackbarHostState = remember { SnackbarHostState() }

        val updateGroups by repository.getUpdates().collectAsState(initial = emptyList())
        val historyGroups by repository.getHistory().collectAsState(initial = emptyList())
        var isRefreshingUpdates by remember { mutableStateOf(false) }

        UpdatesScreenContent(
            updateGroups = updateGroups,
            historyGroups = historyGroups,
            isRefreshingUpdates = isRefreshingUpdates,
            onRefreshUpdates = {
                if (!isRefreshingUpdates) {
                    isRefreshingUpdates = true
                    coroutineScope.launch {
                        try {
                            val newChapters = repository.refreshLibraryUpdates()
                            val msg = if (newChapters > 0) {
                                "Found $newChapters new chapter${if (newChapters == 1) "" else "s"}"
                            } else {
                                "All caught up — no new updates"
                            }
                            snackbarHostState.showSnackbar(msg)
                        } catch (_: Throwable) {
                            snackbarHostState.showSnackbar("Failed to refresh updates")
                        } finally {
                            isRefreshingUpdates = false
                        }
                    }
                }
            },
            // TODO: re-enable onDownloadUpdate once the blank-page download-integrity bug is fixed
            onDownloadUpdate = null,
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
            onResumeChapter = { manga, chapter ->
                val sid = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                if (manga.isNovel) {
                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name, manga.url, sid))
                } else {
                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name, sid))
                }
            },
            onDeleteHistoryItem = { historyId ->
                coroutineScope.launchSafe(
                    tag = "UpdatesScreen",
                    onError = {
                        coroutineScope.launchSafe("UpdatesScreen", onError = {}) {
                            snackbarHostState.showSnackbar("Could not delete history entry. Please try again.")
                        }
                    }
                ) {
                    repository.deleteHistoryItem(historyId)
                }
            },
            onClearAllHistory = {
                coroutineScope.launchSafe(
                    tag = "UpdatesScreen",
                    onError = {
                        coroutineScope.launchSafe("UpdatesScreen", onError = {}) {
                            snackbarHostState.showSnackbar("Could not clear reading history. Please try again.")
                        }
                    }
                ) {
                    repository.clearAllHistory()
                }
            },
            snackbarHostState = snackbarHostState,
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.replaceAll(MainTabScreen(PanelTab.LIBRARY))
                    PanelTab.UPDATES -> {} // Current
                    PanelTab.BROWSE -> navigator.replaceAll(MainTabScreen(PanelTab.BROWSE))
                    PanelTab.MORE -> navigator.replaceAll(MainTabScreen(PanelTab.MORE))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreenContent(
    updateGroups: List<MockUpdateGroup>,
    historyGroups: List<HistoryGroup> = emptyList(),
    isRefreshingUpdates: Boolean = false,
    onRefreshUpdates: () -> Unit = {},
    onDownloadUpdate: ((SourceManga, SourceChapter) -> Unit)? = null,
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit = {},
    onClearAllHistory: () -> Unit = {},
    onTabSelected: (PanelTab) -> Unit,
    showBottomBar: Boolean = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedTab by remember { mutableStateOf("Updates") }
    val tabs = listOf("Updates", "History")
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        val tokens = LocalGlassTokens.current
        GlassDialog(
            title = "Clear Reading History?",
            onDismissRequest = { showClearHistoryDialog = false },
            confirmText = "Clear All",
            isDestructive = true,
            onConfirm = {
                showClearHistoryDialog = false
                onClearAllHistory()
            }
        ) {
            Text(
                text = "This will remove all reading history entries. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.subtextOnGlass
            )
        }
    }

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
                        Color(0x10A855F7), // ultra-faint liquid violet refraction
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.65f),
                    radius = (size.width * 0.8f).coerceAtLeast(1f)
                )
            )
        }

        // LAYER 1: Scrollable Content Layer (marked with glassSource)
        PullToRefreshBox(
            isRefreshing = selectedTab == "Updates" && isRefreshingUpdates,
            onRefresh = {
                if (selectedTab == "Updates") onRefreshUpdates()
            },
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 110.dp
            )
        ) {
            // Header Bar & Glass Segmented Control
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTab == "Updates") "Updates" else "History",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        if (selectedTab == "Updates") {
                            val infiniteTransition = rememberInfiniteTransition(label = "updates_refresh_spin")
                            val spinAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 850, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "refresh_rotation"
                            )
                            ui.components.PanelCircleIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = "Refresh updates",
                                size = 38.dp,
                                iconSize = 19.dp,
                                glassState = glassState,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = if (isRefreshingUpdates) spinAngle else 0f
                                },
                                onClick = onRefreshUpdates
                            )
                        } else if (selectedTab == "History" && historyGroups.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CapsuleShape)
                                    .clickable { showClearHistoryDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Clear History",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Unified Glass Segmented Control with sliding glass lens indicator
                    GlassSegmentedControl(
                        items = tabs,
                        selectedIndex = tabs.indexOf(selectedTab),
                        onItemSelected = { selectedTab = tabs[it] },
                        labelProvider = { it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (selectedTab == "Updates") {
                if (updateGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No new updates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Updates for followed library titles will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Date Grouped Sections ("Today", "Yesterday") with per-comic dropdowns when >1 chapter
                    updateGroups.forEachIndexed { groupIndex, group ->
                        val byComic = group.items.groupBy { it.manga.url }.values.toList()
                        item {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        itemsIndexed(
                            items = byComic,
                            key = { _, itemsForManga -> "${group.title}_${itemsForManga.first().manga.url}" }
                        ) { itemIndex, itemsForManga ->
                            val gradientIdx = (groupIndex * 3) + itemIndex
                            if (itemsForManga.size == 1) {
                                val single = itemsForManga.first()
                                UpdateItemCard(
                                    title = single.manga.title,
                                    chapter = single.chapter.name,
                                    gradientIndex = gradientIdx,
                                    thumbnailUrl = single.manga.thumbnailUrl,
                                    onResume = { onResumeChapter(single.manga, single.chapter) },
                                    onClick = { onOpenManga(single.manga) },
                                    onDownloadClick = onDownloadUpdate?.let { cb -> { cb(single.manga, single.chapter) } },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            } else {
                                ExpandableComicUpdatesCard(
                                    manga = itemsForManga.first().manga,
                                    chapters = itemsForManga.map { it.chapter },
                                    gradientIndex = gradientIdx,
                                    onOpenManga = { onOpenManga(itemsForManga.first().manga) },
                                    onResumeChapter = { ch -> onResumeChapter(itemsForManga.first().manga, ch) },
                                    onDownloadChapter = onDownloadUpdate?.let { cb -> { ch -> cb(itemsForManga.first().manga, ch) } },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            } else {
                // History Tab View
                if (historyGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No reading history",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Chapters and novels you read will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    historyGroups.forEachIndexed { groupIndex, group ->
                        item {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        itemsIndexed(group.items, key = { _, item -> item.id }) { itemIndex, item ->
                            val gradientIdx = (groupIndex * 3) + itemIndex
                            val progressLabel = if (item.lastPageRead > 0) "Page ${item.lastPageRead}" else null
                            val relTime = formatRelativeTime(item.readAt)

                            HistoryItemCard(
                                title = item.manga.title,
                                chapter = item.chapter.name,
                                timeText = relTime,
                                progressText = progressLabel,
                                gradientIndex = gradientIdx,
                                thumbnailUrl = item.manga.thumbnailUrl,
                                onResume = { onResumeChapter(item.manga, item.chapter) },
                                onClick = { onOpenManga(item.manga) },
                                onDelete = { onDeleteHistoryItem(item.id) },
                                modifier = Modifier.widthIn(max = 720.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
        }

        // LAYER 2: Floating Liquid Glass Bottom Navigation Bar (Sibling Overlay)
        if (showBottomBar) {
            PanelBottomNavBar(
                selectedTab = PanelTab.UPDATES,
                onTabSelected = onTabSelected,
                glassState = glassState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        GlassSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (showBottomBar) 88.dp else 16.dp)
        )
    }
}

@Composable
private fun ExpandableComicUpdatesCard(
    manga: SourceManga,
    chapters: List<SourceChapter>,
    gradientIndex: Int,
    onOpenManga: () -> Unit,
    onResumeChapter: (SourceChapter) -> Unit,
    onDownloadChapter: ((SourceChapter) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Column(
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
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onOpenManga),
                contentAlignment = Alignment.Center
            ) {
                val isRemote = !manga.thumbnailUrl.isNullOrBlank() &&
                    (manga.thumbnailUrl.startsWith("http://", ignoreCase = true) ||
                        manga.thumbnailUrl.startsWith("https://", ignoreCase = true))
                if (isRemote) {
                    coil3.compose.AsyncImage(
                        model = rememberCoverImageRequest(manga.thumbnailUrl!!, widthPx = 200, heightPx = 260),
                        contentDescription = manga.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = manga.title.take(1),
                        color = TextSecondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${chapters.size} new chapters · ${chapters.first().name}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse chapters" else "Expand chapters",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chapters.forEach { chapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onResumeChapter(chapter) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = chapter.name,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TODO: re-enable update chapter download once the blank-page download-integrity bug is fixed
                            if (onDownloadChapter != null) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download ${chapter.name}",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDownloadChapter(chapter) }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Read ${chapter.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onResumeChapter(chapter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun UpdatesScreenPreview() {
    PanelTheme {
        UpdatesScreenContent(
            updateGroups = FakePanelRepository().getUpdates().let { emptyList() },
            onOpenManga = {},
            onResumeChapter = { _, _ -> },
            onTabSelected = {}
        )
    }
}
