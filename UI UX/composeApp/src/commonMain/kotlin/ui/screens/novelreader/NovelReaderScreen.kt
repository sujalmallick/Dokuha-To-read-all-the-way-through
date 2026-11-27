package ui.screens.novelreader

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.platform.PlatformTts
import core.platform.TtsListener
import core.platform.rememberPlatformTts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.NovelContent
import data.mock.PanelRepository
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.ReaderPrefsRepository
import data.settings.ReaderSettingsScope
import data.settings.SettingsRepository
import data.settings.canonicalTitleKey
import data.settings.observeResolvedNovelSettings
import data.settings.resolveReaderSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelCircleIconButton
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.nonGlassPress
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

data class NovelThemeColors(
    val dotColor: Color,
    val backgroundColor: Color,
    val textColor: Color
)

fun NovelReaderTheme.toColors(): NovelThemeColors {
    return when (this) {
        NovelReaderTheme.DARK -> NovelThemeColors(
            dotColor = Color(0xFF141926),
            backgroundColor = Color(0xFF090D15),
            textColor = Color(0xFFE2E8F0)
        )
        NovelReaderTheme.SEPIA -> NovelThemeColors(
            dotColor = Color(0xFF382A20),
            backgroundColor = Color(0xFF261D17),
            textColor = Color(0xFFDECBB6)
        )
        NovelReaderTheme.LIGHT -> NovelThemeColors(
            dotColor = Color(0xFFE8DCB8),
            backgroundColor = Color(0xFFF7F1DF),
            textColor = Color(0xFF241C16)
        )
        NovelReaderTheme.OLED -> NovelThemeColors(
            dotColor = Color(0xFF000000),
            backgroundColor = Color(0xFF000000),
            textColor = Color(0xFFD4D4D8)
        )
    }
}

fun NovelFont.toFontFamily(): FontFamily {
    return when (this) {
        NovelFont.SYSTEM -> FontFamily.Default
        NovelFont.SERIF -> FontFamily.Serif
        NovelFont.SANS_SERIF -> FontFamily.SansSerif
        NovelFont.MONOSPACE -> FontFamily.Monospace
    }
}

