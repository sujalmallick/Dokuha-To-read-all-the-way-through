package ui.screens.updates

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        val coroutineScope = rememberCoroutineScope()

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
                            repository.refreshLibraryUpdates()
                        } finally {
                            isRefreshingUpdates = false
                        }
                    }
                }
            },
            onDownloadUpdate = { manga, chapter ->
                repository.enqueueDownload(manga, chapter)
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
            onResumeChapter = { manga, chapter ->
                if (manga.isNovel) {
                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name))
                } else {
                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name))
                }
            },
            onDeleteHistoryItem = { historyId ->
                coroutineScope.launch {
                    repository.deleteHistoryItem(historyId)
                }
            },
            onClearAllHistory = {
                coroutineScope.launch {
                    repository.clearAllHistory()
                }
            },
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
    onDownloadUpdate: (SourceManga, SourceChapter) -> Unit = { _, _ -> },
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit = {},
    onClearAllHistory: () -> Unit = {},
    onTabSelected: (PanelTab) -> Unit,
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedTab by remember { mutableStateOf("Updates") }
    val tabs = listOf("Updates", "History")
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear Reading History?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove all reading history entries. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearAllHistory()
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextPrimary
                    )
                }
            },
            containerColor = Color(0xFF161A26),
            shape = RoundedCornerShape(20.dp)
        )
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
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 110.dp
            )
        ) {
            // Header Bar & Glass Segmented Control
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
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
                            ui.components.PanelCircleIconButton(
                                icon = Icons.Default.Refresh,
                                size = 38.dp,
                                iconSize = 19.dp,
                                glassState = glassState,
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
                    // Date Grouped Sections ("Today", "Yesterday")
                    updateGroups.forEachIndexed { groupIndex, group ->
                        item {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        itemsIndexed(group.items) { itemIndex, item ->
                            val gradientIdx = (groupIndex * 3) + itemIndex
                            UpdateItemCard(
                                title = item.manga.title,
                                chapter = item.chapter.name,
                                gradientIndex = gradientIdx,
                                thumbnailUrl = item.manga.thumbnailUrl,
                                onResume = { onResumeChapter(item.manga, item.chapter) },
                                onClick = { onOpenManga(item.manga) },
                                onDownloadClick = { onDownloadUpdate(item.manga, item.chapter) }
                            )
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
                                color = TextPrimary
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
                                onDelete = { onDeleteHistoryItem(item.id) }
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
