package ui.screens.browse

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import core.source.BrowseFilters
import core.source.SourceInfo
import core.source.SourceManga
import data.mock.MockSourceInfo
import data.mock.PanelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.MangaCoverCard
import ui.components.PanelCircleIconButton
import ui.components.PanelSearchBar
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.screens.details.TitleDetailsScreen
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

data class SourceDetailScreen(
    val sourceId: String,
    val sourceName: String
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val sources by repository.getSources().collectAsState(initial = emptyList())
        val sourceInfo = sources.find { it.id == sourceId || it.name.equals(sourceName, ignoreCase = true) }
            ?: MockSourceInfo(
                id = sourceId,
                name = sourceName,
                type = "Manga",
                languages = listOf("EN")
            )

        SourceDetailScreenContent(
            sourceInfo = sourceInfo,
            repository = repository,
            onBack = { navigator.pop() },
            onOpenManga = { manga ->
                navigator.push(
                    TitleDetailsScreen(
                        mangaUrl = manga.url,
                        initialTitle = manga.title,
                        initialThumbnailUrl = manga.thumbnailUrl,
                        sourceId = sourceInfo.id
                    )
                )
            }
        )
    }
}

@Composable
fun SourceDetailScreenContent(
    sourceInfo: MockSourceInfo,
    repository: PanelRepository,
    onBack: () -> Unit,
    onOpenManga: (SourceManga) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(BrowseFilters()) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }

    val mangaItems = remember { mutableStateListOf<SourceManga>() }
    var currentPage by remember { mutableIntStateOf(1) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Reset and trigger initial load whenever source, search query or filters change
    LaunchedEffect(sourceInfo.id, searchQuery, filters) {
        if (searchQuery.isNotBlank()) {
            delay(350L) // Debounce rapid keyboard typing
        }
        isInitialLoading = true
        isLoadingMore = false
        hasMorePages = true
        loadError = null
        currentPage = 1
        mangaItems.clear()

        try {
            val results = repository.getSourceCatalogue(
                sourceId = sourceInfo.id,
                page = 1,
                query = searchQuery,
                filters = filters
            ).first()
            val unique = results.distinctBy { it.url }
            mangaItems.addAll(unique)
            hasMorePages = unique.isNotEmpty()
        } catch (e: Exception) {
            loadError = e.message ?: "Failed to load titles"
        } finally {
            isInitialLoading = false
        }
    }

    // Prefetch trigger: observe when user scrolls within 6 items of bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 6
        }
    }

    LaunchedEffect(shouldLoadMore, isInitialLoading, isLoadingMore, hasMorePages) {
        if (shouldLoadMore && !isInitialLoading && !isLoadingMore && hasMorePages) {
            isLoadingMore = true
            loadError = null
            val nextPage = currentPage + 1
            try {
                val results = repository.getSourceCatalogue(
                    sourceId = sourceInfo.id,
                    page = nextPage,
                    query = searchQuery,
                    filters = filters
                ).first()

                val existingUrls = mangaItems.map { it.url }.toSet()
                val unique = results.filter { it.url !in existingUrls }

                if (unique.isEmpty()) {
                    hasMorePages = false
                } else {
                    mangaItems.addAll(unique)
                    currentPage = nextPage
                }
            } catch (e: Exception) {
                loadError = e.message ?: "Failed to load more titles"
            } finally {
                isLoadingMore = false
            }
        }
    }

    val retryLoadMore: () -> Unit = {
        if (!isLoadingMore && hasMorePages) {
            coroutineScope.launch {
                isLoadingMore = true
                loadError = null
                val nextPage = currentPage + 1
                try {
                    val results = repository.getSourceCatalogue(
                        sourceId = sourceInfo.id,
                        page = nextPage,
                        query = searchQuery,
                        filters = filters
                    ).first()

                    val existingUrls = mangaItems.map { it.url }.toSet()
                    val unique = results.filter { it.url !in existingUrls }

                    if (unique.isEmpty()) {
                        hasMorePages = false
                    } else {
                        mangaItems.addAll(unique)
                        currentPage = nextPage
                    }
                } catch (e: Exception) {
                    loadError = e.message ?: "Failed to load more titles"
                } finally {
                    isLoadingMore = false
                }
            }
        }
    }

    val avatarColor = when (sourceInfo.name) {
        "MangaDex" -> Color(0xFFFF6740)
        "Comick" -> Color(0xFF60A5FA)
        "Asura Scans" -> Color(0xFFA855F7)
        else -> Color.White
    }

    val hasActiveFilters = filters.genres.isNotEmpty() ||
        filters.status != null ||
        filters.isNovel != null ||
        filters.sortBy != data.mock.BrowseSortOrder.POPULARITY

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Ambient liquid refraction glow orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(avatarColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.20f),
                    radius = (size.width * 0.70f).coerceAtLeast(1f)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.70f),
                    radius = (size.width * 0.60f).coerceAtLeast(1f)
                )
            )
        }

        // LAYER 1: Scrollable Content Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 48.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar & Controls
            item(span = { GridItemSpan(3) }) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PanelCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            size = 40.dp,
                            iconSize = 20.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.18f),
                            onClick = onBack
                        )

                        Text(
                            text = sourceInfo.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Box {
                            PanelCircleIconButton(
                                icon = Icons.Default.Tune,
                                size = 40.dp,
                                iconSize = 20.dp,
                                backgroundColor = if (hasActiveFilters) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                borderColor = if (hasActiveFilters) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.18f),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Source Info Glass Card
                    val bannerShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bannerShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .border(1.dp, GlassBorderStrong, bannerShape)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(avatarColor.copy(alpha = 0.22f))
                                    .border(1.dp, avatarColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sourceInfo.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sourceInfo.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CapsuleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sourceInfo.type,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    sourceInfo.languages.forEach { lang ->
                                        Box(
                                            modifier = Modifier
                                                .clip(CapsuleShape)
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = lang,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // In-Source Search Bar
                    PanelSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Catalogue Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Catalogue" else "Search Results",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = if (isInitialLoading) "Loading..." else "${mangaItems.size} titles",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Manga Grid Items & Infinite Scroll States
            if (isInitialLoading) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Loading titles...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (mangaItems.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (loadError != null) "Failed to load titles" else "No titles found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loadError ?: "Try adjusting your search query or filters",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = mangaItems,
                    key = { _, manga -> manga.url }
                ) { index, manga ->
                    MangaCoverCard(
                        title = manga.title,
                        subtitle = manga.genres.firstOrNull() ?: (if (manga.isNovel) "Novel" else "Manga"),
                        gradientIndex = index,
                        thumbnailUrl = manga.thumbnailUrl,
                        onClick = { onOpenManga(manga) }
                    )
                }

                // Loading More Indicator
                if (isLoadingMore) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, GlassBorder, CapsuleShape)
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Loading more titles...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Retry Button if Loading More Failed
                if (loadError != null && !isLoadingMore) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val retryInteraction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .glassPress(retryInteraction)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CapsuleShape)
                                    .clickable(
                                        interactionSource = retryInteraction,
                                        indication = null,
                                        onClick = retryLoadMore
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Couldn't load more. Tap to retry",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // End of Catalogue Indicator
                if (!hasMorePages && mangaItems.size >= 8) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "End of catalogue • ${mangaItems.size} titles",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Sibling Overlay: Filter Bottom Sheet
        BrowseFilterBottomSheet(
            visible = isFilterSheetOpen,
            currentFilters = filters,
            glassState = glassState,
            onApplyFilters = { newFilters ->
                filters = newFilters
            },
            onDismiss = { isFilterSheetOpen = false }
        )
    }
}

@Preview
@Composable
fun SourceDetailScreenPreview() {
    PanelTheme {
        SourceDetailScreenContent(
            sourceInfo = MockSourceInfo("1", "MangaDex", "Manga", listOf("EN", "JP")),
            repository = data.mock.FakePanelRepository(),
            onBack = {},
            onOpenManga = {}
        )
    }
}
