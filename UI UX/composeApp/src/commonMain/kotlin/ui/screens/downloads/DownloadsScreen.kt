package ui.screens.downloads

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.MockDownloadItem
import data.mock.MockStorageInfo
import data.mock.PanelRepository
import kotlin.math.roundToInt
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.DownloadQueueCard
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelTab
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

class DownloadsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val storageInfo by repository.getStorageInfo().collectAsState(initial = MockStorageInfo())
        val initialDownloads by repository.getDownloads().collectAsState(initial = emptyList())

        DownloadsScreenContent(
            storageInfo = storageInfo,
            initialDownloads = initialDownloads,
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> {} // Current
                }
            }
        )
    }
}

@Composable
fun DownloadsScreenContent(
    storageInfo: MockStorageInfo,
    initialDownloads: List<MockDownloadItem>,
    onOpenManga: (SourceManga) -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()

    val downloadItems = remember(initialDownloads) {
        mutableStateListOf<MockDownloadItem>().apply {
            addAll(
                if (initialDownloads.isNotEmpty()) initialDownloads
                else FakePanelRepository().getDownloads().let { emptyList() }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Subtle ambient liquid glow orbs behind content for realistic glass refraction
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x18FFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1038BDF8), // ultra-faint liquid cyan refraction
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.60f),
                    radius = size.width * 0.8f
                )
            )
        }

        // LAYER 1: Scrollable Content Layer (marked with glassSource)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 110.dp
            )
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    PanelCircleIconButton(
                        icon = Icons.Default.MoreHoriz,
                        size = 40.dp,
                        iconSize = 20.dp,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.18f),
                        onClick = { /* Menu */ }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Minimal Aesthetic Storage Breakdown Card (Liquid Glass)
            item {
                AestheticStorageWidget(storageInfo = storageInfo)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // "Downloading (3)" Section Header
            item {
                Text(
                    text = "Downloading (${downloadItems.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Download Queue Items (Liquid Glass cards)
            itemsIndexed(downloadItems) { index, item ->
                val downloadedMb = item.downloadedBytes / (1024 * 1024)
                val totalMb = item.totalBytes / (1024 * 1024)

                DownloadQueueCard(
                    title = item.manga.title,
                    chapter = item.chapter.name,
                    progress = item.progress,
                    downloadedMb = downloadedMb,
                    totalMb = totalMb,
                    etaMinutes = item.etaMinutes,
                    isPaused = item.isPaused,
                    gradientIndex = index,
                    onPauseToggle = {
                        downloadItems[index] = item.copy(isPaused = !item.isPaused)
                    },
                    onClick = { onOpenManga(item.manga) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // LAYER 2: Floating Liquid Glass Bottom Navigation Bar (Sibling Overlay)
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Minimal, aesthetic storage breakdown widget with Liquid Glass container,
 * horizontal segmented liquid capsule bar, and refined typography.
 */
@Composable
fun AestheticStorageWidget(
    storageInfo: MockStorageInfo,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val usedGb = storageInfo.usedGb
    val totalGb = storageInfo.totalGb
    val freeGb = (totalGb - usedGb).coerceAtLeast(0f)
    val totalMb = totalGb * 1024f
    val mangaFraction = (storageInfo.mangaMb / totalMb).coerceIn(0f, 1f)
    val manhwaFraction = (storageInfo.manhwaMb / totalMb).coerceIn(0f, 1f)
    val novelsFraction = (storageInfo.novelsMb / totalMb).coerceIn(0f, 1f)
    val othersFraction = (storageInfo.othersMb / totalMb).coerceIn(0f, 1f)
    val freeFraction = (1f - (mangaFraction + manhwaFraction + novelsFraction + othersFraction)).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Storage label & Free Space Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${usedGb} GB used of ${totalGb.toInt()} GB",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Minimal Frosted Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${(freeGb * 10).roundToInt() / 10f} GB Free",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment horizontal liquid capsule bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Manga Segment (Crisp Luxe Platinum White)
                    if (mangaFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(mangaFraction.coerceAtLeast(0.04f))
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                    // Manhwa Segment (Titanium Slate)
                    if (manhwaFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(manhwaFraction.coerceAtLeast(0.03f))
                                .fillMaxHeight()
                                .background(Color(0xFF94A3B8))
                        )
                    }
                    // Novels Segment (Muted Titanium)
                    if (novelsFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .weight(novelsFraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(Color(0xFF64748B))
                        )
                    }
                    // Free space remaining
                    Box(
                        modifier = Modifier
                            .weight(freeFraction.coerceAtLeast(0.05f))
                            .fillMaxHeight()
                            .background(Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimal Legend Row (Free space is displayed in the top right pill badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendItem(
                    color = Color.White,
                    label = "Manga ${(storageInfo.mangaMb / 1024f * 10).roundToInt() / 10f} GB"
                )
                StorageLegendItem(
                    color = Color(0xFF94A3B8),
                    label = "Manhwa ${storageInfo.manhwaMb.toInt()} MB"
                )
                StorageLegendItem(
                    color = Color(0xFF64748B),
                    label = "Novels ${storageInfo.novelsMb.toInt()} MB"
                )
            }
        }
    }
}

@Composable
fun StorageLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview
@Composable
fun DownloadsScreenPreview() {
    PanelTheme {
        DownloadsScreenContent(
            storageInfo = MockStorageInfo(),
            initialDownloads = emptyList(),
            onOpenManga = {},
            onTabSelected = {}
        )
    }
}
