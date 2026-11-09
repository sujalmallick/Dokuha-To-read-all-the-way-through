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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelCircleIconButton
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.nonGlassPress
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

enum class NovelThemePreset(
    val title: String,
    val dotColor: Color,
    val backgroundColor: Color,
    val textColor: Color
) {
    DARK("Dark", Color(0xFF141926), Color(0xFF090D15), Color(0xFFE2E8F0)),
    SEPIA("Sepia", Color(0xFF382A20), Color(0xFF261D17), Color(0xFFDECBB6)),
    CREAM("Cream", Color(0xFFE8DCB8), Color(0xFFF7F1DF), Color(0xFF241C16)),
    OLED("OLED", Color(0xFF000000), Color(0xFF000000), Color(0xFFD4D4D8))
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

        val novelContent by repository.getNovelContent(chapterUrl).collectAsState(initial = null)

        NovelReaderScreenContent(
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle,
            novelContent = novelContent,
            onBack = { navigator.pop() }
        )
    }
}

@Composable
fun NovelReaderScreenContent(
    chapterNumber: String,
    chapterTitle: String,
    novelContent: NovelContent?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedTheme by remember { mutableStateOf(NovelThemePreset.DARK) }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var isListening by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(selectedTheme.backgroundColor)
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
                        color = selectedTheme.textColor,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapterTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = selectedTheme.textColor.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // First Paragraph with Drop Cap "T" matching option 2.png
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
                        color = selectedTheme.textColor,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Text(
                        text = "he morning light filtered through the leaves, casting moving shadows on the stone path. Wei Wuxian adjusted his sword at his side and looked toward the distant mountains. A new journey awaited, and with it, countless unknowns.",
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.65f).sp,
                        color = selectedTheme.textColor.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Serif
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Subsequent Paragraphs
            item {
                Text(
                    text = "\"We'll set out at dawn,\" he said, turning to his companions. \"This time, we don't just fight the darkness—we understand it.\"",
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.65f).sp,
                    color = selectedTheme.textColor.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Text(
                    text = "A gentle breeze rustled the bamboo groves nearby, carrying the faint scent of rain. The map lay unrolled on the weathered wooden table before them, its ink faded by years of travel, yet each contour held memories of battles fought and quiet moments shared under foreign stars.",
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.65f).sp,
                    color = selectedTheme.textColor.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(28.dp))
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
                        color = selectedTheme.textColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$chapterNumber • 68%",
                        fontSize = 12.sp,
                        color = selectedTheme.textColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
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
                tint = selectedTheme.textColor,
                glassState = glassState,
                hasDimming = true,
                onClick = onBack
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = selectedTheme.textColor,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = { /* Table of contents */ }
                )
                PanelCircleIconButton(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = if (isBookmarked) ElectricBlue else selectedTheme.textColor,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = { isBookmarked = !isBookmarked }
                )
                PanelCircleIconButton(
                    icon = Icons.Default.MoreVert,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = selectedTheme.textColor,
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
                .padding(horizontal = 24.dp, vertical = 20.dp)
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
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: "Aa" Typography toggle with nonGlassPress
                val aaInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .nonGlassPress(aaInteraction)
                        .clickable(
                            interactionSource = aaInteraction,
                            indication = null,
                            onClick = {
                                fontSize = when (fontSize) {
                                    16f -> 18f
                                    18f -> 20f
                                    else -> 16f
                                }
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Center: 4 Color Theme Dots with "Theme" label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NovelThemePreset.entries.forEach { preset ->
                            val isSelected = preset == selectedTheme
                            val dotInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .nonGlassPress(dotInteraction)
                                    .background(preset.dotColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = dotInteraction,
                                        indication = null,
                                        onClick = { selectedTheme = preset }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Theme",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right: "Listen" TTS Audio Button with nonGlassPress
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
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Listen",
                        tint = if (isListening) ElectricBlue else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "Playing" else "Listen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) ElectricBlue else TextPrimary
                    )
                }
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
