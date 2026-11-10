package ui.screens.webtoonreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourcePage
import data.mock.PanelRepository
import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import data.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelCircleIconButton
import ui.components.PanelSlider
import ui.glass.GlassVariant
import ui.glass.PanelGlassState
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

data class WebtoonReaderScreen(
    val mangaUrl: String = "manga/the-last-horizon",
    val chapterUrl: String = "ch-210",
    val chapterTitle: String = "Episode 42"
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()

        var currentChapterUrl by remember(chapterUrl) { mutableStateOf(chapterUrl) }
        var currentChapterTitle by remember(chapterTitle) { mutableStateOf(chapterTitle) }

        val chapters by repository.getChapters(mangaUrl).collectAsState(initial = emptyList())
        val pages by repository.getPages(currentChapterUrl).collectAsState(initial = emptyList())

        val currentChapterIndex = chapters.indexOfFirst { it.url == currentChapterUrl || it.name == currentChapterTitle }
        val hasPrevious = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex < chapters.lastIndex
        } else true
        val hasNext = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex > 0
        } else true

        val nextTitle = if (chapters.isNotEmpty() && currentChapterIndex > 0) {
            chapters[currentChapterIndex - 1].name
        } else "Episode 43"

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(currentChapterUrl) {
            repository.recordHistory(currentChapterUrl)
        }

        WebtoonReaderScreenContent(
            chapterTitle = currentChapterTitle,
            pages = pages,
            onBack = { navigator.pop() },
            settingsRepository = settingsRepository,
            onOpenSettings = { navigator.push(ReaderSettingsScreen()) },
            onProgressChange = { page, isCompleted ->
                coroutineScope.launch {
                    repository.updateChapterProgress(currentChapterUrl, page, isCompleted)
                }
            },
            onPreviousChapter = {
                if (chapters.isNotEmpty() && currentChapterIndex in 0 until chapters.lastIndex) {
                    val prevChapter = chapters[currentChapterIndex + 1]
                    currentChapterUrl = prevChapter.url
                    currentChapterTitle = prevChapter.name
                } else {
                    currentChapterTitle = "Episode 41"
                    currentChapterUrl = "ch-209"
                }
            },
            onNextChapter = {
                if (chapters.isNotEmpty() && currentChapterIndex > 0) {
                    val nextChapter = chapters[currentChapterIndex - 1]
                    currentChapterUrl = nextChapter.url
                    currentChapterTitle = nextChapter.name
                } else {
                    currentChapterTitle = "Episode 43"
                    currentChapterUrl = "ch-211"
                }
            },
            hasPreviousChapter = hasPrevious,
            hasNextChapter = hasNext,
            nextChapterTitle = nextTitle
        )
    }
}

