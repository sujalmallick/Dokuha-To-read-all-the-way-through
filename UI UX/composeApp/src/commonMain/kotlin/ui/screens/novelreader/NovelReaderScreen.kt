package ui.screens.novelreader

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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.SettingsRepository
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
import ui.theme.ElectricBlue
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
    val chapterTitle: String = "The Wind Rises"
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()

        var currentChapterUrl by remember(chapterUrl) { mutableStateOf(chapterUrl) }
        var currentChapterNumber by remember(chapterNumber) { mutableStateOf(chapterNumber) }
        var currentChapterTitle by remember(chapterTitle) { mutableStateOf(chapterTitle) }

        val novelContent by repository.getNovelContent(currentChapterUrl).collectAsState(initial = null)

        LaunchedEffect(currentChapterUrl) {
            repository.recordHistory(currentChapterUrl)
        }

        NovelReaderScreenContent(
            chapterNumber = currentChapterNumber,
            chapterTitle = currentChapterTitle,
            novelContent = novelContent,
            settingsRepository = settingsRepository,
            onBack = { navigator.pop() },
            onPreviousChapter = {
                currentChapterNumber = "Chapter 11"
                currentChapterTitle = "Shadows in the Mist"
                currentChapterUrl = "manga/the-wind-rises/ch-11"
            },
            onNextChapter = {
                currentChapterNumber = "Chapter 13"
                currentChapterTitle = "Beyond the Horizon"
                currentChapterUrl = "manga/the-wind-rises/ch-13"
            }
        )
    }
}

@Composable
fun NovelReaderScreenContent(
    chapterNumber: String,
    chapterTitle: String,
    novelContent: NovelContent?,
    onBack: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    hasPreviousChapter: Boolean = true,
    hasNextChapter: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val repo = settingsRepository ?: remember { FakeSettingsRepository() }

    val selectedTheme by repo.novelTheme.collectAsState()
    val novelFont by repo.novelFont.collectAsState()
    val fontSize by repo.novelFontSize.collectAsState()
    val lineHeightMultiplier by repo.novelLineHeight.collectAsState()

    val themeColors = remember(selectedTheme) { selectedTheme.toColors() }
    val fontFamily = remember(novelFont) { novelFont.toFontFamily() }
    val lineHeight = (fontSize * lineHeightMultiplier).sp

    var isListening by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    val parsedParagraphs: List<String> = remember(novelContent) {
        novelContent?.plainText
            ?.split("\n\n", "\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.backgroundColor)
    ) {
        // LAYER 1: Main Scrollable Reader Content (Sampled by Glass Overlays)
        LazyColumn(
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

            // Paragraphs: render from novelContent or procedural fallback matching UI mockups
            if (parsedParagraphs.isNotEmpty()) {
                parsedParagraphs.forEachIndexed { index, para ->
                    item {
                        if (index == 0) {
                            val firstChar = para.take(1)
                            val restOfPara = para.drop(1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = firstChar,
                                    fontSize = (fontSize * 2.8f).sp,
                                    lineHeight = (fontSize * 2.8f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.textColor,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    text = restOfPara,
                                    fontSize = fontSize.sp,
                                    lineHeight = lineHeight,
                                    color = themeColors.textColor.copy(alpha = 0.9f),
                                    fontFamily = fontFamily
                                )
                            }
                        } else {
                            Text(
                                text = para,
                                fontSize = fontSize.sp,
                                lineHeight = lineHeight,
                                color = themeColors.textColor.copy(alpha = 0.9f),
                                fontFamily = fontFamily
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // Procedural fallback paragraphs matching mockups
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "T",
                            fontSize = 52.sp,
                            lineHeight = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textColor,
                            fontFamily = fontFamily,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                        )
                        Text(
                            text = "he morning light filtered through the leaves, casting moving shadows on the stone path. Wei Wuxian adjusted his sword at his side and looked toward the distant mountains. A new journey awaited, and with it, countless unknowns.",
                            fontSize = fontSize.sp,
                            lineHeight = lineHeight,
                            color = themeColors.textColor.copy(alpha = 0.9f),
                            fontFamily = fontFamily
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Text(
                        text = "\"We'll set out at dawn,\" he said, turning to his companions. \"This time, we don't just fight the darkness—we understand it.\"",
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight,
                        color = themeColors.textColor.copy(alpha = 0.9f),
                        fontFamily = fontFamily
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Text(
                        text = "A gentle breeze rustled the bamboo groves nearby, carrying the faint scent of rain. The map lay unrolled on the weathered wooden table before them, its ink faded by years of travel, yet each contour held memories of battles fought and quiet moments shared under foreign stars.",
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight,
                        color = themeColors.textColor.copy(alpha = 0.9f),
                        fontFamily = fontFamily
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // Reading Progress Metadata Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12 min left",
                        fontSize = 12.sp,
                        color = themeColors.textColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$chapterNumber • 100%",
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
                    tint = if (isBookmarked) ElectricBlue else themeColors.textColor,
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
                    onClick = { /* Menu */ }
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
                                repo.setNovelFontSize(nextSize)
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
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .nonGlassPress(dotInteraction)
                                .background(colors.dotColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = dotInteraction,
                                    indication = null,
                                    onClick = { repo.setNovelTheme(theme) }
                                )
                        )
                    }
                }

                // TTS "Listen" Audio Button
                val ttsInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .nonGlassPress(ttsInteraction)
                        .clickable(
                            interactionSource = ttsInteraction,
                            indication = null,
                            onClick = { isListening = !isListening }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Listen",
                        tint = if (isListening) ElectricBlue else themeColors.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isListening) "Playing" else "Listen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) ElectricBlue else themeColors.textColor
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
