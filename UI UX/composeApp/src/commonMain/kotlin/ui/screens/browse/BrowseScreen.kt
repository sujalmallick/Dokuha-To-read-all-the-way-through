package ui.screens.browse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.MockSourceInfo
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.MangaCoverCard
import ui.components.PanelBottomNavBar
import ui.components.PanelSearchBar
import ui.components.PanelTab
import ui.components.SourceCard
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.details.TitleDetailsScreen
import ui.screens.extensions.ExtensionsScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.ElectricBlue
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class BrowseScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val sources by repository.getSources().collectAsState(initial = emptyList())
        val trending by repository.getTrending().collectAsState(initial = emptyList())

        BrowseScreenContent(
            sources = sources,
            trending = trending,
            onOpenSource = { /* Open Source catalogue */ },
            onSeeAllSources = { navigator.push(ExtensionsScreen()) },
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> {} // Current
                    PanelTab.MORE -> navigator.push(MoreScreen())
                }
            }
        )
    }
}

@Composable
fun BrowseScreenContent(
    sources: List<MockSourceInfo>,
    trending: List<SourceManga>,
    onOpenSource: (MockSourceInfo) -> Unit,
    onSeeAllSources: () -> Unit,
    onOpenManga: (SourceManga) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Ambient liquid refraction glow orbs in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.35f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA855F7).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.55f),
                    radius = size.width * 0.60f
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
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discover stories from around the world.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Global Search Bar with quiet translucent styling in scroll layer
                    PanelSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sources Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sources",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.clickable(onClick = onSeeAllSources)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Source Cards List (Liquid Glass Source Cards)
            items(sources) { source ->
                val avatarColor = when (source.name) {
                    "MangaDex" -> Color(0xFFFF6740)
                    "Comick" -> Color(0xFF60A5FA)
                    "Asura Scans" -> Color(0xFFA855F7)
                    else -> Color.White
                }

                SourceCard(
                    name = source.name,
                    type = source.type,
                    languages = source.languages,
                    avatarColor = avatarColor,
                    onClick = { onOpenSource(source) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Trending Now Section Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trending Now",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Trending Cards (3 in a row matching option 2.png)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    trending.forEachIndexed { index, manga ->
                        val chapterSubtitle = when (manga.title) {
                            "Velvet Night" -> "Ch. 34"
                            "Crimson Oath" -> "Ch. 21"
                            "Silent Winter" -> "Ch. 5"
                            else -> "Ch. 1"
                        }

                        MangaCoverCard(
                            title = manga.title,
                            subtitle = chapterSubtitle,
                            gradientIndex = index + 3,
                            onClick = { onOpenManga(manga) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // LAYER 2: Floating Liquid Glass Bottom Navigation Dock
        PanelBottomNavBar(
            selectedTab = PanelTab.BROWSE,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Preview
@Composable
fun BrowseScreenPreview() {
    PanelTheme {
        BrowseScreenContent(
            sources = FakePanelRepository().getSources().let { emptyList() },
            trending = emptyList(),
            onOpenSource = {},
            onSeeAllSources = {},
            onOpenManga = {},
            onTabSelected = {}
        )
    }
}
