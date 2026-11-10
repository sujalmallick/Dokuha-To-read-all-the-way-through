package ui.screens.updates

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.HistoryGroup
import data.mock.MockUpdateGroup
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.components.UpdateItemCard
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class UpdatesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val updateGroups by repository.getUpdates().collectAsState(initial = emptyList())
        val historyGroups by repository.getHistory().collectAsState(initial = emptyList())

        UpdatesScreenContent(
            updateGroups = updateGroups,
            historyGroups = historyGroups,
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onResumeChapter = { manga, chapter ->
                if (manga.isNovel) {
                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name))
                } else {
                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url))
                }
            },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> {} // Current
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> navigator.push(MoreScreen())
                }
            }
        )
    }
}

@Composable
fun UpdatesScreenContent(
    updateGroups: List<MockUpdateGroup>,
    historyGroups: List<HistoryGroup> = emptyList(),
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedTab by remember { mutableStateOf("Updates") }
    val tabs = listOf("Updates", "History")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Subtle ambient liquid glow orbs behind content for realistic glass refraction
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x18FFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x10A855F7), // ultra-faint liquid violet refraction
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.65f),
                    radius = size.width * 0.8f
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
            // Header Bar & Glass Segmented Control
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = "Updates",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
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
                                onDownloadClick = {}
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

                        itemsIndexed(group.items) { itemIndex, item ->
                            val gradientIdx = (groupIndex * 3) + itemIndex
                            UpdateItemCard(
                                title = item.manga.title,
                                chapter = item.chapter.name,
                                gradientIndex = gradientIdx,
                                thumbnailUrl = item.manga.thumbnailUrl,
                                onResume = { onResumeChapter(item.manga, item.chapter) },
                                onClick = { onOpenManga(item.manga) },
                                onDownloadClick = {}
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

        // LAYER 2: Floating Liquid Glass Bottom Navigation Bar (Sibling Overlay)
        PanelBottomNavBar(
            selectedTab = PanelTab.UPDATES,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
