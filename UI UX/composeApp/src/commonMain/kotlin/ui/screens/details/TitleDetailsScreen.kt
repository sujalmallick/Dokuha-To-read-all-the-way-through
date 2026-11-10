package ui.screens.details

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ChapterRowItem
import ui.components.PanelCircleIconButton
import ui.components.PanelPillButton
import ui.glass.glassSource
import ui.glass.nonGlassPress
import ui.glass.rememberPanelGlassState
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import ui.theme.TextSecondary

data class TitleDetailsScreen(val mangaUrl: String = "manga/the-last-horizon") : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val scope = rememberCoroutineScope()

        val manga by repository.getMangaDetails(mangaUrl).collectAsState(initial = null)
        val chapters by repository.getChapters(mangaUrl).collectAsState(initial = emptyList())
        val library by repository.getLibrary("All").collectAsState(initial = emptyList())
        val isInLibrary = library.any { it.url == mangaUrl }

        TitleDetailsScreenContent(
            manga = manga,
            chapters = chapters,
            isInLibrary = isInLibrary,
            onToggleLibrary = {
                val currentManga = manga ?: SourceManga(url = mangaUrl, title = "Unknown")
                scope.launch {
                    repository.toggleFavorite(currentManga, !isInLibrary)
                }
            },
            onBack = { navigator.pop() },
            onReadChapter = { chapter ->
                if (manga?.isNovel == true) {
                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name))
                } else {
                    navigator.push(WebtoonReaderScreen(mangaUrl, chapter.url))
                }
            }
        )
    }
}

@Composable
fun TitleDetailsScreenContent(
    manga: SourceManga?,
    chapters: List<SourceChapter>,
    isInLibrary: Boolean = false,
    onToggleLibrary: () -> Unit = {},
    onBack: () -> Unit,
    onReadChapter: (SourceChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var isBookmarked by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val displayManga = manga ?: SourceManga(
        url = "manga/the-last-horizon",
        title = "The Last Horizon",
        description = "In a world where the sky has split, a young man follows the last light to uncover the truth behind a forgotten era...",
        genres = listOf("Action", "Fantasy", "Adventure"),
        status = MangaStatus.ONGOING
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // LAYER 1: Scrolling Content Layer (Glass Sampling Source)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Hero Image Header with Backdrop Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    TitleHeroArtwork(
                        thumbnailUrl = displayManga.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Vertical bottom gradient overlay into dark navy
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.4f to Color.Transparent,
                                    0.75f to PanelBackground.copy(alpha = 0.8f),
                                    1.0f to PanelBackground
                                )
                            )
                    )
                }
            }

            // Main Details Block
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Title
                    Text(
                        text = displayManga.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary text replacing badges: Status & Rating
                    val statusText = if (displayManga.status == MangaStatus.ONGOING) "Ongoing" else "Completed"
                    Text(
                        text = "$statusText · 4.8 (12.4K)",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary text replacing genre chips
                    Text(
                        text = displayManga.genres.joinToString(" · "),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA Button: "Start Reading" (flat ElectricBlue fill, no gradient slop)
                    PanelPillButton(
                        text = "Start Reading",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (chapters.isNotEmpty()) {
                                onReadChapter(chapters.first())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Row (Add to Library, Download, Track)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionGlassButton(
                            icon = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
                            text = if (isInLibrary) "In Library" else "Add to Library",
                            onClick = onToggleLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        ActionGlassButton(
                            icon = Icons.Default.FileDownload,
                            text = "Download",
                            onClick = { /* Download */ },
                            modifier = Modifier.weight(1f)
                        )
                        ActionGlassButton(
                            icon = Icons.Default.FavoriteBorder,
                            text = "Track",
                            onClick = { /* Track */ },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // About Section
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val description = displayManga.description ?: ""
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (description.length > 80) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDescriptionExpanded) "Show Less" else "More",
                            color = ElectricBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                .padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chapters Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters (${chapters.size.takeIf { it > 0 } ?: 210})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Chapter List Items (Quiet translucent fill, non-glass content layer)
            items(chapters) { chapter ->
                val dateStr = when (chapter.name) {
                    "Ch. 210" -> "Jun 15, 2024"
                    "Ch. 209" -> "Jun 8, 2024"
                    "Ch. 208" -> "Jun 1, 2024"
                    "Ch. 207" -> "May 25, 2024"
                    else -> "Recently"
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ChapterRowItem(
                        title = chapter.name,
                        date = dateStr,
                        isRead = chapter.name == "Ch. 208" || chapter.name == "Ch. 207",
                        onClick = { onReadChapter(chapter) },
                        onDownloadClick = {},
                        onOptionsClick = {}
                    )
                }
            }
        }

        // LAYER 2: Floating Top Navigation Bar (Back & Bookmark Overlays)
        // Hard Rule: Sibling overlay to LazyColumn, uses GlassVariant.Clear with 35% scrim over hero art
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelCircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40.dp,
                iconSize = 20.dp,
                glassState = glassState,
                hasDimming = true,
                onClick = onBack
            )
            PanelCircleIconButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                size = 40.dp,
                iconSize = 20.dp,
                tint = if (isBookmarked) ElectricBlue else TextPrimary,
                glassState = glassState,
                hasDimming = true,
                onClick = { isBookmarked = !isBookmarked }
            )
        }
    }
}

