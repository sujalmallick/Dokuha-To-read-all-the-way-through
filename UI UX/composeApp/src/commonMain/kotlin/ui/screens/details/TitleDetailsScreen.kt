package ui.screens.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ripple
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
import data.mock.FakePanelRepository
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ChapterRowItem
import ui.components.GenreChip
import ui.components.PanelCircleIconButton
import ui.components.RatingBadge
import ui.components.StatusBadge
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.ElectricBlueLight
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

data class TitleDetailsScreen(val mangaUrl: String = "manga/the-last-horizon") : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val manga by repository.getMangaDetails(mangaUrl).collectAsState(initial = null)
        val chapters by repository.getChapters(mangaUrl).collectAsState(initial = emptyList())

        TitleDetailsScreenContent(
            manga = manga,
            chapters = chapters,
            onBack = { navigator.pop() },
            onReadChapter = { chapter ->
                navigator.push(WebtoonReaderScreen(mangaUrl, chapter.url))
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleDetailsScreenContent(
    manga: SourceManga?,
    chapters: List<SourceChapter>,
    onBack: () -> Unit,
    onReadChapter: (SourceChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }
    var isInLibrary by remember { mutableStateOf(false) }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Hero Image Header with Backdrop Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    // Procedural Hero Artwork Canvas (character silhouette with glowing eyes and crimson aura)
                    TitleHeroArtwork(
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

                    // Top Floating Navigation Bar (Back & Bookmark)
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
                            onClick = onBack
                        )
                        PanelCircleIconButton(
                            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            size = 40.dp,
                            iconSize = 20.dp,
                            tint = if (isBookmarked) ElectricBlue else TextPrimary,
                            onClick = { isBookmarked = !isBookmarked }
                        )
                    }
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status and Rating Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            statusText = if (displayManga.status == MangaStatus.ONGOING) "Ongoing" else "Completed",
                            isOngoing = displayManga.status == MangaStatus.ONGOING
                        )
                        RatingBadge(ratingText = "4.8 (12.4K)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genre Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayManga.genres.forEach { genre ->
                            GenreChip(text = genre)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA Button: "Start Reading"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CapsuleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(ElectricBlue, ElectricBlueLight)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White),
                                onClick = {
                                    if (chapters.isNotEmpty()) {
                                        onReadChapter(chapters.first())
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Reading",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Row (Add to Library, Download, Track)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionGlassButton(
                            icon = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
                            text = if (isInLibrary) "In Library" else "Add to Library",
                            onClick = { isInLibrary = !isInLibrary },
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

            // Chapter List Items
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
    }
}

@Composable
fun ActionGlassButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .background(PanelSurfaceGlassHigh)
            .border(1.dp, GlassBorder, CapsuleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
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
 * with dark hair, glowing cyan aura, and crimson cape from option 2.png Screen 3.
 */
@Composable
fun TitleHeroArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Dark Crimson & Night Navy backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2E0A12), // Deep crimson
                    Color(0xFF1F0D1E), // Dark plum
                    Color(0xFF0F1528), // Midnight navy
                    Color(0xFF080B11)  // Base background
                ),
                startY = 0f,
                endY = height
            )
        )

        // Radial crimson energy flare on the right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFDC2626).copy(alpha = 0.45f),
                    Color(0xFF991B1B).copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(width * 0.75f, height * 0.35f),
                radius = width * 0.5f
            ),
            center = Offset(width * 0.75f, height * 0.35f),
            radius = width * 0.5f
        )

        // Character silhouette with dramatic cape
        val capePath = Path().apply {
            moveTo(width * 0.2f, height * 0.9f)
            lineTo(width * 0.38f, height * 0.42f)
            lineTo(width * 0.62f, height * 0.42f)
            lineTo(width * 0.85f, height * 0.9f)
            close()
        }
        drawPath(
            path = capePath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF180A12), Color(0xFF0A0B14))
            )
        )

        // Head and hair shape
        drawCircle(
            color = Color(0xFF0D0F1A),
            radius = 42f,
            center = Offset(width * 0.5f, height * 0.32f)
        )

        // Glowing Blue eye / anime aura (electric-blue accent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFF38BDF8),
                    Color(0xFF0284C7).copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = Offset(width * 0.48f, height * 0.31f),
                radius = 16f
            ),
            center = Offset(width * 0.48f, height * 0.31f),
            radius = 16f
        )
    }
}

@Preview
@Composable
fun TitleDetailsScreenPreview() {
    PanelTheme {
        TitleDetailsScreenContent(
            manga = FakePanelRepository().getMangaDetails("").let { null },
            chapters = emptyList(),
            onBack = {},
            onReadChapter = {}
        )
    }
}