data class NovelReaderScreen(
    val chapterUrl: String = "manga/the-wind-rises/ch-12",
    val chapterNumber: String = "Chapter 12",
    val chapterTitle: String = "The Wind Rises",
    val mangaUrl: String? = null,
    val sourceId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()
        val readerPrefsRepository: ReaderPrefsRepository = koinInject()
        val volumeKeyState = ui.volume.LocalVolumeKeyState.current

        androidx.compose.runtime.DisposableEffect(volumeKeyState) {
            volumeKeyState.isReaderActive.value = true
            onDispose { volumeKeyState.isReaderActive.value = false }
        }

        var currentChapterUrl by remember(chapterUrl) { mutableStateOf(chapterUrl) }
        var currentChapterNumber by remember(chapterNumber) { mutableStateOf(chapterNumber) }
        var currentChapterTitle by remember(chapterTitle) { mutableStateOf(chapterTitle) }
        var refreshTrigger by remember { mutableStateOf(0) }

        val targetMangaUrl = remember(mangaUrl, currentChapterUrl) {
            mangaUrl ?: if (currentChapterUrl.contains("/")) currentChapterUrl.substringBeforeLast("/") else ""
        }
        val mangaDetails by remember(targetMangaUrl, sourceId) {
            repository.getMangaDetails(targetMangaUrl, sourceId)
        }.collectAsState(initial = null)
        val resolvedTitleKey = remember(mangaDetails?.title, targetMangaUrl) {
            canonicalTitleKey(mangaDetails?.title ?: targetMangaUrl, isNovel = true)
        }
        val chapters by repository.getChapters(targetMangaUrl, sourceId).collectAsState(initial = emptyList())
        var isLoading by remember(currentChapterUrl, refreshTrigger) { mutableStateOf(true) }
        var loadingTimedOut by remember(currentChapterUrl, refreshTrigger) { mutableStateOf(false) }
        val novelContent by remember(currentChapterUrl, sourceId, refreshTrigger) {
            kotlinx.coroutines.flow.flow {
                try {
                    repository.getNovelContent(currentChapterUrl, sourceId).collect { content ->
                        isLoading = false
                        emit(content)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    isLoading = false
                    emit(null)
                }
            }
        }.collectAsState(initial = null)

        LaunchedEffect(currentChapterUrl, refreshTrigger, novelContent) {
            if (novelContent == null && isLoading) {
                kotlinx.coroutines.delay(10_000L)
                if (novelContent == null) {
                    isLoading = false
                    loadingTimedOut = true
                }
            }
        }

        val currentChapterIndex = chapters.indexOfFirst { it.url == currentChapterUrl || it.name == currentChapterTitle }
        val hasPrevious = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex < chapters.lastIndex
        } else false
        val hasNext = if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
            currentChapterIndex > 0
        } else false

        LaunchedEffect(currentChapterUrl) {
            repository.recordHistory(currentChapterUrl)
        }

        NovelReaderScreenContent(
            chapterNumber = currentChapterNumber,
            chapterTitle = currentChapterTitle,
            novelContent = novelContent,
            settingsRepository = settingsRepository,
            titleKey = resolvedTitleKey,
            readerPrefsRepository = readerPrefsRepository,
            onBack = { navigator.pop() },
            onPreviousChapter = {
                if (chapters.isNotEmpty() && currentChapterIndex in 0 until chapters.lastIndex) {
                    val prevChapter = chapters[currentChapterIndex + 1]
                    currentChapterUrl = prevChapter.url
                    currentChapterNumber = if (prevChapter.chapterNumber > 0) "Chapter ${prevChapter.chapterNumber}" else prevChapter.name
                    currentChapterTitle = prevChapter.name
                }
            },
            onNextChapter = {
                if (chapters.isNotEmpty() && currentChapterIndex > 0) {
                    val nextChapter = chapters[currentChapterIndex - 1]
                    currentChapterUrl = nextChapter.url
                    currentChapterNumber = if (nextChapter.chapterNumber > 0) "Chapter ${nextChapter.chapterNumber}" else nextChapter.name
                    currentChapterTitle = nextChapter.name
                }
            },
            hasPreviousChapter = hasPrevious,
            hasNextChapter = hasNext,
            isLoading = isLoading && !loadingTimedOut,
            onRetry = {
                loadingTimedOut = false
                isLoading = true
                refreshTrigger++
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreenContent(
    chapterNumber: String,
    chapterTitle: String,
    novelContent: NovelContent?,
    onBack: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    hasPreviousChapter: Boolean = true,
    hasNextChapter: Boolean = true,
    isLoading: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val repo = settingsRepository ?: remember { FakeSettingsRepository() }
    val prefsRepo = readerPrefsRepository ?: remember { FakeReaderPrefsRepository(repo) }
    val uiScope = rememberCoroutineScope()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var settingsScope by rememberSaveable(titleKey) {
        mutableStateOf(ReaderSettingsScope.ALL_TITLES)
    }
    var hasInitializedScope by rememberSaveable(titleKey) { mutableStateOf(false) }

    LaunchedEffect(titleKey, prefsRepo) {
        if (!hasInitializedScope && titleKey != null) {
            val initialPrefs = prefsRepo.getPrefs(titleKey)
            if (initialPrefs?.hasNovelOverrides == true) {
                settingsScope = ReaderSettingsScope.THIS_TITLE
            }
            hasInitializedScope = true
        }
    }

    val resolvedNovel by remember(titleKey, repo, prefsRepo) {
        val overrideFlow = if (titleKey != null) prefsRepo.observePrefs(titleKey) else flowOf(null)
        observeResolvedNovelSettings(titleKey ?: "", repo, overrideFlow)
    }.collectAsState(initial = resolveReaderSettings(titleKey ?: "", repo, null))

    val isTitleScope = titleKey != null && settingsScope == ReaderSettingsScope.THIS_TITLE

    val selectedTheme = resolvedNovel.novelTheme
    val novelFont = resolvedNovel.novelFont
    val fontSize = resolvedNovel.novelFontSize
    val lineHeightMultiplier = resolvedNovel.novelLineHeight

    val themeColors = remember(selectedTheme) { selectedTheme.toColors() }
    val fontFamily = remember(novelFont) { novelFont.toFontFamily() }
    val lineHeight = (fontSize * lineHeightMultiplier).sp

    val tts = rememberPlatformTts()
    var isListening by remember { mutableStateOf(false) }
    var currentSpeakingIndex by remember { mutableIntStateOf(-1) }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }
    var isBookmarked by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val parsedParagraphs: List<String> = remember(novelContent) {
        novelContent?.plainText
            ?.split("\n\n", "\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    // Auto-scroll to currently spoken paragraph
    LaunchedEffect(currentSpeakingIndex, isListening) {
        if (currentSpeakingIndex >= 0 && isListening) {
            val target = (currentSpeakingIndex + 1).coerceIn(0, parsedParagraphs.size)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.backgroundColor)
    ) {
        // LAYER 1: Main Scrollable Reader Content (Sampled by Glass Overlays)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 84.dp,
                bottom = 120.dp
            )
        ) {
            // Chapter Header Block
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(
                        text = chapterNumber,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textColor,
                        letterSpacing = (-0.5).sp,
                        fontFamily = fontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapterTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = themeColors.textColor.copy(alpha = 0.75f),
                        fontFamily = fontFamily
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Paragraphs: render from novelContent or real empty state
            if (parsedParagraphs.isNotEmpty()) {
                parsedParagraphs.forEachIndexed { index, para ->
                    item(key = "para_$index") {
                        val isCurrentSpeech = isListening && currentSpeakingIndex == index
                        val highlightShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(highlightShape)
                                .background(
                                    if (isCurrentSpeech) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .then(
                                    if (isCurrentSpeech) Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), highlightShape)
                                    else Modifier
                                )
                                .padding(horizontal = if (isCurrentSpeech) 10.dp else 0.dp, vertical = if (isCurrentSpeech) 6.dp else 0.dp)
                        ) {
                            if (index == 0) {
                                val firstChar = para.take(1)
                                val restOfPara = para.drop(1)
                                val styledFirstPara = androidx.compose.ui.text.buildAnnotatedString {
                                    pushStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            fontSize = (fontSize * 1.65f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentSpeech) MaterialTheme.colorScheme.primary else themeColors.textColor
                                        )
                                    )
                                    append(firstChar)
                                    pop()
                                    pushStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            fontSize = fontSize.sp,
                                            color = themeColors.textColor.copy(alpha = if (isCurrentSpeech) 1.0f else 0.9f)
                                        )
                                    )
                                    append(restOfPara)
                                    pop()
                                }
                                Text(
                                    text = styledFirstPara,
                                    lineHeight = lineHeight,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = para,
                                    fontSize = fontSize.sp,
                                    lineHeight = lineHeight,
                                    color = themeColors.textColor.copy(alpha = if (isCurrentSpeech) 1.0f else 0.9f),
                                    fontFamily = fontFamily
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loading chapter...",
                                fontSize = 14.sp,
                                color = themeColors.textColor.copy(alpha = 0.7f),
                                fontFamily = fontFamily
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No chapter content found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColors.textColor,
                                fontFamily = fontFamily
                            )
                            Text(
                                text = "This chapter could not be loaded or is empty.",
                                fontSize = 14.sp,
                                color = themeColors.textColor.copy(alpha = 0.6f),
                                fontFamily = fontFamily
                            )
                            if (onRetry != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { onRetry() }
                                        .padding(horizontal = 18.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = "Retry",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeColors.textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reading Progress Metadata Row
            item {
                val wordCount = remember(parsedParagraphs) {
                    parsedParagraphs.sumOf { p -> p.split("\\s+".toRegex()).count { it.isNotBlank() } }
                }
                val readTimeMinutes = (wordCount / 225).coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (parsedParagraphs.isNotEmpty()) "~$readTimeMinutes min read" else "",
                        fontSize = 12.sp,
                        color = themeColors.textColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = chapterNumber,
                        fontSize = 12.sp,
                        color = themeColors.textColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // End-of-Chapter Transition Card
            item {
                NovelEndOfChapterCard(
                    currentChapterNumber = chapterNumber,
                    hasNextChapter = hasNextChapter,
                    onNextChapter = onNextChapter,
                    glassState = glassState,
                    textColor = themeColors.textColor
                )
            }
        }

        // LAYER 2: Floating Top Bar (Sibling Glass Overlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelCircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40.dp,
                iconSize = 20.dp,
                tint = themeColors.textColor,
                glassState = glassState,
                hasDimming = true,
                onClick = onBack
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = themeColors.textColor,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = { /* Table of contents */ }
                )
                PanelCircleIconButton(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else themeColors.textColor,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = { isBookmarked = !isBookmarked }
                )
                PanelCircleIconButton(
                    icon = Icons.Default.MoreVert,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = themeColors.textColor,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = { isSettingsOpen = true }
                )
            }
        }

        // LAYER 3: Floating Bottom Control Capsule Bar (Liquid Glass Dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .glassDimming(alpha = 0.35f, shape = CapsuleShape)
                .glassSurface(
                    state = glassState,
                    variant = GlassVariant.Clear,
                    shape = CapsuleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous chapter button
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.NavigateBefore,
                    size = 34.dp,
                    iconSize = 18.dp,
                    tint = if (hasPreviousChapter) themeColors.textColor else themeColors.textColor.copy(alpha = 0.3f),
                    glassState = null,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.12f),
                    onClick = { if (hasPreviousChapter) onPreviousChapter() }
                )

                // "Aa" Typography toggle: cycles font sizes
                val aaInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .nonGlassPress(aaInteraction)
                        .clickable(
                            interactionSource = aaInteraction,
                            indication = null,
                            onClick = {
                                val nextSize = when {
                                    fontSize < 15f -> 16f
                                    fontSize < 17f -> 18f
                                    fontSize < 19f -> 20f
                                    fontSize < 21f -> 22f
                                    else -> 14f
                                }
                                if (isTitleScope) {
                                    uiScope.launch {
                                        prefsRepo.updatePrefs(titleKey) { it.copy(novelFontSize = nextSize) }
                                    }
                                } else {
                                    repo.setNovelFontSize(nextSize)
                                }
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textColor
                    )
                }

                // 4 Color Theme Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovelReaderTheme.entries.forEach { theme ->
                        val colors = theme.toColors()
                        val isSelected = theme == selectedTheme
                        val dotInteraction = remember { MutableInteractionSource() }
                        val primaryAccent = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .nonGlassPress(dotInteraction)
                                .background(colors.dotColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) primaryAccent else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = dotInteraction,
                                    indication = null,
                                    onClick = {
                                        if (isTitleScope) {
                                            uiScope.launch {
                                                prefsRepo.updatePrefs(titleKey) { it.copy(novelTheme = theme) }
                                            }
                                        } else {
                                            repo.setNovelTheme(theme)
                                        }
                                    }
                                )
                        )
                    }
                }

                // TTS "Listen" Audio Button
                val ttsInteraction = remember { MutableInteractionSource() }
                val primaryAccent = MaterialTheme.colorScheme.primary
                Row(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(if (isListening) primaryAccent.copy(alpha = 0.20f) else Color.Transparent)
                        .border(
                            width = if (isListening) 1.dp else 0.dp,
                            color = if (isListening) primaryAccent.copy(alpha = 0.40f) else Color.Transparent,
                            shape = CapsuleShape
                        )
                        .nonGlassPress(ttsInteraction)
                        .clickable(
                            interactionSource = ttsInteraction,
                            indication = null,
                            onClick = {
                                if (isListening) {
                                    isListening = false
                                    tts.pause()
                                } else if (parsedParagraphs.isNotEmpty()) {
                                    isListening = true
                                    val startIdx = if (currentSpeakingIndex >= 0) currentSpeakingIndex else 0
                                    tts.speak(
                                        paragraphs = parsedParagraphs,
                                        startIndex = startIdx,
                                        listener = object : TtsListener {
                                            override fun onParagraphStart(index: Int) {
                                                currentSpeakingIndex = index
                                            }
                                            override fun onComplete() {
                                                isListening = false
                                                currentSpeakingIndex = -1
                                            }
                                            override fun onError(message: String) {
                                                isListening = false
                                            }
                                        }
                                    )
                                }
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Pause else Icons.Default.Headphones,
                        contentDescription = "Listen",
                        tint = if (isListening) primaryAccent else themeColors.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isListening) "Playing" else "Listen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) primaryAccent else themeColors.textColor
                    )
                }

                // Next chapter button
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                    size = 34.dp,
                    iconSize = 18.dp,
                    tint = if (hasNextChapter) themeColors.textColor else themeColors.textColor.copy(alpha = 0.3f),
                    glassState = null,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.12f),
                    onClick = { if (hasNextChapter) onNextChapter() }
                )
            }
        }

        // Floating Audio Player Glass Bar for TTS
        AnimatedVisibility(
            visible = isListening,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            NovelAudioPlayerBar(
                currentParagraph = currentSpeakingIndex + 1,
                totalParagraphs = parsedParagraphs.size,
                speed = ttsSpeed,
                onTogglePlay = {
                    if (isListening) {
                        isListening = false
                        tts.pause()
                    } else {
                        isListening = true
                        tts.resume()
                    }
                },
                onPreviousParagraph = {
                    if (currentSpeakingIndex > 0) {
                        currentSpeakingIndex--
                        tts.speak(parsedParagraphs, currentSpeakingIndex, object : TtsListener {
                            override fun onParagraphStart(index: Int) { currentSpeakingIndex = index }
                            override fun onComplete() { isListening = false }
                            override fun onError(message: String) { isListening = false }
                        })
                    }
                },
                onNextParagraph = {
                    if (currentSpeakingIndex < parsedParagraphs.lastIndex) {
                        currentSpeakingIndex++
                        tts.speak(parsedParagraphs, currentSpeakingIndex, object : TtsListener {
                            override fun onParagraphStart(index: Int) { currentSpeakingIndex = index }
                            override fun onComplete() { isListening = false }
                            override fun onError(message: String) { isListening = false }
                        })
                    }
                },
                onCycleSpeed = {
                    ttsSpeed = when {
                        ttsSpeed <= 0.9f -> 1.0f
                        ttsSpeed <= 1.1f -> 1.25f
                        ttsSpeed <= 1.35f -> 1.5f
                        ttsSpeed <= 1.7f -> 2.0f
                        else -> 0.8f
                    }
                    tts.setSpeed(ttsSpeed)
                },
                onClose = {
                    isListening = false
                    tts.stop()
                    currentSpeakingIndex = -1
                }
            )
        }

        if (isSettingsOpen) {
            NovelSettingsBottomSheet(
                onDismiss = { isSettingsOpen = false },
                settingsRepository = repo,
                titleKey = titleKey,
                readerPrefsRepository = prefsRepo,
                scope = settingsScope,
                onScopeChanged = { chosen ->
                    settingsScope = chosen
                    hasInitializedScope = true
                }
            )
        }
    }
}

