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
import androidx.compose.foundation.layout.heightIn
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelCircleIconButton
import ui.components.PanelSlider
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
        var startAtEndOnLoad by rememberSaveable(chapterUrl) { mutableStateOf(false) }
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

        // Seamlessly preload next & previous chapter pages so chapter transitions don't stall
        val preloadContext = coil3.compose.LocalPlatformContext.current
        val preloadUa = remember { CloudflareChallengeHeuristics.defaultReaderUserAgent() }
        LaunchedEffect(currentChapterUrl, chapters.size) {
            if (chapters.isEmpty() || currentChapterIndex < 0) return@LaunchedEffect
            val imageLoader = coil3.SingletonImageLoader.get(preloadContext)
            if (currentChapterIndex > 0) {
                val nextCh = chapters[currentChapterIndex - 1]
                val nextPages = runCatching { repository.getPages(nextCh.url, sourceId).firstOrNull() }.getOrNull().orEmpty()
                nextPages.take(3).forEach { p ->
                    val url = p.imageUrl ?: p.url
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        imageLoader.enqueue(buildReaderImageRequest(preloadContext, url, p.headers, preloadUa))
                    }
                }
            }
            if (currentChapterIndex < chapters.lastIndex) {
                val prevCh = chapters[currentChapterIndex + 1]
                val prevPages = runCatching { repository.getPages(prevCh.url, sourceId).firstOrNull() }.getOrNull().orEmpty()
                prevPages.takeLast(3).forEach { p ->
                    val url = p.imageUrl ?: p.url
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        imageLoader.enqueue(buildReaderImageRequest(preloadContext, url, p.headers, preloadUa))
                    }
                }
            }
        }

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
                        startAtEndOnLoad = true
                        currentChapterUrl = prevChapter.url
                        currentChapterTitle = prevChapter.name
                    }
                },
                onNextChapter = {
                    if (chapters.isNotEmpty() && currentChapterIndex > 0) {
                        val nextChapter = chapters[currentChapterIndex - 1]
                        startAtEndOnLoad = false
                        currentChapterUrl = nextChapter.url
                        currentChapterTitle = nextChapter.name
                    }
                },
                startAtEndOnLoad = startAtEndOnLoad,
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
    startAtEndOnLoad: Boolean = false,
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

    // Chapter transition lock prevents double-firing or mid-load jumps (auto-resets if chapterUrl doesn't change)
    var isChapterTransitioning by remember(chapterUrl) { mutableStateOf(false) }
    LaunchedEffect(isChapterTransitioning) {
        if (isChapterTransitioning) {
            kotlinx.coroutines.delay(500L)
            isChapterTransitioning = false
        }
    }
    val safePreviousChapter: () -> Unit = remember(chapterUrl, onPreviousChapter) {
        {
            if (!isChapterTransitioning) {
                isChapterTransitioning = true
                onPreviousChapter()
            }
        }
    }
    val safeNextChapter: () -> Unit = remember(chapterUrl, onNextChapter) {
        {
            if (!isChapterTransitioning) {
                isChapterTransitioning = true
                onNextChapter()
            }
        }
    }

    var areControlsVisible by remember { mutableStateOf(true) }

    var isAutoscrollActive by remember { mutableStateOf(false) }
    var autoscrollSpeed by remember { mutableFloatStateOf(1f) }
    var isAutoscrollPausedByTouch by remember { mutableStateOf(false) }

    // Clean up autoscroll when leaving screen or changing chapter
    DisposableEffect(chapterUrl) {
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
    LaunchedEffect(isAutoscrollActive, isAutoscrollPausedByTouch, autoscrollSpeed, hasNextChapter, isChapterTransitioning) {
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
                    } else if (hasNextChapter && !isChapterTransitioning) {
                        isAutoscrollActive = false
                        safeNextChapter()
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
                if (displayPages.isNotEmpty()) {
                    previousChapterUrl = chapterUrl
                    val targetIdx = if (startAtEndOnLoad) displayPages.lastIndex else 0
                    savedPageIndex = targetIdx
                    savedScrollOffset = 0
                    hasRestoredSavedPosition = true
                    listState.scrollToItem(targetIdx)
                    pagerState.scrollToPage(
                        if (isDualSpreadActive && startAtEndOnLoad) (spreads.size - 1).coerceAtLeast(0) else targetIdx
                    )
                }
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

        // Guard so downward/upward over-scroll at chapter edges requires deliberate user pull & only triggers once
        var hasTriggeredAutoNext by remember(chapterUrl) { mutableStateOf(false) }
        var hasTriggeredAutoPrev by remember(chapterUrl) { mutableStateOf(false) }
        var pullDownOverscrollPx by remember(chapterUrl) { mutableFloatStateOf(0f) }
        var pullUpOverscrollPx by remember(chapterUrl) { mutableFloatStateOf(0f) }
        val autoNextNestedScroll = remember(chapterUrl, hasNextChapter, hasPreviousChapter, displayPages.size) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (source != NestedScrollSource.UserInput) {
                        return Offset.Zero
                    }
                    if (!listState.canScrollForward && available.y < 0f && hasNextChapter && !hasTriggeredAutoNext && displayPages.isNotEmpty()) {
                        pullDownOverscrollPx += available.y
                        if (shouldAutoAdvanceOnBottomScroll(
                                hasNextChapter = hasNextChapter,
                                alreadyTriggered = hasTriggeredAutoNext || isChapterTransitioning,
                                canScrollForward = listState.canScrollForward,
                                downwardScrollDeltaY = pullDownOverscrollPx,
                                isEmptyPages = displayPages.isEmpty()
                            )
                        ) {
                            hasTriggeredAutoNext = true
                            pullDownOverscrollPx = 0f
                            safeNextChapter()
                        }
                    } else if (listState.canScrollForward || available.y > 0f) {
                        pullDownOverscrollPx = 0f
                    }

                    if (!listState.canScrollBackward && available.y > 0f && hasPreviousChapter && !hasTriggeredAutoPrev && !isChapterTransitioning && displayPages.isNotEmpty()) {
                        pullUpOverscrollPx += available.y
                        if (pullUpOverscrollPx > 60f) {
                            hasTriggeredAutoPrev = true
                            pullUpOverscrollPx = 0f
                            safePreviousChapter()
                        }
                    } else if (listState.canScrollBackward || available.y < 0f) {
                        pullUpOverscrollPx = 0f
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    pullDownOverscrollPx = 0f
                    pullUpOverscrollPx = 0f
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

        // Sync active page position when switching readingMode or dual-page mode mid-chapter
        var lastKnownMode by remember { mutableStateOf(readingMode) }
        var lastKnownDual by remember { mutableStateOf(isDualSpreadActive) }
        LaunchedEffect(readingMode, isDualSpreadActive, displayPages.size) {
            if (displayPages.isNotEmpty() && (lastKnownMode != readingMode || lastKnownDual != isDualSpreadActive)) {
                val targetPage = savedPageIndex.coerceIn(0, displayPages.lastIndex)
                if (readingMode == DefaultReadingMode.WEBTOON) {
                    listState.scrollToItem(targetPage)
                } else if (isDualSpreadActive) {
                    val spreadIdx = DualPageEngine.findSpreadIndexForPage(spreads, targetPage)
                    pagerState.scrollToPage(spreadIdx)
                } else {
                    pagerState.scrollToPage(targetPage)
                }
                lastKnownMode = readingMode
                lastKnownDual = isDualSpreadActive
            }
        }

        // Volume key navigation across ALL reading modes (respects RTL/LTR and dual-page)
        val volumeKeyState = LocalVolumeKeyState.current
        val volumeUpCount = volumeKeyState.upPresses.value
        val volumeDownCount = volumeKeyState.downPresses.value
        LaunchedEffect(volumeUpCount) {
            if (volumeKeyNavEnabled && volumeUpCount > 0) {
                handlePrevious()
            }
        }
        LaunchedEffect(volumeDownCount) {
            if (volumeKeyNavEnabled && volumeDownCount > 0) {
                handleNext()
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
                                .fillMaxSize()
                                .nestedScroll(autoNextNestedScroll)
                                .readerTapHandler(
                                    isRtl = false,
                                    tapZonesEnabled = false,
                                    onPrevious = handleToggleControls,
                                    onToggleControls = handleToggleControls,
                                    onNext = handleToggleControls
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
                                .padding(
                                    end = 4.dp,
                                    top = if (areControlsVisible) 88.dp else 12.dp,
                                    bottom = if (areControlsVisible) 88.dp else 12.dp
                                )
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
                                    pages = displayPages,
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
                                    pages = displayPages,
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

        // LAYER 2: Minimal Glass Top Control Bar (Back, 1-line Title, Settings)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val barSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            val barBorder = MaterialTheme.colorScheme.outlineVariant
            val barContent = MaterialTheme.colorScheme.onSurface
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(barSurface)
                    .border(1.dp, barBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    size = 48.dp,
                    iconSize = 22.dp,
                    tint = barContent,
                    glassState = null,
                    backgroundColor = Color.Transparent,
                    borderColor = Color.Transparent,
                    modifier = Modifier.requiredSize(48.dp),
                    onClick = onBack
                )

                Text(
                    text = chapterTitle,
                    color = barContent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                PanelCircleIconButton(
                    icon = Icons.Default.Tune,
                    contentDescription = "Reader Settings",
                    size = 48.dp,
                    iconSize = 20.dp,
                    tint = barContent,
                    glassState = null,
                    backgroundColor = Color.Transparent,
                    borderColor = Color.Transparent,
                    modifier = Modifier.requiredSize(48.dp),
                    onClick = onOpenSettings
                )
            }
        }

        // LAYER 3: Minimal Slim Glass Bottom Bar (Safe-area aware, RTL-aware)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            val barSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            val barBorder = MaterialTheme.colorScheme.outlineVariant
            val barContent = MaterialTheme.colorScheme.onSurface
            val navConfig = resolveReaderBottomNav(
                isRtl = isRtl,
                hasPreviousChapter = hasPreviousChapter,
                hasNextChapter = hasNextChapter
            )
            val sliderValue = mapReaderSliderFraction(currentProgress, isRtl = isRtl)

            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(barSurface)
                    .border(1.dp, barBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = if (navConfig.leftIsNext) {
                            stringResource(Res.string.reader_next_chapter)
                        } else {
                            stringResource(Res.string.reader_previous_chapter)
                        },
                        size = 48.dp,
                        iconSize = 22.dp,
                        glassState = null,
                        backgroundColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        tint = if (navConfig.leftEnabled) barContent else barContent.copy(alpha = 0.28f),
                        modifier = Modifier.requiredSize(48.dp),
                        onClick = {
                            if (navConfig.leftEnabled) {
                                if (navConfig.leftIsNext) safeNextChapter() else safePreviousChapter()
                            }
                        }
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        if (readingMode != DefaultReadingMode.WEBTOON) {
                            PanelSlider(
                                value = sliderValue,
                                onValueChange = { rawFraction ->
                                    val logicalFraction = mapReaderSliderFraction(rawFraction, isRtl = isRtl)
                                    coroutineScope.launch {
                                        val maxIdx = if (isDualSpreadActive) (spreads.size - 1).coerceAtLeast(0) else (displayPages.size - 1).coerceAtLeast(0)
                                        pagerState.scrollToPage((logicalFraction * maxIdx).toInt().coerceIn(0, maxIdx))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = currentDisplayPageLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = if (readingMode == DefaultReadingMode.WEBTOON) 13.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = if (navConfig.rightIsNext) {
                            stringResource(Res.string.reader_next_chapter)
                        } else {
                            stringResource(Res.string.reader_previous_chapter)
                        },
                        size = 48.dp,
                        iconSize = 22.dp,
                        glassState = null,
                        backgroundColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        tint = if (navConfig.rightEnabled) barContent else barContent.copy(alpha = 0.28f),
                        modifier = Modifier.requiredSize(48.dp),
                        onClick = {
                            if (navConfig.rightEnabled) {
                                if (navConfig.rightIsNext) safeNextChapter() else safePreviousChapter()
                            }
                        }
                    )
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
        Triple(DefaultReadingMode.RIGHT_TO_LEFT, if (isDualSpread) "RTL (2P)" else "RTL", "Japanese Right-to-Left"),
        Triple(DefaultReadingMode.LEFT_TO_RIGHT, if (isDualSpread) "LTR (2P)" else "LTR", "Western Left-to-Right"),
        Triple(DefaultReadingMode.WEBTOON, "Webtoon", "Continuous vertical scroll")
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60F1219))
            .border(0.75.dp, GlassBorderStrong, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            modes.forEach { (mode, label, _) ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onContinueToNextChapter() }
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$currentChapterTitle finished",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Loading next: $nextChapterTitle",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Minimal end-of-series footer presented when all chapters have been read.
 */
@Composable
fun WebtoonEndOfSeriesCard(
    chapterTitle: String,
    onBack: () -> Unit,
    glassState: PanelGlassState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$chapterTitle finished",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "All available chapters read",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Back to series",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
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
            var isImageLoaded by remember(imageUrl, retryKey) { mutableStateOf(false) }
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
                modifier = modifier
                    .fillMaxWidth()
                    .then(if (!isImageLoaded) Modifier.heightIn(min = 460.dp) else Modifier),
                onState = { state ->
                    when (state) {
                        is coil3.compose.AsyncImagePainter.State.Error -> {
                            isImageLoaded = false
                            loadError = formatImageLoadError(imageUrl, state.result.throwable.message)
                        }
                        is coil3.compose.AsyncImagePainter.State.Success -> {
                            isImageLoaded = true
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

internal data class ReaderBottomNavConfig(
    val leftIsNext: Boolean,
    val leftEnabled: Boolean,
    val rightIsNext: Boolean,
    val rightEnabled: Boolean
)

internal fun resolveReaderBottomNav(
    isRtl: Boolean,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean
): ReaderBottomNavConfig = if (isRtl) {
    ReaderBottomNavConfig(
        leftIsNext = true,
        leftEnabled = hasNextChapter,
        rightIsNext = false,
        rightEnabled = hasPreviousChapter
    )
} else {
    ReaderBottomNavConfig(
        leftIsNext = false,
        leftEnabled = hasPreviousChapter,
        rightIsNext = true,
        rightEnabled = hasNextChapter
    )
}

internal fun mapReaderSliderFraction(fraction: Float, isRtl: Boolean): Float {
    val clamped = fraction.coerceIn(0f, 1f)
    return if (isRtl) 1f - clamped else clamped
}

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
        downwardScrollDeltaY <= -60f
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
