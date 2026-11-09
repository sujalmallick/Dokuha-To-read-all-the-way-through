package ui.screens.webtoonreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelCircleIconButton
import ui.components.PanelSlider
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.PanelBackground
import ui.theme.PanelTheme
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

        val pages by repository.getPages(chapterUrl).collectAsState(initial = emptyList())

        WebtoonReaderScreenContent(
            chapterTitle = chapterTitle,
            pages = pages,
            onBack = { navigator.pop() },
            onOpenSettings = { navigator.push(ReaderSettingsScreen()) }
        )
    }
}

@Composable
fun WebtoonReaderScreenContent(
    chapterTitle: String,
    pages: List<SourcePage>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var areControlsVisible by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }

    val displayPages = if (pages.isNotEmpty()) pages else (1..6).map {
        SourcePage(index = it, url = "slice_$it")
    }

    // Derive scroll progression percentage 0f..1f
    val currentProgress by remember {
        derivedStateOf {
            val total = displayPages.size
            if (total <= 1) 0f
            else {
                val index = listState.firstVisibleItemIndex.toFloat()
                val offset = listState.firstVisibleItemScrollOffset.toFloat() / 1000f
                ((index + offset) / total.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    LaunchedEffect(currentProgress) {
        if (!isDraggingSlider) {
            sliderProgress = currentProgress
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Continuous Vertical Webtoon Strip (0px seams) - Content layer sampled by glass
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { areControlsVisible = !areControlsVisible }
                )
        ) {
            itemsIndexed(displayPages) { index, page ->
                WebtoonPanelSlice(
                    sliceIndex = index,
                    totalSlices = displayPages.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                )
            }
        }

        // Top Floating Control Bar - Sibling overlay using Clear Glass with 35% dimming over artwork
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                PanelCircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    size = 42.dp,
                    iconSize = 20.dp,
                    glassState = glassState,
                    hasDimming = true,
                    onClick = onBack
                )

                // Chapter Title Capsule
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .glassDimming(alpha = 0.35f, shape = CapsuleShape)
                        .glassSurface(
                            state = glassState,
                            variant = GlassVariant.Clear,
                            shape = CapsuleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chapterTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }

                // Bookmark & More Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelCircleIconButton(
                        icon = Icons.Default.Tune,
                        size = 42.dp,
                        iconSize = 20.dp,
                        glassState = glassState,
                        hasDimming = true,
                        onClick = onOpenSettings
                    )
                    PanelCircleIconButton(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        size = 42.dp,
                        iconSize = 20.dp,
                        tint = if (isBookmarked) ElectricBlue else TextPrimary,
                        glassState = glassState,
                        hasDimming = true,
                        onClick = { isBookmarked = !isBookmarked }
                    )
                    PanelCircleIconButton(
                        icon = Icons.Default.MoreVert,
                        size = 42.dp,
                        iconSize = 20.dp,
                        glassState = glassState,
                        hasDimming = true,
                        onClick = { /* Menu */ }
                    )
                }
            }
        }

        // Bottom Floating Scrubber Capsule Bar matching option 2.png
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous button
                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.NavigateBefore,
                        size = 36.dp,
                        iconSize = 20.dp,
                        glassState = null,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.12f),
                        onClick = {
                            coroutineScope.launch {
                                val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Reading progress slider
                    PanelSlider(
                        value = if (isDraggingSlider) sliderProgress else currentProgress,
                        onValueChange = {
                            isDraggingSlider = true
                            sliderProgress = it
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Next button
                    PanelCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.NavigateNext,
                        size = 36.dp,
                        iconSize = 20.dp,
                        glassState = null,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.12f),
                        onClick = {
                            coroutineScope.launch {
                                val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(displayPages.lastIndex)
                                listState.animateScrollToItem(target)
                            }
                        }
                    )
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
    modifier: Modifier = Modifier
) {
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
            // Left pillar
            drawRect(
                color = gateColor,
                topLeft = Offset(width * 0.12f, 0f),
                size = androidx.compose.ui.geometry.Size(width * 0.06f, height)
            )
            // Right pillar
            drawRect(
                color = gateColor,
                topLeft = Offset(width * 0.82f, 0f),
                size = androidx.compose.ui.geometry.Size(width * 0.06f, height)
            )
            // Top beam
            if (sliceIndex % 4 == 0) {
                drawRect(
                    color = gateColor,
                    topLeft = Offset(width * 0.06f, height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(width * 0.88f, 24f)
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
        val pageNumber = sliceIndex + 1
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 16f,
            center = Offset(width - 32f, height - 32f)
        )
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