@Composable
fun WebtoonReaderScreenContent(
    chapterTitle: String,
    pages: List<SourcePage>,
    onBack: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    onOpenSettings: () -> Unit = {},
    onProgressChange: (page: Int, isCompleted: Boolean) -> Unit = { _, _ -> },
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    hasPreviousChapter: Boolean = true,
    hasNextChapter: Boolean = true,
    nextChapterTitle: String = "Episode 43",
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val repo = settingsRepository ?: remember { FakeSettingsRepository() }
    val readingMode by repo.defaultReadingMode.collectAsState()
    val sidePadding by repo.sidePadding.collectAsState()
    val brightness by repo.brightness.collectAsState()
    val cropBorders by repo.cropBorders.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }

    val displayPages = if (pages.isNotEmpty()) pages else (1..6).map {
        SourcePage(index = it, url = "slice_$it")
    }

    // In paginated modes, pageCount includes all pages plus 1 end-of-chapter transition card
    val totalContentCount = displayPages.size + 1
    val pagerState = rememberPagerState(pageCount = { totalContentCount })

    val isRtl = readingMode == DefaultReadingMode.RIGHT_TO_LEFT

    // Derive scroll progression percentage 0f..1f
    val currentProgress by remember(readingMode, displayPages.size) {
        derivedStateOf {
            if (readingMode == DefaultReadingMode.WEBTOON) {
                val total = displayPages.size
                if (total <= 1) 0f
                else {
                    val index = listState.firstVisibleItemIndex.toFloat()
                    val offset = listState.firstVisibleItemScrollOffset.toFloat() / 1000f
                    ((index + offset) / total.toFloat()).coerceIn(0f, 1f)
                }
            } else {
                val total = displayPages.size
                if (total <= 1) 0f
                else {
                    (pagerState.currentPage.toFloat() / (total - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Current page counter for indicator pill
    val currentPageIndex by remember(readingMode, displayPages.size) {
        derivedStateOf {
            if (readingMode == DefaultReadingMode.WEBTOON) {
                (listState.firstVisibleItemIndex + 1).coerceAtMost(displayPages.size)
            } else {
                (pagerState.currentPage + 1).coerceAtMost(displayPages.size)
            }
        }
    }

    val isAtEndOfChapter by remember(readingMode, displayPages.size) {
        derivedStateOf {
            if (readingMode == DefaultReadingMode.WEBTOON) {
                listState.firstVisibleItemIndex >= displayPages.size
            } else {
                pagerState.currentPage >= displayPages.size
            }
        }
    }

    LaunchedEffect(currentPageIndex, isAtEndOfChapter) {
        onProgressChange(currentPageIndex, isAtEndOfChapter)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        val horizontalPadding = maxWidth * sidePadding.coerceIn(0f, 0.4f)

        val handlePrevious: () -> Unit = {
            coroutineScope.launch {
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(target)
                } else {
                    val prevPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                    pagerState.animateScrollToPage(prevPage)
                }
            }
        }

        val handleNext: () -> Unit = {
            coroutineScope.launch {
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(displayPages.size)
                    listState.animateScrollToItem(target)
                } else {
                    val nextPage = (pagerState.currentPage + 1).coerceAtMost(totalContentCount - 1)
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }

        val handleToggleControls: () -> Unit = {
            areControlsVisible = !areControlsVisible
        }

        // LAYER 1: Artwork Reading Viewport (Multi-Mode Engine with Side Padding)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .glassSource(glassState)
        ) {
            when (readingMode) {
                DefaultReadingMode.WEBTOON -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = false,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) {
                        itemsIndexed(displayPages) { index, page ->
                            val imageUrl = page.imageUrl ?: page.url
                            val isRemote = !imageUrl.isBlank() &&
                                (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))
                            WebtoonPanelSlice(
                                sliceIndex = index,
                                totalSlices = displayPages.size,
                                cropBorders = cropBorders,
                                page = page,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isRemote) Modifier.wrapContentHeight()
                                        else Modifier.height(if (cropBorders) 540.dp else 520.dp)
                                    )
                            )
                        }
                        item {
                            WebtoonEndOfChapterCard(
                                chapterTitle = chapterTitle,
                                nextChapterTitle = nextChapterTitle,
                                hasNextChapter = hasNextChapter,
                                onNextChapter = onNextChapter,
                                glassState = glassState
                            )
                        }
                    }
                }
                DefaultReadingMode.RIGHT_TO_LEFT -> {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = true,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) { pageIndex ->
                        if (pageIndex < displayPages.size) {
                            MangaPageCanvas(
                                pageIndex = pageIndex,
                                totalPages = displayPages.size,
                                chapterTitle = chapterTitle,
                                isRtl = true,
                                cropBorders = cropBorders,
                                page = displayPages.getOrNull(pageIndex),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WebtoonEndOfChapterCard(
                                chapterTitle = chapterTitle,
                                nextChapterTitle = nextChapterTitle,
                                hasNextChapter = hasNextChapter,
                                onNextChapter = onNextChapter,
                                glassState = glassState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                DefaultReadingMode.LEFT_TO_RIGHT -> {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = false,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) { pageIndex ->
                        if (pageIndex < displayPages.size) {
                            MangaPageCanvas(
                                pageIndex = pageIndex,
                                totalPages = displayPages.size,
                                chapterTitle = chapterTitle,
                                isRtl = false,
                                cropBorders = cropBorders,
                                page = displayPages.getOrNull(pageIndex),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WebtoonEndOfChapterCard(
                                chapterTitle = chapterTitle,
                                nextChapterTitle = nextChapterTitle,
                                hasNextChapter = hasNextChapter,
                                onNextChapter = onNextChapter,
                                glassState = glassState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                DefaultReadingMode.VERTICAL -> {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = false,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) { pageIndex ->
                        if (pageIndex < displayPages.size) {
                            WebtoonPanelSlice(
                                sliceIndex = pageIndex,
                                totalSlices = displayPages.size,
                                cropBorders = cropBorders,
                                page = displayPages.getOrNull(pageIndex),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WebtoonEndOfChapterCard(
                                chapterTitle = chapterTitle,
                                nextChapterTitle = nextChapterTitle,
                                hasNextChapter = hasNextChapter,
                                onNextChapter = onNextChapter,
                                glassState = glassState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // LAYER 1.5: In-App Brightness Dimmer Scrim Overlay (10%..100% -> 0.0..0.9 alpha black)
        val dimmerAlpha = (1f - brightness).coerceIn(0f, 0.9f)
        if (dimmerAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimmerAlpha))
            )
        }

        // Floating Page Indicator Pill
        if (!isAtEndOfChapter) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (areControlsVisible) 96.dp else 24.dp)
                    .glassDimming(alpha = 0.35f, shape = CapsuleShape)
                    .glassSurface(
                        state = glassState,
                        variant = GlassVariant.Clear,
                        shape = CapsuleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$currentPageIndex / ${displayPages.size}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // LAYER 2: Top Floating Control Bar - Sibling overlay using Clear Glass with 35% dimming over artwork
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Back button, Chapter Title, Bookmark & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        size = 42.dp,
                        iconSize = 20.dp,
                        glassState = null,
                        backgroundColor = Color(0xF50F1219),
                        borderColor = GlassBorderStrong,
                        onClick = onBack
                    )

                    // Chapter Title Capsule
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(CapsuleShape)
                            .background(Color(0xF50F1219))
                            .border(1.dp, GlassBorderStrong, CapsuleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chapterTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }

                    // Bookmark & Settings Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelCircleIconButton(
                            icon = Icons.Default.Tune,
                            size = 42.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            onClick = onOpenSettings
                        )
                        PanelCircleIconButton(
                            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            size = 42.dp,
                            iconSize = 20.dp,
                            tint = if (isBookmarked) ElectricBlue else Color.White,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            onClick = { isBookmarked = !isBookmarked }
                        )
                        PanelCircleIconButton(
                            icon = Icons.Default.MoreVert,
                            size = 42.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            onClick = { /* Menu */ }
                        )
                    }
                }

                // In-Reader Liquid Glass Mode Switcher [ RTL Manga | LTR Comic | Webtoon ]
                ReaderModeSwitcher(
                    currentMode = readingMode,
                    onSelectMode = { newMode ->
                        repo.setDefaultReadingMode(newMode)
                    },
                    glassState = glassState
                )
            }
        }

        // LAYER 2.5: Educational 3-Zone Tap Hints (Subtle visual guidance when controls are shown)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Zone
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isRtl) "◀ Next (RTL)" else "◀ Prev",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Center Zone
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "◉ Menu",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right Zone
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isRtl) "Prev (RTL) ▶" else "Next ▶",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // LAYER 3: Bottom Floating Scrubber Capsule Bar with Page Indicator & Step Buttons
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF50F1219))
                    .border(1.dp, GlassBorderStrong, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Row 1: Direction-aware page indicator and step buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step previous page
                        Row(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .clickable { handlePrevious() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Page",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Prev", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                        }

                        // Page Indicator Badge
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(0.5.dp, GlassBorderStrong, CapsuleShape)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            val pageText = if (readingMode == DefaultReadingMode.WEBTOON) {
                                "${(currentProgress * 100).toInt()}% • Continuous"
                            } else {
                                val curPage = (pagerState.currentPage + 1).coerceAtMost(displayPages.size)
                                "Page $curPage / ${displayPages.size} • ${if (isRtl) "RTL Manga" else "LTR Comic"}"
                            }
                            Text(
                                text = pageText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Step next page
                        Row(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .clickable { handleNext() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Next", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Page",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Chapter Prev | Scrubber Slider | Chapter Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Chapter button
                        PanelCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.NavigateBefore,
                            size = 36.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            tint = if (hasPreviousChapter) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                            onClick = { if (hasPreviousChapter) onPreviousChapter() }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Reading progress slider
                        PanelSlider(
                            value = currentProgress,
                            onValueChange = { targetFraction ->
                                coroutineScope.launch {
                                    if (readingMode == DefaultReadingMode.WEBTOON) {
                                        val targetIndex = (targetFraction * (displayPages.size - 1)).toInt().coerceIn(0, displayPages.size - 1)
                                        listState.scrollToItem(targetIndex)
                                    } else {
                                        val targetPage = (targetFraction * (displayPages.size - 1)).toInt().coerceIn(0, displayPages.size - 1)
                                        pagerState.scrollToPage(targetPage)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Next Chapter button
                        PanelCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.NavigateNext,
                            size = 36.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            tint = if (hasNextChapter) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                            onClick = { if (hasNextChapter) onNextChapter() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * In-Reader Mode Switcher segmented control allowing users to switch reading modes instantly.
 */
@Composable
private fun ReaderModeSwitcher(
    currentMode: DefaultReadingMode,
    onSelectMode: (DefaultReadingMode) -> Unit,
    glassState: PanelGlassState,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(DefaultReadingMode.RIGHT_TO_LEFT, "◂ RTL Manga", "Japanese Right-to-Left paging"),
        Triple(DefaultReadingMode.LEFT_TO_RIGHT, "LTR Comic ▸", "Western Left-to-Right paging"),
        Triple(DefaultReadingMode.WEBTOON, "▾ Webtoon", "Continuous vertical infinite scroll")
    )

    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .background(Color(0xEE0F1219))
            .border(1.dp, GlassBorderStrong, CapsuleShape)
            .padding(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { (mode, label, _) ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF090D15) else Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

/**
 * End-of-Chapter transition glass card presented after the last page of a chapter.
 */
@Composable
fun WebtoonEndOfChapterCard(
    chapterTitle: String,
    nextChapterTitle: String,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    glassState: PanelGlassState,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val buttonInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .glassDimming(alpha = 0.35f, shape = cardShape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = cardShape
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(ElectricBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CHAPTER COMPLETE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$chapterTitle Finished",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasNextChapter) "Up Next: $nextChapterTitle" else "You have caught up to the latest chapter",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (hasNextChapter) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .clip(CapsuleShape)
                        .glassPress(buttonInteraction)
                        .background(ElectricBlue)
                        .clickable(
                            interactionSource = buttonInteraction,
                            indication = null,
                            onClick = onNextChapter
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Read Next Chapter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Procedural stylized manhwa webtoon panel slice depicting a scenic vertical scene
 * with cherry blossom petals, dusk gradients, and torii/shrine silhouettes matching option 2.png.
 */
@Composable
fun WebtoonPanelSlice(
    sliceIndex: Int,
    totalSlices: Int,
    cropBorders: Boolean = false,
    page: SourcePage? = null,
    modifier: Modifier = Modifier
) {
    val imageUrl = page?.imageUrl ?: page?.url
    val isRemoteImage = !imageUrl.isNullOrBlank() &&
        (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))

    if (isRemoteImage) {
        val platformContext = LocalPlatformContext.current
        val imageRequest = remember(imageUrl, page?.headers) {
            ImageRequest.Builder(platformContext)
                .data(imageUrl)
                .apply {
                    val headers = page?.headers
                    if (!headers.isNullOrEmpty()) {
                        val networkHeaders = NetworkHeaders.Builder().apply {
                            headers.forEach { (k, v) -> add(k, v) }
                        }.build()
                        httpHeaders(networkHeaders)
                    }
                }
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = "Slice ${sliceIndex + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Coordinated color palette across vertical slices to ensure seamless stitching
        val (topColor, bottomColor) = when (sliceIndex % 4) {
            0 -> Color(0xFF1E152D) to Color(0xFF351A3B) // Purple dusk
            1 -> Color(0xFF351A3B) to Color(0xFF5A2548) // Blossom magenta
            2 -> Color(0xFF5A2548) to Color(0xFF28183B) // Deep rose to navy
            else -> Color(0xFF28183B) to Color(0xFF1E152D) // Navy return
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor),
                startY = 0f,
                endY = height
            )
        )

        // Torii Gate / Architectural Silhouette in slice 0 and 1
        if (sliceIndex % 4 == 0 || sliceIndex % 4 == 1) {
            val gateColor = Color(0xFF0F0915).copy(alpha = 0.85f)
            val leftPillarInset = if (cropBorders) 0.08f else 0.12f
            val rightPillarInset = if (cropBorders) 0.86f else 0.82f
            // Left pillar
            drawRect(
                color = gateColor,
                topLeft = Offset(width * leftPillarInset, 0f),
                size = androidx.compose.ui.geometry.Size(width * 0.06f, height)
            )
            // Right pillar
            drawRect(
                color = gateColor,
                topLeft = Offset(width * rightPillarInset, 0f),
                size = androidx.compose.ui.geometry.Size(width * 0.06f, height)
            )
            // Top beam
            if (sliceIndex % 4 == 0) {
                drawRect(
                    color = gateColor,
                    topLeft = Offset(width * 0.04f, height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(width * 0.92f, 24f)
                )
            }
        }

        // Falling Cherry Blossom Sakura Petals
        val petalOffsets = listOf(
            Offset(width * 0.24f, height * 0.15f) to 14f,
            Offset(width * 0.78f, height * 0.28f) to 18f,
            Offset(width * 0.45f, height * 0.42f) to 12f,
            Offset(width * 0.18f, height * 0.65f) to 16f,
            Offset(width * 0.88f, height * 0.72f) to 20f,
            Offset(width * 0.58f, height * 0.85f) to 13f,
            Offset(width * 0.32f, height * 0.92f) to 15f
        )

        for ((offset, radius) in petalOffsets) {
            val petalColor = Color(0xFFF472B6).copy(alpha = 0.55f)
            drawCircle(
                color = petalColor,
                radius = radius,
                center = offset
            )
        }

        // Hero character silhouette in slice 1 & 2
        if (sliceIndex % 4 == 1) {
            val charCenter = Offset(width * 0.5f, height * 0.75f)
            // Head
            drawCircle(
                color = Color(0xFF0A0712),
                radius = 36f,
                center = Offset(charCenter.x, charCenter.y - 45f)
            )
            // Hair flow
            val hairPath = Path().apply {
                moveTo(charCenter.x - 36f, charCenter.y - 65f)
                lineTo(charCenter.x + 36f, charCenter.y - 65f)
                lineTo(charCenter.x + 52f, charCenter.y + 40f)
                lineTo(charCenter.x - 20f, charCenter.y + 20f)
                close()
            }
            drawPath(hairPath, Color(0xFF08060E))
        }

        // Page watermark indicator
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 16f,
            center = Offset(width - 32f, height - 32f)
        )
    }
}
}

@Preview
@Composable
fun WebtoonReaderScreenPreview() {
    PanelTheme {
        WebtoonReaderScreenContent(
            chapterTitle = "Episode 42",
            pages = emptyList(),
            onBack = {}
        )
    }
}
