package ui.screens.updates

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.MockUpdateGroup
import data.mock.PanelRepository
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.CategoryPillRow
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.components.UpdateItemCard
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary

class UpdatesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val updateGroups by repository.getUpdates().collectAsState(initial = emptyList())

        UpdatesScreenContent(
            updateGroups = updateGroups,
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onResumeChapter = { manga, chapter ->
                navigator.push(WebtoonReaderScreen(manga.url, chapter.url))
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
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf("Updates") }
    val tabs = listOf("Updates", "History")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 100.dp
            )
        ) {
            // Header Bar & Tab Filter
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

                    CategoryPillRow(
                        categories = tabs,
                        selectedCategory = selectedTab,
                        onCategorySelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

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

        // Floating Frosted Glass Bottom Navigation Bar
        PanelBottomNavBar(
            selectedTab = PanelTab.UPDATES,
            onTabSelected = onTabSelected,
            hazeState = hazeState,
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