/**
 * Floating glass audio player control bar for novel Text-to-Speech playback.
 */
@Composable
fun NovelAudioPlayerBar(
    currentParagraph: Int,
    totalParagraphs: Int,
    speed: Float,
    onTogglePlay: () -> Unit,
    onPreviousParagraph: () -> Unit,
    onNextParagraph: () -> Unit,
    onCycleSpeed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(20.dp)
    val playInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(Color(0xF20B0E17))
            .border(1.dp, GlassBorderStrong, barShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Paragraph indicator
            Column {
                Text(
                    text = "AUDIO READER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Para $currentParagraph / $totalParagraphs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Center: Playback Controls (Prev, Play/Pause, Next)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prev paragraph
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onPreviousParagraph),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Previous Paragraph",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play / Pause main button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = onTogglePlay
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Next paragraph
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onNextParagraph),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Next Paragraph",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Right: Speed Stepper & Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Speed pill
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, GlassBorder, CapsuleShape)
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Close button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Audio Reader",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * End-of-Chapter transition card presented after the last paragraph of a novel.
 */
@Composable
fun NovelEndOfChapterCard(
    currentChapterNumber: String,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    glassState: ui.glass.PanelGlassState,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val buttonInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassDimming(alpha = 0.35f, shape = cardShape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = cardShape
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chapter Complete",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$currentChapterNumber Finished",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (hasNextChapter) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .glassPress(buttonInteraction)
                        .background(MaterialTheme.colorScheme.primary)
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "You're all caught up!",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview
@Composable
fun NovelReaderScreenPreview() {
    PanelTheme {
        NovelReaderScreenContent(
            chapterNumber = "Chapter 12",
            chapterTitle = "The Wind Rises",
            novelContent = null,
            onBack = {}
        )
    }
}
