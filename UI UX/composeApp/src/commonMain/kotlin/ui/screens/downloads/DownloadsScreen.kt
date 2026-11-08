package ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.DownloadQueueCard
import ui.components.GlassContainer
import ui.components.PanelBottomNavBar
import ui.components.PanelCircleIconButton
import ui.components.PanelTab
import ui.components.StorageDonutChart
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.library.LibraryScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

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
    val hazeState = remember { HazeState() }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 100.dp
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
                        onClick = { /* Menu */ }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Storage Breakdown Card matching option 2.png
            item {
                GlassContainer(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = PanelSurfaceGlassHigh,
                    borderColor = GlassBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Donut Storage Chart
                        StorageDonutChart(
                            usedGb = storageInfo.usedGb,
                            totalGb = storageInfo.totalGb,
                            mangaMb = storageInfo.mangaMb,
                            manhwaMb = storageInfo.manhwaMb,
                            novelsMb = storageInfo.novelsMb,
                            othersMb = storageInfo.othersMb
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Storage Legend Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${storageInfo.usedGb} GB of ${storageInfo.totalGb.toInt()} GB used",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Breakdown Dots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageLegendItem(color = ElectricBlue, label = "Manga 1.6 GB")
                                StorageLegendItem(color = Color(0xFF06B6D4), label = "Manhwa 512 MB")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageLegendItem(color = Color(0xFFA855F7), label = "Novels 208 MB")
                                StorageLegendItem(color = Color(0xFF64748B), label = "Others")
                            }
                        }
                    }
                }
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

            // Download Queue Items
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

        // Floating Frosted Glass Bottom Navigation Bar
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
