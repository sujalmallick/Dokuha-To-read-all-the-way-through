package ui.screens.browse

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import core.source.MangaStatus
import core.source.SourceManga
import data.mock.BrowseFilters
import data.mock.BrowseSortOrder
import data.mock.FakePanelRepository
import data.mock.MockSourceInfo
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.MangaCoverCard
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelSearchBar
import ui.components.PanelTab
import ui.components.SourceCard
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.details.TitleDetailsScreen
import ui.screens.extensions.ExtensionsScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
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
            repository = repository,
            onOpenSource = { source -> navigator.push(SourceDetailScreen(source.id, source.name)) },
            onSeeAllSources = { navigator.push(ExtensionsScreen()) },
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> {}
                    PanelTab.MORE -> navigator.push(MoreScreen())
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseScreenContent(
    sources: List<MockSourceInfo>,
    trending: List<SourceManga>,
    repository: PanelRepository? = null,
    onOpenSource: (MockSourceInfo) -> Unit,
    onSeeAllSources: () -> Unit,
    onOpenManga: (SourceManga) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val repo = repository ?: remember { FakePanelRepository() }

    var searchQuery by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(BrowseFilters()) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }

    val hasActiveFilters = filters.genres.isNotEmpty() ||
        filters.status != null ||
        filters.isNovel != null ||
        filters.sortBy != BrowseSortOrder.POPULARITY

    val isSearching = searchQuery.isNotBlank() || hasActiveFilters

    val searchResults by repo.searchManga(
        query = searchQuery,
        filters = filters
    ).collectAsState(initial = emptyList())

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
            // Header Bar & Search Controls
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

                    // Global Search Bar with Filter Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PanelSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            PanelCircleIconButton(
                                icon = Icons.Default.Tune,
                                size = 48.dp,
                                iconSize = 22.dp,
                                backgroundColor = if (hasActiveFilters) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                borderColor = if (hasActiveFilters) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.16f),
                                onClick = { isFilterSheetOpen = true }
                            )
                            if (hasActiveFilters) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Active Filter Tags Row
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (filters.status != null) {
                                FilterTagChip(
                                    label = when (filters.status) {
                                        MangaStatus.ONGOING -> "Ongoing"
                                        MangaStatus.COMPLETED -> "Completed"
                                        else -> filters.status.toString()
                                    },
                                    onRemove = { filters = filters.copy(status = null) }
                                )
                            }

                            if (filters.isNovel != null) {
                                FilterTagChip(
                                    label = if (filters.isNovel == true) "Novels" else "Manga",
                                    onRemove = { filters = filters.copy(isNovel = null) }
                                )
                            }

                            if (filters.sortBy != BrowseSortOrder.POPULARITY) {
                                FilterTagChip(
                                    label = filters.sortBy.displayName,
                                    onRemove = { filters = filters.copy(sortBy = BrowseSortOrder.POPULARITY) }
                                )
                            }

                            filters.genres.forEach { genre ->
                                FilterTagChip(
                                    label = genre,
                                    onRemove = { filters = filters.copy(genres = filters.genres - genre) }
                                )
                            }

                            // Clear all action
                            Box(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .clickable { filters = BrowseFilters() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // DYNAMIC SECTION: Search Results or Default Catalog
            if (isSearching) {
                // Search Results Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "${searchResults.size} titles",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No matching stories",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Check spelling or reset filters to see all titles",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // 3 in a row grid for search results
                    items(searchResults.chunked(3)) { rowMangas ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMangas.forEachIndexed { index, manga ->
                                MangaCoverCard(
                                    title = manga.title,
                                    subtitle = manga.genres.firstOrNull() ?: (if (manga.isNovel) "Novel" else "Manga"),
                                    gradientIndex = index + 1,
                                    thumbnailUrl = manga.thumbnailUrl,
                                    onClick = { onOpenManga(manga) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty slots in row
                            repeat(3 - rowMangas.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            } else {
                // DEFAULT VIEW: Sources Section Header
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
                if (sources.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable(onClick = onSeeAllSources)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Sources Installed",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to browse and install extensions",
                                    fontSize = 13.sp,
                                    color = ElectricBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else {
                    items(sources) { source ->
                        val avatarColor = when (source.name) {
                            "MangaDex" -> Color(0xFFFF6740)
                            "Comick" -> Color(0xFF60A5FA)
                            "Asura Scans" -> Color(0xFFA855F7)
                            else -> Color(0xFF38BDF8)
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
                }

                // Trending Now Section Header (Only if there are trending titles)
                if (trending.isNotEmpty()) {
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
                                    thumbnailUrl = manga.thumbnailUrl,
                                    onClick = { onOpenManga(manga) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
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

        // Sibling Overlay: Filter Bottom Sheet
        BrowseFilterBottomSheet(
            visible = isFilterSheetOpen,
            currentFilters = filters,
            glassState = glassState,
            onApplyFilters = { filters = it },
            onDismiss = { isFilterSheetOpen = false }
        )
    }
}

@Composable
fun FilterTagChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .glassPress(interactionSource)
            .background(Color.White.copy(alpha = 0.14f))
            .border(0.5.dp, Color.White.copy(alpha = 0.35f), CapsuleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRemove
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
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