/**
 * Action button in details header (Add to Library, Download, Track).
 * Content layer: quiet translucent fill with hairline stroke.
 */
@Composable
fun ActionGlassButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .nonGlassPress(interactionSource)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, GlassBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Procedural stylized hero artwork composition evoking the anime protagonist
 * with dark hair, glowing cyan aura, and crimson cape.
 */
@Composable
fun TitleHeroArtwork(
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val isRemote = !thumbnailUrl.isNullOrBlank() &&
        (thumbnailUrl.startsWith("http://", ignoreCase = true) || thumbnailUrl.startsWith("https://", ignoreCase = true))

    if (isRemote) {
        Box(modifier = modifier) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Dark Crimson & Night Navy backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2E0A12),
                    Color(0xFF1F0D1E),
                    Color(0xFF0F1528),
                    Color(0xFF080B11)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Radial crimson energy flare on the right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE50914).copy(alpha = 0.45f),
                    Color(0xFF990000).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(width * 0.85f, height * 0.35f),
                radius = width * 0.55f
            ),
            center = Offset(width * 0.85f, height * 0.35f),
            radius = width * 0.55f
        )

        // Radial electric blue / cyan magic flare on the left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricBlue.copy(alpha = 0.35f),
                    Color(0xFF0055AA).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(width * 0.15f, height * 0.5f),
                radius = width * 0.45f
            ),
            center = Offset(width * 0.15f, height * 0.5f),
            radius = width * 0.45f
        )

        // Stylized anime character silhouette: Cape
        val capePath = Path().apply {
            moveTo(width * 0.28f, height * 0.32f)
            cubicTo(
                width * 0.15f, height * 0.45f,
                width * 0.05f, height * 0.70f,
                width * 0.18f, height * 0.95f
            )
            lineTo(width * 0.82f, height * 0.95f)
            cubicTo(
                width * 0.92f, height * 0.70f,
                width * 0.85f, height * 0.45f,
                width * 0.72f, height * 0.32f
            )
            close()
        }
        drawPath(
            path = capePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1B0409),
                    Color(0xFF0A0205)
                )
            )
        )

        // Stylized anime character silhouette: Torso & Shoulders
        val bodyPath = Path().apply {
            moveTo(width * 0.36f, height * 0.38f)
            lineTo(width * 0.64f, height * 0.38f)
            lineTo(width * 0.70f, height * 0.85f)
            lineTo(width * 0.30f, height * 0.85f)
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F1424),
                    Color(0xFF050810)
                )
            )
        )

        // Head and Spiky Hair Silhouette
        val hairPath = Path().apply {
            moveTo(width * 0.50f, height * 0.16f)
            lineTo(width * 0.44f, height * 0.20f)
            lineTo(width * 0.42f, height * 0.23f)
            lineTo(width * 0.38f, height * 0.22f)
            lineTo(width * 0.40f, height * 0.27f)
            lineTo(width * 0.44f, height * 0.32f)
            lineTo(width * 0.50f, height * 0.36f)
            lineTo(width * 0.56f, height * 0.32f)
            lineTo(width * 0.60f, height * 0.27f)
            lineTo(width * 0.62f, height * 0.22f)
            lineTo(width * 0.58f, height * 0.23f)
            lineTo(width * 0.56f, height * 0.20f)
            close()
        }
        drawPath(
            path = hairPath,
            color = Color(0xFF060912)
        )

        // Glowing Cyan Eyes
        drawCircle(
            color = Color(0xFF64D2FF),
            radius = 3.5.dp.toPx(),
            center = Offset(width * 0.47f, height * 0.285f)
        )
        drawCircle(
            color = Color(0xFF64D2FF),
            radius = 3.5.dp.toPx(),
            center = Offset(width * 0.53f, height * 0.285f)
        )

        // Cyan Eye Glow
        drawCircle(
            color = Color(0xFF00D1FF).copy(alpha = 0.4f),
            radius = 9.dp.toPx(),
            center = Offset(width * 0.47f, height * 0.285f)
        )
        drawCircle(
            color = Color(0xFF00D1FF).copy(alpha = 0.4f),
            radius = 9.dp.toPx(),
            center = Offset(width * 0.53f, height * 0.285f)
        )
    }
    }
}

@Preview
@Composable
fun TitleDetailsScreenPreview() {
    PanelTheme {
        TitleDetailsScreenContent(
            manga = null,
            chapters = emptyList(),
            onBack = {},
            onReadChapter = {}
        )
    }
}
