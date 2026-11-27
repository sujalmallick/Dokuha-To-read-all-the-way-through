package ui.screens.webtoonreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import core.extensions.js.CloudflareChallengeHeuristics
import core.logging.AppCoroutineExceptionHandler
import core.logging.AppLogger
import core.logging.launchSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.reader_next_chapter
import panelreader.composeapp.generated.resources.reader_previous_chapter
import kotlin.math.roundToInt
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourcePage
import core.source.SourceManga
import ui.volume.LocalVolumeKeyState
import core.download.DownloadManager
import data.mock.PanelRepository
import data.reader.DualPageEngine
import data.reader.DualPageSpread
import data.reader.ReaderColorFilters
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.ReaderPrefsRepository
import data.settings.SettingsRepository
import data.settings.canonicalTitleKey
import data.settings.observeResolvedMangaSettings
import data.settings.resolveReaderSettings
import kotlinx.coroutines.flow.flowOf
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
    val chapterTitle: String = "Episode 42",
    val sourceId: String? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()
        val readerPrefsRepository: ReaderPrefsRepository = koinInject()
        val downloadManager: DownloadManager = koinInject()
        val volumeKeyState = ui.volume.LocalVolumeKeyState.current

        androidx.compose.runtime.DisposableEffect(volumeKeyState) {
            volumeKeyState.isReaderActive.value = true
            onDispose { volumeKeyState.isReaderActive.value = false }
        }

        var currentChapterUrl by rememberSaveable(chapterUrl) { mutableStateOf(chapterUrl) }
        var currentChapterTitle by rememberSaveable(chapterTitle) { mutableStateOf(chapterTitle) }
        var pagesRefreshTrigger by remember { mutableIntStateOf(0) }

        val mangaDetails by remember(mangaUrl, sourceId) {
            repository.getMangaDetails(mangaUrl, sourceId)
        }.collectAsState(initial = null)
        val chapters by repository.getChapters(mangaUrl, sourceId).collectAsState(initial = emptyList())
        val pages by remember(currentChapterUrl, sourceId, pagesRefreshTrigger) {
            repository.getPages(currentChapterUrl, sourceId)
        }.collectAsState(initial = emptyList())
        val isDownloaded = remember(currentChapterUrl) {
            downloadManager.isChapterDownloaded(mangaUrl, currentChapterUrl)
        }

        val currentChapterIndex = chapters.indexOfFirst { it.url == currentChapterUrl || it.name == currentChapterTitle }
        val hasPrevious = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex < chapters.lastIndex
        } else false
        val hasNext = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex > 0
        } else false

        val nextTitle = if (chapters.isNotEmpty() && currentChapterIndex > 0) {
            chapters[currentChapterIndex - 1].name
        } else ""

        val coroutineScope = rememberCoroutineScope { AppCoroutineExceptionHandler }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(currentChapterUrl) {
            try {
                repository.recordHistory(currentChapterUrl)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLogger.e("WebtoonReaderScreen", "recordHistory failed (${t::class.simpleName ?: "Error"})")
                snackbarHostState.showSnackbar("Could not record reading history.")
            }
        }

        var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
        val resolvedTitleKey = remember(mangaDetails?.title, mangaUrl) {
            canonicalTitleKey(mangaDetails?.title ?: mangaUrl, isNovel = false)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            WebtoonReaderScreenContent(
                chapterUrl = currentChapterUrl,
                chapterTitle = currentChapterTitle,
                pages = pages,
                onBack = { navigator.pop() },
                settingsRepository = settingsRepository,
                titleKey = resolvedTitleKey,
                readerPrefsRepository = readerPrefsRepository,
                onOpenSettings = { isSettingsOpen = true },
                isOfflineCached = isDownloaded,
                onProgressChange = { page, isCompleted ->
                    coroutineScope.launchSafe(
                        tag = "WebtoonReaderScreen",
                        onError = {
                            coroutineScope.launchSafe("WebtoonReaderScreen", onError = {}) {
                                snackbarHostState.showSnackbar("Could not save reading progress.")
                            }
                        }
                    ) {
                        repository.updateChapterProgress(currentChapterUrl, page, isCompleted)

                        if (settingsRepository.autoDownloadNextChapter.value) {
                            val totalPages = pages.size.coerceAtLeast(1)
                            val progressFraction = page.toFloat() / totalPages
                            if (progressFraction >= 0.8f || isCompleted) {
                                if (chapters.isNotEmpty() && currentChapterIndex > 0) {
                                    val nextChapter = chapters[currentChapterIndex - 1]
                                    val isNextDownloaded = downloadManager.isChapterDownloaded(mangaUrl, nextChapter.url)
                                    val nextStatus = downloadManager.getChapterStatus(mangaUrl, nextChapter.url)
                                    if (!isNextDownloaded && nextStatus == null) {
                                        val resolvedSid = sourceId?.toLongOrNull()
                                            ?: mangaDetails?.sourceId?.takeIf { it != 0L }
                                            ?: repository.resolveSource(sourceId, mangaUrl, nextChapter.url)?.id
                                            ?: 0L
                                        val baseManga = mangaDetails ?: SourceManga(
                                            url = mangaUrl,
                                            title = currentChapterTitle
                                        )
                                        val targetManga = baseManga.copy(sourceId = resolvedSid)
                                        downloadManager.enqueue(targetManga, listOf(nextChapter))
                                    }
                                }
                            }
                        }
                    }
                },
                onPreviousChapter = {
                    if (chapters.isNotEmpty() && currentChapterIndex in 0 until chapters.lastIndex) {
                        val prevChapter = chapters[currentChapterIndex + 1]
                        currentChapterUrl = prevChapter.url
                        currentChapterTitle = prevChapter.name
                    }
                },
                onNextChapter = {
                    if (chapters.isNotEmpty() && currentChapterIndex > 0) {
                        val nextChapter = chapters[currentChapterIndex - 1]
                        currentChapterUrl = nextChapter.url
                        currentChapterTitle = nextChapter.name
                    }
                },
                hasPreviousChapter = hasPrevious,
                hasNextChapter = hasNext,
                nextChapterTitle = nextTitle
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        if (isSettingsOpen) {
            ReaderSettingsBottomSheet(
                onDismiss = { isSettingsOpen = false },
                settingsRepository = settingsRepository,
                titleKey = resolvedTitleKey,
                readerPrefsRepository = readerPrefsRepository
            )
        }
    }
}

