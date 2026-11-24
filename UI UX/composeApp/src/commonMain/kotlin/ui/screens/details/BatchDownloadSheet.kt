package ui.screens.details

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.source.SourceChapter
import core.source.SourceManga
import data.download.DownloadManager
import data.download.DownloadStatus
import ui.components.PanelCircleIconButton
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

enum class DownloadPreset {
    NEXT_1,
    NEXT_5,
    NEXT_10,
    ALL_UNREAD,
    CUSTOM
}

@Composable
fun BatchDownloadSheet(
    visible: Boolean,
    manga: SourceManga,
    chapters: List<SourceChapter>,
    downloadManager: DownloadManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storageInfo by downloadManager.storageInfo.collectAsState()
    val queue by downloadManager.queue.collectAsState()
    val completed by downloadManager.completedDownloads.collectAsState()

    var selectedPreset by remember(visible) { mutableStateOf(DownloadPreset.NEXT_5) }
    val customSelectedUrls = remember(visible) { mutableStateListOf<String>() }

    // Helper to determine un-downloaded chapters
    val undownloadedChapters = remember(chapters, queue, completed) {
        chapters.filter { ch ->
            val jobId = "${manga.url}::${ch.url}"
            val isCompleted = completed.any { it.id == jobId }
            val isQueued = queue.any { it.id == jobId }
            !isCompleted && !isQueued
        }
    }

    // Compute active chapters to download based on preset
    val targetChapters = remember(selectedPreset, undownloadedChapters, customSelectedUrls.toList()) {
        when (selectedPreset) {
            DownloadPreset.NEXT_1 -> undownloadedChapters.take(1)
            DownloadPreset.NEXT_5 -> undownloadedChapters.take(5)
            DownloadPreset.NEXT_10 -> undownloadedChapters.take(10)
            DownloadPreset.ALL_UNREAD -> undownloadedChapters
            DownloadPreset.CUSTOM -> chapters.filter { it.url in customSelectedUrls }
        }
    }

    val estimatedMb = targetChapters.size * 24
    val freeGb = (storageInfo.totalGb - storageInfo.usedGb).coerceAtLeast(0f)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Intercept clicks so sheet doesn't close
                        )
                        .clip(sheetShape)
                        .background(Color(0xF50F1219))
                        .border(1.dp, GlassBorderStrong, sheetShape)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Sheet Drag Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Header with Title & Dismiss Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Batch Download",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = manga.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            PanelCircleIconButton(
                                icon = Icons.Default.Close,
                                size = 36.dp,
                                iconSize = 18.dp,
                                backgroundColor = Color.White.copy(alpha = 0.08f),
                                borderColor = Color.White.copy(alpha = 0.16f),
                                onClick = onDismiss
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Storage Metrics Chip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.75.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Est. Download: ~${estimatedMb} MB",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Free: ${(kotlin.math.round(freeGb * 10) / 10.0)} GB",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preset Selector Grid / List
                        Text(
                            text = "Download Preset",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PresetSelectionRow(
                                title = "Next 1 Chapter",
                                subtitle = undownloadedChapters.firstOrNull()?.name ?: "All up to date",
                                sizeEstimate = "~24 MB",
                                isSelected = selectedPreset == DownloadPreset.NEXT_1,
                                enabled = undownloadedChapters.isNotEmpty(),
                                onClick = { selectedPreset = DownloadPreset.NEXT_1 }
                            )

                            PresetSelectionRow(
                                title = "Next 5 Chapters",
                                subtitle = if (undownloadedChapters.size >= 5) {
                                    "${undownloadedChapters.first().name} - ${undownloadedChapters[4].name}"
                                } else "${undownloadedChapters.size} remaining",
                                sizeEstimate = "~${(undownloadedChapters.take(5).size * 24)} MB",
                                isSelected = selectedPreset == DownloadPreset.NEXT_5,
                                enabled = undownloadedChapters.isNotEmpty(),
                                onClick = { selectedPreset = DownloadPreset.NEXT_5 }
                            )

                            PresetSelectionRow(
                                title = "Next 10 Chapters",
                                subtitle = if (undownloadedChapters.size >= 10) {
                                    "${undownloadedChapters.first().name} - ${undownloadedChapters[9].name}"
                                } else "${undownloadedChapters.size} remaining",
                                sizeEstimate = "~${(undownloadedChapters.take(10).size * 24)} MB",
                                isSelected = selectedPreset == DownloadPreset.NEXT_10,
                                enabled = undownloadedChapters.isNotEmpty(),
                                onClick = { selectedPreset = DownloadPreset.NEXT_10 }
                            )

                            PresetSelectionRow(
                                title = "All Unread Chapters",
                                subtitle = "${undownloadedChapters.size} chapters un-downloaded",
                                sizeEstimate = "~${undownloadedChapters.size * 24} MB",
                                isSelected = selectedPreset == DownloadPreset.ALL_UNREAD,
                                enabled = undownloadedChapters.isNotEmpty(),
                                onClick = { selectedPreset = DownloadPreset.ALL_UNREAD }
                            )

                            PresetSelectionRow(
                                title = "Custom Selection",
                                subtitle = "${customSelectedUrls.size} chapters chosen",
                                sizeEstimate = "~${customSelectedUrls.size * 24} MB",
                                isSelected = selectedPreset == DownloadPreset.CUSTOM,
                                enabled = true,
                                onClick = { selectedPreset = DownloadPreset.CUSTOM }
                            )
                        }

                        // Custom Selection Chapter List (if CUSTOM chosen)
                        if (selectedPreset == DownloadPreset.CUSTOM) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                items(chapters) { ch ->
                                    val isSelected = ch.url in customSelectedUrls
                                    val isDownloaded = downloadManager.isChapterDownloaded(manga.url, ch.url)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isDownloaded) {
                                                if (isSelected) customSelectedUrls.remove(ch.url)
                                                else customSelectedUrls.add(ch.url)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val primaryColor = MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected || isDownloaded) primaryColor
                                                    else Color.White.copy(alpha = 0.08f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected || isDownloaded) primaryColor else GlassBorder,
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected || isDownloaded) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDownloaded) TextMuted else TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (isDownloaded) {
                                            Text(
                                                text = "Downloaded",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = primaryColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Button
                        val buttonText = if (targetChapters.isEmpty()) {
                            if (undownloadedChapters.isEmpty()) "All Chapters Downloaded" else "Select Chapters"
                        } else {
                            "Download ${targetChapters.size} Chapter${if (targetChapters.size > 1) "s" else ""}"
                        }

                        val canDownload = targetChapters.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (canDownload) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White,
                                                Color(0xFFE2E8F0)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.2f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                )
                                .clickable(enabled = canDownload) {
                                    downloadManager.enqueue(manga, targetChapters)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (canDownload) Color.Black else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buttonText,
                                    color = if (canDownload) Color.Black else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetSelectionRow(
    title: String,
    subtitle: String,
    sizeEstimate: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.03f)
            )
            .border(
                width = if (isSelected) 1.dp else 0.75.dp,
                color = if (isSelected) primaryColor else GlassBorder,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio circle indicator
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) primaryColor else GlassBorderStrong,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (enabled) TextPrimary else TextMuted
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = sizeEstimate,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) primaryColor else TextTertiary
        )
    }
}
