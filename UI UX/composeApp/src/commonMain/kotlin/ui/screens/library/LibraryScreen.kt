package ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import ui.components.rememberCoverImageRequest
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import core.source.SourceChapter
import core.source.SourceManga
import data.library.CategoryRepository
import data.library.DefaultCategories
import data.library.DefaultCategoryRepository
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import data.settings.SettingsRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.CategoryPillRow
import ui.components.ContinueReadingCompactCard
import ui.components.MangaCoverCard
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelProgressBar
import ui.components.PanelTab
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.maintab.MainTabScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

class LibraryScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val categoryRepo: CategoryRepository = koinInject()

        LibraryScreenContent(
            repository = repository,
            categoryRepository = categoryRepo,
            settingsRepository = koinInject(),
            downloadManager = koinInject(),
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
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> {} // Current
                    PanelTab.UPDATES -> navigator.replaceAll(MainTabScreen(PanelTab.UPDATES))
                    PanelTab.BROWSE -> navigator.replaceAll(MainTabScreen(PanelTab.BROWSE))
                    PanelTab.MORE -> navigator.replaceAll(MainTabScreen(PanelTab.MORE))
                }
            }
        )
    }
}

@Composable
fun LibraryScreenContent(
    repository: PanelRepository,
    categoryRepository: CategoryRepository? = null,
    settingsRepository: SettingsRepository? = null,
    downloadManager: core.download.DownloadManager? = null,
    showBottomBar: Boolean = true,
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectionMode: Boolean = false,
    initialSelectedUrls: Set<String> = emptySet(),
    onModalSheetVisibilityChanged: (Boolean) -> Unit = {}
) {
    val showUnreadCount = settingsRepository?.showUnreadCount?.collectAsState()?.value ?: true
    val catRepo = remember(categoryRepository) {
        categoryRepository ?: DefaultCategoryRepository()
    }

    val glassState = rememberPanelGlassState()
    val categories by catRepo.categories.collectAsState()
    val selectedCategory by catRepo.selectedCategory.collectAsState()
    val mappings by catRepo.mangaCategoryMappings.collectAsState()

    val currentCategory = categories.find { it.id == selectedCategory }
    val categoryName = currentCategory?.name ?: "All"

    val allLibraryItems by repository.getLibrary("All").collectAsState(initial = emptyList())
    val libraryStats by repository.getLibraryStats().collectAsState(initial = emptyMap())
    val continueReadingPair by repository.getContinueReading().collectAsState(initial = null)
    val recentlyReadItems by repository.getRecentlyReadManga(limit = 10L).collectAsState(initial = emptyList())

    // 1. Filter library items according to active category (supports explicit mappings + smart status fallback)
    val categoryFilteredItems = remember(allLibraryItems, selectedCategory, mappings, libraryStats) {
        if (selectedCategory == DefaultCategories.ALL) {
            allLibraryItems
        } else {
            allLibraryItems.filter { manga ->
                val assigned = mappings[manga.url]
                    ?: mappings[manga.title.lowercase().replace(" ", "-")]
                    ?: emptySet()
                if (selectedCategory in assigned) {
                    true
                } else if (assigned.isNotEmpty()) {
                    false
                } else {
                    val stats = libraryStats[manga.url]
                    when (selectedCategory) {
                        DefaultCategories.READING -> stats != null && stats.readChapters > 0 && stats.readChapters < stats.totalChapters
                        DefaultCategories.PLAN_TO_READ -> stats == null || stats.readChapters == 0
                        DefaultCategories.COMPLETED -> stats != null && stats.totalChapters > 0 && stats.readChapters >= stats.totalChapters
                        DefaultCategories.ON_HOLD -> manga.status == core.source.MangaStatus.ON_HIATUS
                        else -> false
                    }
                }
            }
        }
    }

    // Filter & Sorting State
    var libraryFilter by remember { mutableStateOf(LibraryFilter()) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val completedDownloads by (
        downloadManager?.completedDownloads
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }
    ).collectAsState()
    val downloadedMangaUrls = remember(completedDownloads) {
        completedDownloads.map { it.manga.url }.toSet()
    }

    // 2. Filter and sort by active LibraryFilter (Status, Format, Progress, Downloaded, Sort) + searchQuery
    val filteredLibraryItems = remember(categoryFilteredItems, libraryFilter, libraryStats, searchQuery, downloadedMangaUrls) {
        val base = filterAndSortLibrary(
            items = categoryFilteredItems,
            filter = libraryFilter,
            stats = libraryStats,
            isDownloaded = { manga -> manga.url in downloadedMangaUrls }
        )
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            base
        } else {
            base.filter { manga ->
                manga.title.contains(q, ignoreCase = true) ||
                    (manga.author?.contains(q, ignoreCase = true) == true) ||
                    manga.genres.any { it.contains(q, ignoreCase = true) }
            }
        }
    }

    // Batch Selection Mode State
    var isSelectionMode by remember { mutableStateOf(initialSelectionMode) }
    var selectedMangaUrls by remember { mutableStateOf(initialSelectedUrls) }

    core.platform.PlatformBackHandler(enabled = isSelectionMode || isSearchActive) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedMangaUrls = emptySet()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    // Modal Sheets & Dialogs State
    var isCategoryEditSheetVisible by remember { mutableStateOf(false) }
    var isBatchMoveSheetVisible by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAnyModalSheetOpen = isFilterSheetVisible || isCategoryEditSheetVisible || isBatchMoveSheetVisible
    LaunchedEffect(isAnyModalSheetOpen, isSelectionMode) {
        onModalSheetVisibilityChanged(isAnyModalSheetOpen || isSelectionMode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Main scrollable content (glass sampling source)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 112.dp),
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    if (isSelectionMode) {
                        // BATCH SELECTION MODE HEADER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PanelCircleIconButton(
                                    icon = Icons.Default.Close,
                                    size = 38.dp,
                                    iconSize = 18.dp,
                                    glassState = glassState,
                                    onClick = {
                                        isSelectionMode = false
                                        selectedMangaUrls = emptySet()
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${selectedMangaUrls.size} Selected",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // Select All / Deselect All Pill
                            val allSelected = filteredLibraryItems.isNotEmpty() &&
                                    selectedMangaUrls.size == filteredLibraryItems.size
                            Box(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CapsuleShape)
                                    .clickable {
                                        selectedMangaUrls = if (allSelected) {
                                            emptySet()
                                        } else {
                                            filteredLibraryItems.map { it.url }.toSet()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (allSelected) "Deselect All" else "Select All",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // STANDARD HEADER
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Filter Toggle Icon with active filter indicator
                                Box {
                                    PanelCircleIconButton(
                                        icon = Icons.Default.FilterList,
                                        contentDescription = "Filter library",
                                        size = 40.dp,
                                        iconSize = 20.dp,
                                        glassState = glassState,
                                        onClick = { isFilterSheetVisible = true }
                                    )
                                    if (libraryFilter.isActive) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 4.dp, end = 4.dp)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3B82F6))
                                                .border(1.5.dp, Color(0xFF0F1219), CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Batch Selection Toggle Icon
                                PanelCircleIconButton(
                                    icon = Icons.Default.Checklist,
                                    contentDescription = "Select titles",
                                    size = 40.dp,
                                    iconSize = 20.dp,
                                    glassState = glassState,
                                    onClick = {
                                        isSelectionMode = true
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Search Icon
                                PanelCircleIconButton(
                                    icon = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchActive) "Close search" else "Search library",
                                    size = 40.dp,
                                    iconSize = 20.dp,
                                    glassState = glassState,
                                    onClick = {
                                        if (isSearchActive) {
                                            isSearchActive = false
                                            searchQuery = ""
                                        } else {
                                            isSearchActive = true
                                        }
                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = isSearchActive) {
                            androidx.compose.material3.OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search library by title or author...",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = TextSecondary
                                            )
                                        }
                                    }
                                } else null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Filter Pills with "+ Edit" button
                        CategoryPillRow(
                            categories = categories.sortedBy { it.order }.map { it.name },
                            selectedCategory = categoryName,
                            onCategorySelected = { name ->
                                val found = categories.find { it.name == name }
                                if (found != null) {
                                    catRepo.selectCategory(found.id)
                                }
                            },
                            onEditCategoriesClick = {
                                isCategoryEditSheetVisible = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Active Filter Chips Strip
                        if (libraryFilter.isActive) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ActiveFilterPillRow(
                                filter = libraryFilter,
                                onUpdateFilter = { libraryFilter = it },
                                onResetAll = { libraryFilter = LibraryFilter() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Continue Reading Section (Only in non-selection mode)
            if (!isSelectionMode) {
                if (recentlyReadItems.size > 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Continue Reading",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${recentlyReadItems.size} in progress",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(recentlyReadItems, key = { it.manga.url }) { recentItem ->
                                    val pageLabel = if (recentItem.lastPageRead > 0) "P. ${recentItem.lastPageRead}" else ""
                                    ContinueReadingCompactCard(
                                        manga = recentItem.manga,
                                        chapter = recentItem.chapter,
                                        progressText = pageLabel,
                                        onResume = { onResumeChapter(recentItem.manga, recentItem.chapter) },
                                        onClick = { onOpenManga(recentItem.manga) }
                                    )
                                }
                            }
                        }
                    }
                } else if (recentlyReadItems.size == 1) {
                    val recentItem = recentlyReadItems.first()
                    val pageLabel = if (recentItem.lastPageRead > 0) "Page ${recentItem.lastPageRead}" else "In progress"
                    val mangaStats = libraryStats[recentItem.manga.url]
                    val realProgress = if (mangaStats != null && mangaStats.totalChapters > 0) {
                        (mangaStats.readChapters.toFloat() / mangaStats.totalChapters.toFloat()).coerceIn(0.02f, 1f)
                    } else 0.05f
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                            Text(
                                text = "Continue Reading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ContinueReadingCard(
                                manga = recentItem.manga,
                                chapter = recentItem.chapter,
                                progress = realProgress,
                                progressText = pageLabel,
                                onResume = { onResumeChapter(recentItem.manga, recentItem.chapter) },
                                onClick = { onOpenManga(recentItem.manga) }
                            )
                        }
                    }
                } else {
                    val fallbackPair = continueReadingPair
                    if (fallbackPair != null) {
                        val (manga, chapter) = fallbackPair
                        val mangaStats = libraryStats[manga.url]
                        val realProgress = if (mangaStats != null && mangaStats.totalChapters > 0) {
                            (mangaStats.readChapters.toFloat() / mangaStats.totalChapters.toFloat()).coerceIn(0.02f, 1f)
                        } else 0f
                        val pctText = "${(realProgress * 100).toInt()}%"
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
                                    progress = realProgress,
                                    progressText = pctText,
                                    onResume = { onResumeChapter(manga, chapter) },
                                    onClick = { onOpenManga(manga) }
                                )
                            }
                        }
                    }
                }
            }

            // Empty Library / Filter State
            if (filteredLibraryItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val hasQuery = searchQuery.isNotBlank()
                            Text(
                                text = if (libraryFilter.isActive || hasQuery) {
                                    "No matching titles"
                                } else if (selectedCategory != DefaultCategories.ALL) {
                                    "No titles in this category"
                                } else {
                                    "Your library is empty"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (libraryFilter.isActive || hasQuery) {
                                    "No titles match your active filter or search criteria."
                                } else if (selectedCategory != DefaultCategories.ALL) {
                                    "Add titles to \"$selectedCategory\" to see them here."
                                } else {
                                    "Install an extension and favorite manga or novels to see them here."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            if (libraryFilter.isActive || hasQuery) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CapsuleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CapsuleShape)
                                        .clickable {
                                            libraryFilter = LibraryFilter()
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Reset Filters",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Library Grid Items (responsive adaptive columns)
            itemsIndexed(filteredLibraryItems, key = { _, manga -> manga.url }) { index, manga ->
                val isSelected = manga.url in selectedMangaUrls
                val stats = libraryStats[manga.url]
                val unread = if (stats != null) (stats.totalChapters - stats.readChapters).coerceAtLeast(0) else null
                val progressFraction = if (stats != null && stats.totalChapters > 0) {
                    (stats.readChapters.toFloat() / stats.totalChapters.toFloat()).coerceIn(0f, 1f)
                } else null
                val chapterSubtitle = when {
                    stats != null && stats.totalChapters > 0 && stats.readChapters > 0 ->
                        "${stats.readChapters}/${stats.totalChapters} ch · ${(progressFraction!! * 100).toInt()}%"
                    stats != null && stats.totalChapters > 0 ->
                        "${stats.totalChapters} chapters"
                    else -> "Unread"
                }

                MangaCoverCard(
                    title = manga.title,
                    subtitle = chapterSubtitle,
                    gradientIndex = index,
                    thumbnailUrl = manga.thumbnailUrl,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    unreadCount = unread,
                    showUnreadBadge = showUnreadCount,
                    progress = progressFraction,
                    onClick = {
                        if (isSelectionMode) {
                            selectedMangaUrls = if (isSelected) {
                                selectedMangaUrls - manga.url
                            } else {
                                selectedMangaUrls + manga.url
                            }
                        } else {
                            onOpenManga(manga)
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            isSelectionMode = true
                            selectedMangaUrls = setOf(manga.url)
                        } else {
                            selectedMangaUrls = if (isSelected) {
                                selectedMangaUrls - manga.url
                            } else {
                                selectedMangaUrls + manga.url
                            }
                        }
                    }
                )
            }
        }

        // LAYER 2: Floating Bottom Bars
        if (isSelectionMode) {
            // FLOATING LIQUID GLASS BATCH ACTION BAR
            BatchActionBar(
                selectedCount = selectedMangaUrls.size,
                onMove = { isBatchMoveSheetVisible = true },
                onMarkRead = {
                    val targetUrls = selectedMangaUrls
                    val selectedMangas = allLibraryItems.filter { it.url in targetUrls }
                    selectedMangaUrls = emptySet()
                    isSelectionMode = false
                    coroutineScope.launch {
                        if (settingsRepository?.incognitoMode?.value == true) {
                            snackbarHostState.showSnackbar("Incognito mode: reading state not saved")
                            return@launch
                        }
                        selectedMangas.forEach { manga ->
                            val sid = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                            val chapters = repository.getChapters(manga.url, sid).first()
                            if (chapters.isNotEmpty()) {
                                repository.markChaptersRead(chapters.map { it.url }, isRead = true)
                            }
                        }
                        snackbarHostState.showSnackbar(
                            "Marked ${selectedMangas.size} title${if (selectedMangas.size == 1) "" else "s"} as read"
                        )
                    }
                },
                onRemove = {
                    if (selectedMangaUrls.isNotEmpty()) {
                        showRemoveConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
        } else if (showBottomBar) {
            // STANDARD NAVIGATION DOCK
            PanelBottomNavBar(
                selectedTab = PanelTab.LIBRARY,
                onTabSelected = onTabSelected,
                glassState = glassState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }

        // LAYER 3: Category Edit Sheet Modal
        CategoryEditSheet(
            categories = categories,
            isVisible = isCategoryEditSheetVisible,
            onDismiss = { isCategoryEditSheetVisible = false },
            onAddCategory = { newName ->
                catRepo.addCategory(newName)
            },
            onDeleteCategory = { catId ->
                catRepo.deleteCategory(catId)
            },
            onMoveCategory = { fromIdx, toIdx ->
                val sorted = categories.sortedBy { it.order }.toMutableList()
                if (fromIdx in sorted.indices && toIdx in sorted.indices) {
                    val item = sorted.removeAt(fromIdx)
                    sorted.add(toIdx, item)
                    catRepo.reorderCategories(sorted.map { it.id })
                }
            }
        )

        // LAYER 4: Batch Move Sheet Modal
        BatchMoveSheet(
            categories = categories.filter { it.id != DefaultCategories.ALL },
            selectedCount = selectedMangaUrls.size,
            isVisible = isBatchMoveSheetVisible,
            onDismiss = { isBatchMoveSheetVisible = false },
            onSelectCategory = { targetCategory ->
                catRepo.batchAssignCategory(selectedMangaUrls, targetCategory.id)
                isSelectionMode = false
                selectedMangaUrls = emptySet()
            }
        )

        // LAYER 5: Library Filter & Sort Sheet Modal
        LibraryFilterSheet(
            filter = libraryFilter,
            isVisible = isFilterSheetVisible,
            onDismiss = { isFilterSheetVisible = false },
            onApply = { libraryFilter = it }
        )

        if (showRemoveConfirmDialog) {
            val count = selectedMangaUrls.size
            AlertDialog(
                onDismissRequest = { showRemoveConfirmDialog = false },
                title = { Text("Remove from library?") },
                text = {
                    Text(
                        "Remove $count selected title${if (count == 1) "" else "s"} from your library?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targetUrls = selectedMangaUrls
                            val selectedMangas = allLibraryItems.filter { it.url in targetUrls }
                            showRemoveConfirmDialog = false
                            selectedMangaUrls = emptySet()
                            isSelectionMode = false
                            coroutineScope.launch {
                                selectedMangas.forEach { manga ->
                                    repository.toggleFavorite(manga, false)
                                }
                                snackbarHostState.showSnackbar(
                                    "Removed ${selectedMangas.size} title${if (selectedMangas.size == 1) "" else "s"} from library"
                                )
                            }
                        }
                    ) {
                        Text("Remove", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 88.dp)
        )
    }
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onMarkRead: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedCount > 0

    Box(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF5111522))
            .border(1.dp, GlassBorderStrong, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatchActionButton(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = "Move",
                enabled = hasSelection,
                onClick = onMove,
                modifier = Modifier.weight(1f)
            )
            BatchActionButton(
                icon = Icons.Default.DoneAll,
                label = "Mark Read",
                enabled = hasSelection,
                onClick = onMarkRead,
                modifier = Modifier.weight(1f)
            )
            // TODO: re-enable batch Download once the blank-page download-integrity bug is fixed
            BatchActionButton(
                icon = Icons.Default.Delete,
                label = "Remove",
                enabled = hasSelection,
                contentColor = Color(0xFFEF4444),
                onClick = onRemove,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BatchActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val color = if (enabled) contentColor else Color.White.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            // Thumbnail with cover artwork or fallback
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
                val imageUrl = manga.thumbnailUrl
                if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))) {
                    AsyncImage(
                        model = rememberCoverImageRequest(imageUrl, widthPx = 220, heightPx = 280),
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = manga.title.take(1),
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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

            // Resume Button Pill - Luxe White with dark charcoal ink
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color.White)
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
                    color = Color(0xFF090D15),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
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