@Composable
fun WebtoonReaderScreenContent(
    chapterUrl: String = "",
    chapterTitle: String,
    pages: List<SourcePage>,
    onBack: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    onOpenSettings: () -> Unit = {},
    isOfflineCached: Boolean = false,
    onProgressChange: (page: Int, isCompleted: Boolean) -> Unit = { _, _ -> },
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    hasPreviousChapter: Boolean = true,
    hasNextChapter: Boolean = true,
    nextChapterTitle: String = "",
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val repo = settingsRepository ?: remember { FakeSettingsRepository() }
    val prefsRepo = readerPrefsRepository ?: remember { FakeReaderPrefsRepository(repo) }
    val resolvedSettings by remember(titleKey, repo, prefsRepo) {
        val overrideFlow = if (titleKey != null) prefsRepo.observePrefs(titleKey) else flowOf(null)
        observeResolvedMangaSettings(titleKey ?: "", repo, overrideFlow)
    }.collectAsState(initial = resolveReaderSettings(titleKey ?: "", repo, null))

    val readingMode = resolvedSettings.readingMode
    val sidePadding = resolvedSettings.sidePadding
    val brightness = resolvedSettings.brightness
    val cropBorders by repo.cropBorders.collectAsState()
    val splitDoubleSpread = resolvedSettings.splitDoubleSpread
    val dualPageOffset = resolvedSettings.dualPageOffset
    val volumeKeyNavEnabled = resolvedSettings.volumeKeyNavigation
    val readerColorFilter = resolvedSettings.colorFilter
    val readerPreloadPages = resolvedSettings.readerPreloadPages
    val tapZonesEnabled by repo.readerTapZonesEnabled.collectAsState()
    val cutoutPadding by repo.readerCutoutPadding.collectAsState()

    // Volume key navigation: scroll one page up/down when enabled
    val volumeKeyState = LocalVolumeKeyState.current
    val volumeUpCount = volumeKeyState.upPresses.value
    val volumeDownCount = volumeKeyState.downPresses.value
    LaunchedEffect(volumeUpCount) {
        if (volumeKeyNavEnabled && volumeUpCount > 0) {
            val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }
    LaunchedEffect(volumeDownCount) {
        if (volumeKeyNavEnabled && volumeDownCount > 0) {
            val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(pages.size - 1)
            listState.animateScrollToItem(target)
        }
    }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }

    var isAutoscrollActive by remember { mutableStateOf(false) }
    var autoscrollSpeed by remember { mutableFloatStateOf(1f) }
    var isAutoscrollPausedByTouch by remember { mutableStateOf(false) }

    // Clean up autoscroll when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            isAutoscrollActive = false
        }
    }

    // Touch-to-pause: If user drags or flings, pause autoscroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isAutoscrollActive && !isAutoscrollPausedByTouch) {
            isAutoscrollPausedByTouch = true
        }
    }

    // Autoscroll continuous frame pump
    LaunchedEffect(isAutoscrollActive, isAutoscrollPausedByTouch, autoscrollSpeed, hasNextChapter) {
        if (!isAutoscrollActive || isAutoscrollPausedByTouch) return@LaunchedEffect
        val baseSpeedPx = 36f * autoscrollSpeed
        var lastTime = 0L
        while (isActive && isAutoscrollActive && !isAutoscrollPausedByTouch) {
            withFrameNanos { now ->
                if (lastTime > 0L) {
                    val dt = (now - lastTime) / 1_000_000_000f
                    val delta = baseSpeedPx * dt
                    if (listState.canScrollForward) {
                        listState.dispatchRawDelta(delta)
                    } else if (hasNextChapter) {
                        onNextChapter()
                    } else {
                        isAutoscrollActive = false
                    }
                }
                lastTime = now
            }
        }
    }

    val displayPages = pages

    val isRtl = readingMode == DefaultReadingMode.RIGHT_TO_LEFT

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        val isLandscapeOrTablet = maxWidth >= 600.dp || (maxWidth > maxHeight && maxWidth >= 480.dp)
        val isDualSpreadActive = isLandscapeOrTablet && splitDoubleSpread && (readingMode == DefaultReadingMode.RIGHT_TO_LEFT || readingMode == DefaultReadingMode.LEFT_TO_RIGHT)

        val spreads = remember(displayPages.size, readingMode, dualPageOffset) {
            DualPageEngine.calculateSpreads(
                pageCount = displayPages.size,
                isRtl = readingMode == DefaultReadingMode.RIGHT_TO_LEFT,
                offsetCover = dualPageOffset
            )
        }

        val totalContentCount = if (isDualSpreadActive) spreads.size + 1 else displayPages.size + 1
        val pagerState = rememberPagerState(pageCount = { totalContentCount })
        var isPageZoomed by remember(chapterUrl, pagerState.currentPage) { mutableStateOf(false) }

        // Persist and restore scroll/pager position across process death; only reset to 0 when chapter changes
        var previousChapterUrl by rememberSaveable { mutableStateOf(chapterUrl) }
        var savedPageIndex by rememberSaveable(chapterUrl) { mutableIntStateOf(0) }
        var savedScrollOffset by rememberSaveable(chapterUrl) { mutableIntStateOf(0) }
        var hasRestoredSavedPosition by remember(chapterUrl) { mutableStateOf(false) }

        LaunchedEffect(chapterUrl, displayPages.size) {
            if (shouldResetReaderScroll(previousChapterUrl, chapterUrl)) {
                previousChapterUrl = chapterUrl
                savedPageIndex = 0
                savedScrollOffset = 0
                hasRestoredSavedPosition = true
                listState.scrollToItem(0)
                pagerState.scrollToPage(0)
            } else if (!hasRestoredSavedPosition && displayPages.isNotEmpty()) {
                val targetIndex = savedPageIndex.coerceIn(0, displayPages.lastIndex)
                if (targetIndex > 0 || savedScrollOffset > 0) {
                    listState.scrollToItem(targetIndex, savedScrollOffset.coerceAtLeast(0))
                    pagerState.scrollToPage(targetIndex)
                }
                hasRestoredSavedPosition = true
            }
        }

        LaunchedEffect(
            hasRestoredSavedPosition,
            displayPages.size,
            readingMode,
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
            pagerState.currentPage
        ) {
            if (hasRestoredSavedPosition && displayPages.isNotEmpty()) {
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    savedPageIndex = listState.firstVisibleItemIndex.coerceIn(0, displayPages.lastIndex)
                    savedScrollOffset = listState.firstVisibleItemScrollOffset.coerceAtLeast(0)
                } else {
                    savedPageIndex = pagerState.currentPage.coerceIn(0, displayPages.lastIndex)
                    savedScrollOffset = 0
                }
            }
        }

        // Guard so downward/upward over-scroll at chapter edges only triggers once per chapter
        var hasTriggeredAutoNext by remember(chapterUrl) { mutableStateOf(false) }
        var hasTriggeredAutoPrev by remember(chapterUrl) { mutableStateOf(false) }
        val autoNextNestedScroll = remember(chapterUrl, hasNextChapter, hasPreviousChapter, displayPages.size) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (shouldAutoAdvanceOnBottomScroll(
                            hasNextChapter = hasNextChapter,
                            alreadyTriggered = hasTriggeredAutoNext,
                            canScrollForward = listState.canScrollForward,
                            downwardScrollDeltaY = available.y,
                            isEmptyPages = displayPages.isEmpty()
                        )
                    ) {
                        hasTriggeredAutoNext = true
                        onNextChapter()
                    } else if (
                        hasPreviousChapter &&
                        !hasTriggeredAutoPrev &&
                        displayPages.isNotEmpty() &&
                        !listState.canScrollBackward &&
                        available.y > 48f
                    ) {
                        hasTriggeredAutoPrev = true
                        onPreviousChapter()
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (shouldAutoAdvanceOnBottomScroll(
                            hasNextChapter = hasNextChapter,
                            alreadyTriggered = hasTriggeredAutoNext,
                            canScrollForward = listState.canScrollForward,
                            downwardScrollDeltaY = available.y,
                            isEmptyPages = displayPages.isEmpty()
                        )
                    ) {
                        hasTriggeredAutoNext = true
                        onNextChapter()
                    }
                    return Velocity.Zero
                }
            }
        }

        // Derive scroll progression percentage 0f..1f
        val currentProgress by remember(readingMode, displayPages.size, isDualSpreadActive, spreads.size) {
            derivedStateOf {
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    val total = displayPages.size
                    if (total <= 1) 0f
                    else {
                        val index = listState.firstVisibleItemIndex.toFloat()
                        val offset = listState.firstVisibleItemScrollOffset.toFloat() / 1000f
                        ((index + offset) / total.toFloat()).coerceIn(0f, 1f)
                    }
                } else if (isDualSpreadActive) {
                    val total = spreads.size
                    if (total <= 1) 0f
                    else (pagerState.currentPage.toFloat() / (total - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
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

        val currentDisplayPageLabel by remember(isDualSpreadActive, pagerState.currentPage, spreads, displayPages.size, currentPageIndex) {
            derivedStateOf {
                if (isDualSpreadActive && pagerState.currentPage < spreads.size) {
                    DualPageEngine.getSpreadPageRangeLabel(spreads[pagerState.currentPage], displayPages.size)
                } else {
                    "$currentPageIndex / ${displayPages.size}"
                }
            }
        }

        val isAtEndOfChapter by remember(readingMode, displayPages.size, isDualSpreadActive, spreads.size) {
            derivedStateOf {
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    listState.firstVisibleItemIndex >= displayPages.size
                } else if (isDualSpreadActive) {
                    pagerState.currentPage >= spreads.size
                } else {
                    pagerState.currentPage >= displayPages.size
                }
            }
        }

        LaunchedEffect(currentPageIndex, isAtEndOfChapter) {
            onProgressChange(currentPageIndex, isAtEndOfChapter)
        }

        // Smart Background Preloader: Proactively buffers ahead next pages into cache
        val platformContext = LocalPlatformContext.current
        val defaultReaderUa = remember { CloudflareChallengeHeuristics.defaultReaderUserAgent() }
        LaunchedEffect(currentPageIndex, displayPages, readerPreloadPages, defaultReaderUa) {
            if (displayPages.isEmpty() || readerPreloadPages <= 0) return@LaunchedEffect
            val imageLoader = coil3.SingletonImageLoader.get(platformContext)
            val start = currentPageIndex.coerceAtLeast(0)
            val end = (start + readerPreloadPages).coerceAtMost(displayPages.size)
            for (i in start until end) {
                val p = displayPages[i]
                val url = p.imageUrl ?: p.url
                if (!url.isNullOrBlank() && (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true))) {
                    val request = buildReaderImageRequest(
                        context = platformContext,
                        imageUrl = url,
                        headers = p.headers,
                        defaultUserAgent = defaultReaderUa
                    )
                    imageLoader.enqueue(request)
                }
            }
        }

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
                    val maxLimit = if (isDualSpreadActive) spreads.size else displayPages.size
                    val nextPage = (pagerState.currentPage + 1).coerceAtMost(maxLimit)
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }

        val handleToggleControls: () -> Unit = {
            if (isAutoscrollActive) {
                isAutoscrollPausedByTouch = !isAutoscrollPausedByTouch
            } else {
                areControlsVisible = !areControlsVisible
            }
        }

        // LAYER 1: Artwork Reading Viewport (Multi-Mode Engine with Side Padding & optional Cutout Guard)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (cutoutPadding) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
                .padding(horizontal = horizontalPadding)
                .glassSource(glassState)
        ) {
            if (displayPages.isEmpty()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                val isExternal = chapterUrl.startsWith("http://", ignoreCase = true) || chapterUrl.startsWith("https://", ignoreCase = true)
                var retryAttempt by remember(chapterUrl) { mutableIntStateOf(0) }
                var loadingTimedOut by remember(chapterUrl, retryAttempt) { mutableStateOf(false) }

                LaunchedEffect(chapterUrl, retryAttempt, displayPages.isEmpty()) {
                    if (!isExternal && displayPages.isEmpty()) {
                        kotlinx.coroutines.delay(10_000L)
                        if (displayPages.isEmpty()) {
                            loadingTimedOut = true
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExternal) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Official External Chapter",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This chapter is officially hosted externally.\nTap below to open and read on the official site.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            ui.components.PanelButton(
                                text = "Open in Browser",
                                icon = Icons.Default.PlayArrow,
                                onClick = { uriHandler.openUri(chapterUrl) }
                            )
                        }
                    } else if (loadingTimedOut) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Unable to load chapter pages",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The source returned no pages or timed out. Check your connection or try another mirror.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ui.components.PanelButton(
                                    text = "Go Back",
                                    variant = ui.components.ButtonVariant.OUTLINE,
                                    onClick = onBack
                                )
                                ui.components.PanelButton(
                                    text = "Retry",
                                    onClick = {
                                        loadingTimedOut = false
                                        retryAttempt++
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading chapter pages...",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                when (readingMode) {
                DefaultReadingMode.WEBTOON -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = 760.dp)
                                .fillMaxSize()
                                .nestedScroll(autoNextNestedScroll)
                                .readerTapHandler(
                                    isRtl = false,
                                    tapZonesEnabled = tapZonesEnabled,
                                    onPrevious = handlePrevious,
                                    onToggleControls = handleToggleControls,
                                    onNext = handleNext
                                )
                        ) {
                            itemsIndexed(
                                items = displayPages,
                                key = { index, page -> "${index}_${page.url}" }
                            ) { index, page ->
                                val imageUrl = page.imageUrl ?: page.url
                                val hasValidImage = !imageUrl.isBlank() && (
                                    imageUrl.startsWith("http://", ignoreCase = true) ||
                                    imageUrl.startsWith("https://", ignoreCase = true) ||
                                    imageUrl.startsWith("file:", ignoreCase = true) ||
                                    imageUrl.startsWith("/")
                                )
                                WebtoonPanelSlice(
                                    sliceIndex = index,
                                    totalSlices = displayPages.size,
                                    cropBorders = cropBorders,
                                    page = page,
                                    colorFilter = readerColorFilter,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (hasValidImage) Modifier.wrapContentHeight()
                                            else Modifier.height(if (cropBorders) 540.dp else 520.dp)
                                        )
                                )
                            }
                            if (hasNextChapter) {
                                item(key = "seamless_gap_${chapterTitle}") {
                                    WebtoonChapterTransitionGap(
                                        currentChapterTitle = chapterTitle,
                                        nextChapterTitle = nextChapterTitle,
                                        onContinueToNextChapter = onNextChapter,
                                        glassState = glassState
                                    )
                                }
                            } else {
                                item(key = "end_of_series_${chapterTitle}") {
                                    WebtoonEndOfSeriesCard(
                                        chapterTitle = chapterTitle,
                                        onBack = onBack,
                                        glassState = glassState
                                    )
                                }
                            }
                        }

                        WebtoonVerticalScrollbar(
                            listState = listState,
                            isControlsVisible = areControlsVisible,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(end = 4.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
                DefaultReadingMode.RIGHT_TO_LEFT -> {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = true,
                        userScrollEnabled = !isPageZoomed,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = true,
                                tapZonesEnabled = tapZonesEnabled,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) { pageIndex ->
                        if (isDualSpreadActive) {
                            if (pageIndex < spreads.size) {
                                DualPageSpreadCanvas(
                                    spread = spreads[pageIndex],
                                    totalPages = displayPages.size,
                                    chapterTitle = chapterTitle,
                                    isRtl = true,
                                    cropBorders = cropBorders,
                                    colorFilter = readerColorFilter,
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
                        } else {
                            if (pageIndex < displayPages.size) {
                                MangaPageCanvas(
                                    pageIndex = pageIndex,
                                    totalPages = displayPages.size,
                                    chapterTitle = chapterTitle,
                                    isRtl = true,
                                    cropBorders = cropBorders,
                                    page = displayPages.getOrNull(pageIndex),
                                    colorFilter = readerColorFilter,
                                    onZoomStateChanged = { isPageZoomed = it },
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
                DefaultReadingMode.LEFT_TO_RIGHT -> {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = false,
                        userScrollEnabled = !isPageZoomed,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = false,
                                tapZonesEnabled = tapZonesEnabled,
                                onPrevious = handlePrevious,
                                onToggleControls = handleToggleControls,
                                onNext = handleNext
                            )
                    ) { pageIndex ->
                        if (isDualSpreadActive) {
                            if (pageIndex < spreads.size) {
                                DualPageSpreadCanvas(
                                    spread = spreads[pageIndex],
                                    totalPages = displayPages.size,
                                    chapterTitle = chapterTitle,
                                    isRtl = false,
                                    cropBorders = cropBorders,
                                    colorFilter = readerColorFilter,
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
                        } else {
                            if (pageIndex < displayPages.size) {
                                MangaPageCanvas(
                                    pageIndex = pageIndex,
                                    totalPages = displayPages.size,
                                    chapterTitle = chapterTitle,
                                    isRtl = false,
                                    cropBorders = cropBorders,
                                    page = displayPages.getOrNull(pageIndex),
                                    colorFilter = readerColorFilter,
                                    onZoomStateChanged = { isPageZoomed = it },
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
                DefaultReadingMode.VERTICAL -> {
                    VerticalPager(
                        state = pagerState,
                        userScrollEnabled = !isPageZoomed,
                        modifier = Modifier
                            .fillMaxSize()
                            .readerTapHandler(
                                isRtl = false,
                                tapZonesEnabled = tapZonesEnabled,
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
                                colorFilter = readerColorFilter,
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

        // Floating Page Indicator Pill (hidden when bottom controls are visible to avoid duplicate badges)
        if (!isAtEndOfChapter && !areControlsVisible) {
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
                    text = currentDisplayPageLabel,
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        size = 48.dp,
                        iconSize = 20.dp,
                        glassState = null,
                        backgroundColor = Color(0xF50F1219),
                        borderColor = GlassBorderStrong,
                        modifier = Modifier.requiredSize(48.dp),
                        onClick = onBack
                    )

                    // Chapter Title Capsule (truncates on narrow widths without squeezing 48dp touch targets)
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }

                    // Settings, Bookmark & More Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PanelCircleIconButton(
                            icon = Icons.Default.Tune,
                            contentDescription = "Reader Settings",
                            size = 48.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            modifier = Modifier.requiredSize(48.dp),
                            onClick = onOpenSettings
                        )
                        PanelCircleIconButton(
                            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            size = 48.dp,
                            iconSize = 20.dp,
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            modifier = Modifier.requiredSize(48.dp),
                            onClick = { isBookmarked = !isBookmarked }
                        )
                        PanelCircleIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            size = 48.dp,
                            iconSize = 20.dp,
                            glassState = null,
                            backgroundColor = Color(0xF50F1219),
                            borderColor = GlassBorderStrong,
                            modifier = Modifier.requiredSize(48.dp),
                            onClick = { /* Menu */ }
                        )
                    }
                }

                // In-Reader Liquid Glass Mode Switcher [ RTL Manga | LTR Comic | Webtoon ]
                ReaderModeSwitcher(
                    currentMode = readingMode,
                    isDualSpread = isDualSpreadActive,
                    onSelectMode = { newMode ->
                        repo.setDefaultReadingMode(newMode)
                    },
                    glassState = glassState
                )
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
                        if (readingMode == DefaultReadingMode.WEBTOON) {
                            // Autoscroll toggle button
                            val primaryAccent = MaterialTheme.colorScheme.primary
                            Row(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(if (isAutoscrollActive) primaryAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                    .border(0.5.dp, if (isAutoscrollActive) primaryAccent else Color.White.copy(alpha = 0.15f), CapsuleShape)
                                    .clickable {
                                        isAutoscrollActive = !isAutoscrollActive
                                        isAutoscrollPausedByTouch = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAutoscrollActive && !isAutoscrollPausedByTouch) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Auto Scroll",
                                    tint = if (isAutoscrollActive) primaryAccent else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAutoscrollActive) (if (isAutoscrollPausedByTouch) "Paused" else "Auto-Scroll") else "Auto-Scroll",
                                    fontSize = 11.sp,
                                    color = if (isAutoscrollActive) primaryAccent else Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
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
                            } else if (isDualSpreadActive) {
                                "$currentDisplayPageLabel • Dual Spread"
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

                        if (readingMode == DefaultReadingMode.WEBTOON) {
                            // Quick Speed Stepper Pill
                            Row(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CapsuleShape)
                                    .clickable {
                                        autoscrollSpeed = when {
                                            autoscrollSpeed <= 1.2f -> 2f
                                            autoscrollSpeed <= 2.2f -> 3f
                                            else -> 1f
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speed: ${if (autoscrollSpeed <= 1.2f) "1x" else if (autoscrollSpeed <= 2.2f) "2x" else "3x"}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
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
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Chapter Prev | Page Indicator | Chapter Next (no horizontal scrollbar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Chapter button
                        PanelCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = stringResource(Res.string.reader_previous_chapter),
                            size = 44.dp,
                            iconSize = 22.dp,
                            glassState = null,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            tint = if (hasPreviousChapter) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                            onClick = { if (hasPreviousChapter) onPreviousChapter() }
                        )

                        Text(
                            text = if (displayPages.isNotEmpty()) "$currentDisplayPageLabel / ${displayPages.size}" else currentDisplayPageLabel,
                            color = TextPrimary.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Next Chapter button
                        PanelCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = stringResource(Res.string.reader_next_chapter),
                            size = 44.dp,
                            iconSize = 22.dp,
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

        // LAYER 4: Floating Autoscroll Pill (accessible even when reader controls are hidden for immersive reading)
        AnimatedVisibility(
            visible = isAutoscrollActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = if (areControlsVisible) 130.dp else 24.dp)
        ) {
            WebtoonAutoscrollPill(
                isPlaying = isAutoscrollActive,
                isPausedByTouch = isAutoscrollPausedByTouch,
                speed = autoscrollSpeed,
                onTogglePlay = {
                    if (isAutoscrollPausedByTouch) {
                        isAutoscrollPausedByTouch = false
                    } else {
                        isAutoscrollPausedByTouch = true
                    }
                },
                onCycleSpeed = {
                    autoscrollSpeed = when {
                        autoscrollSpeed <= 1.2f -> 2f
                        autoscrollSpeed <= 2.2f -> 3f
                        else -> 1f
                    }
                },
                onClose = {
                    isAutoscrollActive = false
                    isAutoscrollPausedByTouch = false
                }
            )
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
    modifier: Modifier = Modifier,
    isDualSpread: Boolean = false
) {
    val modes = listOf(
        Triple(
            DefaultReadingMode.RIGHT_TO_LEFT,
            if (isDualSpread) "◂ RTL (Dual)" else "◂ RTL Manga",
            "Japanese Right-to-Left paging"
        ),
        Triple(
            DefaultReadingMode.LEFT_TO_RIGHT,
            if (isDualSpread) "LTR (Dual) ▸" else "LTR Comic ▸",
            "Western Left-to-Right paging"
        ),
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
 * Inline seamless chapter transition gap for continuous webtoon vertical reading.
 * Provides a clean visual boundary showing the next chapter title and number with tap/scroll continuation.
 */
@Composable
fun WebtoonChapterTransitionGap(
    currentChapterTitle: String,
    nextChapterTitle: String,
    onContinueToNextChapter: () -> Unit,
    glassState: PanelGlassState,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F131D).copy(alpha = 0.85f))
                .border(1.dp, GlassBorderStrong, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onContinueToNextChapter
                )
                .padding(vertical = 20.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Subtle glowing divider line
            val primaryAccent = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(CapsuleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                primaryAccent,
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Finished chapter label
            Text(
                text = "$currentChapterTitle Complete",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Next chapter banner badge
            Row(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(primaryAccent.copy(alpha = 0.15f))
                    .border(1.dp, primaryAccent.copy(alpha = 0.35f), CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NEXT: $nextChapterTitle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryAccent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action / scroll prompt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tap or scroll to continue",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * End-of-series milestone card presented when all chapters have been read.
 */
@Composable
fun WebtoonEndOfSeriesCard(
    chapterTitle: String,
    onBack: () -> Unit,
    glassState: PanelGlassState,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F131D).copy(alpha = 0.90f))
                .border(1.dp, GlassBorderStrong, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), CapsuleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SERIES CAUGHT UP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$chapterTitle Finished",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "You've read all available chapters for this title.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, GlassBorderStrong, CapsuleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBack
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Return to Title Details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Floating glass autoscroll controller pill with Play/Pause, speed stepper, and close button.
 */
@Composable
fun WebtoonAutoscrollPill(
    isPlaying: Boolean,
    isPausedByTouch: Boolean,
    speed: Float,
    onTogglePlay: () -> Unit,
    onCycleSpeed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playInteraction = remember { MutableInteractionSource() }
    val speedInteraction = remember { MutableInteractionSource() }
    val closeInteraction = remember { MutableInteractionSource() }

    val speedLabel = when {
        speed <= 1.2f -> "1x"
        speed <= 2.2f -> "2x"
        else -> "3x"
    }

    val primaryAccent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .background(Color(0xF00A0D14))
            .border(1.dp, if (isPlaying && !isPausedByTouch) primaryAccent.copy(alpha = 0.6f) else GlassBorderStrong, CapsuleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Play/Pause icon button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying && !isPausedByTouch) primaryAccent else Color.White.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null,
                        onClick = onTogglePlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying && !isPausedByTouch) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying && !isPausedByTouch) "Pause Autoscroll" else "Start Autoscroll",
                    tint = if (isPlaying && !isPausedByTouch) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Speed selector badge
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, GlassBorder, CapsuleShape)
                    .clickable(
                        interactionSource = speedInteraction,
                        indication = null,
                        onClick = onCycleSpeed
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying && !isPausedByTouch) primaryAccent else Color.White
                )
            }

            // Status hint if paused by touch
            if (isPausedByTouch) {
                Text(
                    text = "Paused",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Close button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Autoscroll",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
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
        val primaryAccent = MaterialTheme.colorScheme.primary
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(primaryAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CHAPTER COMPLETE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryAccent,
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
            val nextLabel = if (nextChapterTitle.isNotBlank()) "Up Next: $nextChapterTitle" else "Up Next: Next Chapter"
            Text(
                text = if (hasNextChapter) nextLabel else "You have caught up to the latest chapter",
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
                        .background(primaryAccent)
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
    colorFilter: ReaderColorFilter = ReaderColorFilter.DEFAULT,
    modifier: Modifier = Modifier
) {
    val imageUrl = page?.imageUrl ?: page?.url
    val hasValidImage = !imageUrl.isNullOrBlank() && (
        imageUrl.startsWith("http://", ignoreCase = true) ||
        imageUrl.startsWith("https://", ignoreCase = true) ||
        imageUrl.startsWith("file:", ignoreCase = true) ||
        imageUrl.startsWith("/")
    )

    val appliedFilter = remember(colorFilter) { ReaderColorFilters.toColorFilter(colorFilter) }

    if (hasValidImage) {
        val platformContext = LocalPlatformContext.current
        val defaultUa = remember { CloudflareChallengeHeuristics.defaultReaderUserAgent() }
        var retryKey by remember(imageUrl) { mutableIntStateOf(0) }
        var loadError by remember(retryKey, imageUrl) { mutableStateOf<String?>(null) }

        if (loadError != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                MangaPageErrorCard(
                    pageNumber = sliceIndex + 1,
                    errorMessage = loadError,
                    onRetry = {
                        loadError = null
                        retryKey++
                    }
                )
            }
        } else {
            val imageRequest = remember(imageUrl, page?.headers, retryKey, defaultUa) {
                buildReaderImageRequest(
                    context = platformContext,
                    imageUrl = imageUrl,
                    headers = page?.headers,
                    defaultUserAgent = defaultUa
                )
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Slice ${sliceIndex + 1}",
                contentScale = ContentScale.FillWidth,
                colorFilter = appliedFilter,
                modifier = modifier.fillMaxWidth(),
                onState = { state ->
                    when (state) {
                        is coil3.compose.AsyncImagePainter.State.Error -> {
                            loadError = formatImageLoadError(imageUrl, state.result.throwable.message)
                        }
                        is coil3.compose.AsyncImagePainter.State.Success -> {
                            loadError = null
                        }
                        else -> {}
                    }
                }
            )
        }
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

internal fun shouldResetReaderScroll(
    previousChapterUrl: String,
    currentChapterUrl: String
): Boolean = previousChapterUrl != currentChapterUrl

internal fun shouldAutoAdvanceOnBottomScroll(
    hasNextChapter: Boolean,
    alreadyTriggered: Boolean,
    canScrollForward: Boolean,
    downwardScrollDeltaY: Float,
    isEmptyPages: Boolean
): Boolean {
    return hasNextChapter &&
        !alreadyTriggered &&
        !isEmptyPages &&
        !canScrollForward &&
        downwardScrollDeltaY < -4f
}

internal data class WebtoonScrollbarMetrics(
    val thumbSizeFraction: Float,
    val thumbOffsetFraction: Float
)

internal fun computeWebtoonScrollbarMetrics(
    firstVisibleIndex: Int,
    firstVisibleItemScrollOffset: Int,
    firstVisibleItemHeight: Int,
    visibleItemsCount: Int,
    totalItemsCount: Int
): WebtoonScrollbarMetrics {
    if (totalItemsCount <= 0 || visibleItemsCount <= 0 || totalItemsCount <= visibleItemsCount) {
        return WebtoonScrollbarMetrics(thumbSizeFraction = 1f, thumbOffsetFraction = 0f)
    }
    val itemFraction = if (firstVisibleItemHeight > 0) {
        (firstVisibleItemScrollOffset.toFloat() / firstVisibleItemHeight.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val thumbSize = (visibleItemsCount.toFloat() / totalItemsCount.toFloat()).coerceIn(0.08f, 1f)
    val scrollableItems = (totalItemsCount - visibleItemsCount).coerceAtLeast(1).toFloat()
    val progress = ((firstVisibleIndex + itemFraction) / scrollableItems).coerceIn(0f, 1f)
    val thumbOffset = progress * (1f - thumbSize)
    return WebtoonScrollbarMetrics(
        thumbSizeFraction = thumbSize,
        thumbOffsetFraction = thumbOffset
    )
}

@Composable
private fun WebtoonVerticalScrollbar(
    listState: LazyListState,
    isControlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val metrics by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            computeWebtoonScrollbarMetrics(
                firstVisibleIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                firstVisibleItemHeight = visibleItems.firstOrNull()?.size ?: 0,
                visibleItemsCount = visibleItems.size,
                totalItemsCount = layoutInfo.totalItemsCount
            )
        }
    }

    if (metrics.thumbSizeFraction >= 0.99f) return

    var isDraggingBar by remember { mutableStateOf(false) }
    val isScrolling = listState.isScrollInProgress || isDraggingBar
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling || isControlsVisible) 0.95f else 0.45f
    )
    val thumbColor = if (isScrolling) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .width(28.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val thumbHeightPx = (trackHeightPx * metrics.thumbSizeFraction).coerceAtLeast(with(density) { 36.dp.toPx() })
        val maxOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val normalizedProgress = if (1f - metrics.thumbSizeFraction > 0f) {
            (metrics.thumbOffsetFraction / (1f - metrics.thumbSizeFraction)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val thumbOffsetPx = (maxOffsetPx * normalizedProgress).roundToInt()

        fun scrollToTrackY(yPx: Float) {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 0) return
            val fraction = ((yPx - thumbHeightPx / 2f) / maxOffsetPx).coerceIn(0f, 1f)
            val targetIdx = (fraction * (total - 1)).roundToInt().coerceIn(0, total - 1)
            scope.launch {
                listState.scrollToItem(targetIdx)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trackHeightPx, maxOffsetPx) {
                    detectTapGestures(
                        onTap = { offset -> scrollToTrackY(offset.y) }
                    )
                }
                .pointerInput(trackHeightPx, maxOffsetPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDraggingBar = true
                            scrollToTrackY(offset.y)
                        },
                        onDragEnd = { isDraggingBar = false },
                        onDragCancel = { isDraggingBar = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            scrollToTrackY(change.position.y)
                        }
                    )
                },
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, thumbOffsetPx) }
                        .height(with(density) { thumbHeightPx.toDp() })
                        .clip(CapsuleShape)
                        .background(thumbColor)
                )
            }
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
