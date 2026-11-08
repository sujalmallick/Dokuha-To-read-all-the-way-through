package ui.screens.library

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.components.CategoryPillRow
import ui.components.MangaCoverCard
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelProgressBar
import ui.components.PanelTab
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.CapsuleShape
import ui.theme.CoverGradients
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlass
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class LibraryScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        LibraryScreenContent(
            repository = repository,
            onOpenManga = { manga ->
                navigator.push(TitleDetailsScreen(manga.url))
            },
            onResumeChapter = { manga, chapter ->
                navigator.push(WebtoonReaderScreen(manga.url, chapter.url))
            },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> {} // Current
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> navigator.push(MoreScreen())
                }
            }
        )
    }
}

@Composable
fun LibraryScreenContent(
    repository: PanelRepository,
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Manga", "Manhwa", "Novels")

    val libraryItems by repository.getLibrary(selectedCategory).collectAsState(initial = emptyList())
    val continueReadingPair by repository.getContinueReading().collectAsState(initial = null)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Main scrollable content (glass sampling source)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item(span = { GridItemSpan(3) }) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        PanelCircleIconButton(
                            icon = Icons.Default.Search,
                            size = 40.dp,
                            iconSize = 20.dp,
                            glassState = glassState,
                            onClick = { /* Search Action */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Pills
                    CategoryPillRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Continue Reading Section
            if (continueReadingPair != null) {
                val (manga, chapter) = continueReadingPair!!
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = "Continue Reading",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ContinueReadingCard(
                            manga = manga,
                            chapter = chapter,
                            progress = 0.68f,
                            progressText = "68%",
                            onResume = { onResumeChapter(manga, chapter) },
                            onClick = { onOpenManga(manga) }
                        )
                    }
                }
            }

            // Library Grid Items (3 columns)
            itemsIndexed(libraryItems) { index, manga ->
                val chapterSubtitle = when (manga.title) {
                    "Solo Leveling" -> "Ch. 179"
                    "Aurora Blade" -> "Ch. 24"
                    "Wind Beneath" -> "Ch. 102"
                    "Shadow Crown" -> "Ch. 86"
                    "Neon Requiem" -> "Ch. 47"
                    "Frostbound" -> "Ch. 12"
                    else -> "Ch. 1"
                }

                MangaCoverCard(
                    title = manga.title,
                    subtitle = chapterSubtitle,
                    gradientIndex = index,
                    onClick = { onOpenManga(manga) }
                )
            }
        }

        // Floating Liquid Glass Bottom Navigation Dock
        PanelBottomNavBar(
            selectedTab = PanelTab.LIBRARY,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}

@Composable
fun ContinueReadingCard(
    manga: SourceManga,
    chapter: SourceChapter,
    progress: Float,
    progressText: String,
    onResume: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with gradient artwork placeholder
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF3B1D28),
                                Color(0xFF1E1018),
                                Color(0xFF0F0A10)
                            )
                        )
                    )
                    .border(1.dp, GlassBorderStrong, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = manga.title.take(1),
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title, Chapter, Progress Bar
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PanelProgressBar(
                        progress = progress,
                        modifier = Modifier.weight(1f),
                        height = 4.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = progressText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Resume Button Pill matching option 2.png
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(ElectricBlue, Color(0xFF2563EB))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                        onClick = onResume
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Resume",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    PanelTheme {
        LibraryScreenContent(
            repository = FakePanelRepository(),
            onOpenManga = {},
            onResumeChapter = { _, _ -> },
            onTabSelected = {}
        )
    }
}
