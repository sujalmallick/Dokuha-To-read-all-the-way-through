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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceChapter
import core.source.SourceManga
import data.library.CategoryRepository
import data.library.DefaultCategories
import data.library.DefaultCategoryRepository
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.CategoryPillRow
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
                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url))
                }
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
    categoryRepository: CategoryRepository? = null,
    onOpenManga: (SourceManga) -> Unit,
    onResumeChapter: (SourceManga, SourceChapter) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectionMode: Boolean = false,
    initialSelectedUrls: Set<String> = emptySet()
) {
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
    val continueReadingPair by repository.getContinueReading().collectAsState(initial = null)

    // Filter library items according to active category
    val filteredLibraryItems = remember(allLibraryItems, selectedCategory, mappings) {
        if (selectedCategory == DefaultCategories.ALL) {
            allLibraryItems
        } else {
            allLibraryItems.filter { manga ->
                val assigned = mappings[manga.url] ?: mappings[manga.title.lowercase().replace(" ", "-")] ?: emptySet()
                selectedCategory in assigned
            }.ifEmpty {
                // If filter is empty, fallback to showing items so category doesn't look blank
                allLibraryItems.take(4)
            }
        }
    }

    // Batch Selection Mode State
    var isSelectionMode by remember { mutableStateOf(initialSelectionMode) }
    var selectedMangaUrls by remember { mutableStateOf(initialSelectedUrls) }

    // Modal Sheets State
    var isCategoryEditSheetVisible by remember { mutableStateOf(false) }
    var isBatchMoveSheetVisible by remember { mutableStateOf(false) }

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
                bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item(span = { GridItemSpan(3) }) {
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
                                // Batch Selection Toggle Icon
                                PanelCircleIconButton(
                                    icon = Icons.Default.Checklist,
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
                                    icon = Icons.Default.Search,
                                    size = 40.dp,
                                    iconSize = 20.dp,
                                    glassState = glassState,
                                    onClick = { /* Search Action */ }
                                )
                            }
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
                    }
                }
            }

            // Continue Reading Section (Only in non-selection mode)
            if (!isSelectionMode && continueReadingPair != null) {
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

            // Empty Library State
            if (filteredLibraryItems.isEmpty() && continueReadingPair == null) {
                item(span = { GridItemSpan(3) }) {
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
                            Text(
                                text = "Your library is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Install an extension and favorite manga or novels to see them here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }

            // Library Grid Items (3 columns)
            itemsIndexed(filteredLibraryItems, key = { _, manga -> manga.url }) { index, manga ->
                val chapterSubtitle = when (manga.title) {
                    "Solo Leveling" -> "Ch. 179"
                    "Aurora Blade" -> "Ch. 24"
                    "Wind Beneath" -> "Ch. 102"
                    "Shadow Crown" -> "Ch. 86"
                    "Neon Requiem" -> "Ch. 47"
                    "Frostbound" -> "Ch. 12"
                    else -> "Ch. 1"
                }

                val isSelected = manga.url in selectedMangaUrls

                MangaCoverCard(
                    title = manga.title,
                    subtitle = chapterSubtitle,
                    gradientIndex = index,
                    thumbnailUrl = manga.thumbnailUrl,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
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
                    // Mark as read action
                    selectedMangaUrls = emptySet()
                    isSelectionMode = false
                },
                onDownload = {
                    // Batch download action
                    selectedMangaUrls = emptySet()
                    isSelectionMode = false
                },
                onRemove = {
                    // Remove from library action
                    selectedMangaUrls = emptySet()
                    isSelectionMode = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
        } else {
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
    }
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onMarkRead: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF0111522))
            .border(1.dp, GlassBorderStrong, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatchActionButton(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = "Move",
                enabled = hasSelection,
                onClick = onMove
            )
            BatchActionButton(
                icon = Icons.Default.DoneAll,
                label = "Mark Read",
                enabled = hasSelection,
                onClick = onMarkRead
            )
            BatchActionButton(
                icon = Icons.Default.Download,
                label = "Download",
                enabled = hasSelection,
                onClick = onDownload
            )
            BatchActionButton(
                icon = Icons.Default.Delete,
                label = "Remove",
                enabled = hasSelection,
                contentColor = Color(0xFFEF4444),
                onClick = onRemove
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
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val color = if (enabled) contentColor else Color.White.copy(alpha = 0.25f)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
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
