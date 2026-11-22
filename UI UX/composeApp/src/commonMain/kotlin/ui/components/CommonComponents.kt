package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import data.download.DownloadStatus
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassVariant
import ui.glass.PanelGlassState
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSurface
import ui.glass.nonGlassPress
import ui.theme.CapsuleShape
import ui.theme.CoverGradients
import ui.theme.CoverImageShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelSurfaceGlass
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelSurfaceSubtle
import ui.theme.PanelTheme
import ui.theme.StarGold
import ui.theme.StatusOngoingGreen
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

@Composable
fun PlaceholderCover(
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = CoverImageShape,
    subtitle: String? = null,
    gradientIndex: Int = 0
) {
    val gradientColors = CoverGradients[gradientIndex.mod(CoverGradients.size)]
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
            .border(1.dp, GlassBorderStrong, shape),
        contentAlignment = Alignment.BottomStart
    ) {
        // Subtle top atmospheric glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        radius = 300f
                    )
                )
        )
        
        // Dark gradient overlay at bottom for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    statusText: String = "Ongoing",
    isOngoing: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CapsuleShape)
            .background(PanelSurfaceGlassHigh)
            .border(1.dp, GlassBorder, CapsuleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isOngoing) StatusOngoingGreen else TextMuted)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RatingBadge(
    ratingText: String = "4.8 (12.4K)",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CapsuleShape)
            .background(PanelSurfaceGlassHigh)
            .border(1.dp, GlassBorder, CapsuleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = StarGold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = ratingText,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CategoryPillRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEditCategoriesClick: (() -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                        shape = CapsuleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCategorySelected(category) }
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color(0xFF090D15) else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        if (onEditCategoriesClick != null) {
            item {
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CapsuleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onEditCategoriesClick
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Edit",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaCoverCard(
    title: String,
    subtitle: String,
    gradientIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White),
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White),
            onClick = onClick
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(clickModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(CoverImageShape)
                .background(
                    brush = Brush.verticalGradient(
                        CoverGradients[gradientIndex.mod(CoverGradients.size)]
                    )
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.22f),
                    shape = CoverImageShape
                )
        ) {
            val isRemoteImage = !thumbnailUrl.isNullOrBlank() &&
                (thumbnailUrl.startsWith("http://", ignoreCase = true) || thumbnailUrl.startsWith("https://", ignoreCase = true))

            if (isRemoteImage) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Subtle atmosphere
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                radius = 250f
                            )
                        )
                )
                // Title initial watermark in center
                Text(
                    text = title.take(1),
                    color = Color.White.copy(alpha = 0.14f),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Selection indicator badge
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.55f))
                        .border(
                            1.dp,
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF090D15),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 13.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GenreChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .background(PanelSurfaceGlassHigh)
            .border(1.dp, GlassBorder, CapsuleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ChapterRowItem(
    title: String,
    date: String,
    isRead: Boolean = false,
    isDownloaded: Boolean = false,
    downloadStatus: DownloadStatus? = null,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCompleted = isDownloaded || downloadStatus == DownloadStatus.COMPLETED
    val isDownloading = downloadStatus == DownloadStatus.DOWNLOADING
    val isQueued = downloadStatus == DownloadStatus.QUEUED
    val isPaused = downloadStatus == DownloadStatus.PAUSED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Read indicator circle
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (isRead) ElectricBlue else GlassBorderStrong)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isRead) TextSecondary else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelCircleIconButton(
                icon = when {
                    isCompleted -> Icons.Default.Check
                    isPaused -> Icons.Default.PlayArrow
                    isDownloading -> Icons.Default.Pause
                    else -> Icons.Default.FileDownload
                },
                size = 36.dp,
                iconSize = 18.dp,
                tint = when {
                    isCompleted -> ElectricBlue
                    isDownloading -> ElectricBlue
                    isQueued -> TextSecondary
                    else -> TextPrimary
                },
                backgroundColor = if (isCompleted) ElectricBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                borderColor = if (isCompleted) ElectricBlue.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.18f),
                onClick = onDownloadClick
            )
            PanelCircleIconButton(
                icon = Icons.Default.MoreVert,
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onOptionsClick
            )
        }
    }
}

@Composable
fun PanelProgressBar(
    progress: Float, // 0f..1f
    modifier: Modifier = Modifier,
    fillColor: Color = ElectricBlue,
    trackColor: Color = PanelSurfaceSubtle,
    height: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CapsuleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CapsuleShape)
                .background(fillColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = ElectricBlue,
            activeTrackColor = ElectricBlue,
            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier
    )
}

@Composable
fun PanelSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ElectricBlue,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = PanelSurfaceSubtle,
            uncheckedBorderColor = GlassBorder
        ),
        modifier = modifier
    )
}

@Composable
fun ReaderModeTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) ElectricBlue else PanelSurfaceGlassHigh)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.3f) else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) TextPrimary else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PanelSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search titles, authors, genres...",
    modifier: Modifier = Modifier,
    glassState: PanelGlassState? = null
) {
    val shape = CapsuleShape
    val surfaceModifier = if (glassState != null) {
        Modifier
            .glassDimming(alpha = 0.35f, shape = shape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = shape
            )
    } else {
        Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, shape)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(surfaceModifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SourceCard(
    name: String,
    type: String,
    languages: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPress(interactionSource)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source logo avatar in liquid glass frame
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                avatarColor.copy(alpha = 0.22f),
                                avatarColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                avatarColor.copy(alpha = 0.30f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1),
                    color = avatarColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name and type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Plain secondary text replacing language tags
            Text(
                text = languages.joinToString(" · "),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExtensionItemCard(
    name: String,
    version: String,
    isInstalled: Boolean,
    hasUpdate: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String? = null,
    avatarColor: Color = Color.White,
    onDeleteClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                // Long-press on the card body opens the repo picker; clip is applied first
                // so the gesture stays bounded, and onClick is a no-op so the individual
                // Install / Update buttons still handle primary taps on their own.
                if (onLongClick != null)
                    Modifier
                        .clip(shape)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                else
                    Modifier.clip(shape)
            )
            .glassPress(interactionSource)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Extension icon avatar in liquid glass frame
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                avatarColor.copy(alpha = 0.22f),
                                avatarColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                avatarColor.copy(alpha = 0.30f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(2).uppercase(),
                    color = avatarColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name & Version
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!lang.isNullOrBlank() && lang != "ALL") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricBlue.copy(alpha = 0.15f))
                                .border(0.5.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = lang.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = version,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Phase C: Professional Option A actions
            when {
                hasUpdate -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Update" liquid glass mini capsule button
                        val actionInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .glassPress(actionInteraction)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), CapsuleShape)
                                .clickable(
                                    interactionSource = actionInteraction,
                                    indication = null,
                                    onClick = onActionClick
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Update",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (onDeleteClick != null) {
                            val deleteInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .glassPress(deleteInteraction)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.30f), CircleShape)
                                    .clickable(
                                        interactionSource = deleteInteraction,
                                        indication = null,
                                        onClick = onDeleteClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Uninstall $name",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                isInstalled -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Installed" subtle pill
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CapsuleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Installed",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (onDeleteClick != null) {
                            val deleteInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .glassPress(deleteInteraction)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.30f), CircleShape)
                                    .clickable(
                                        interactionSource = deleteInteraction,
                                        indication = null,
                                        onClick = onDeleteClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Uninstall $name",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                else -> {
                    // "Install" Crisp Luxe White button with dark ink
                    val actionInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .glassPress(actionInteraction)
                            .background(Color.White)
                            .clickable(
                                interactionSource = actionInteraction,
                                indication = null,
                                onClick = onActionClick
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Install",
                            color = Color(0xFF090D15),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateItemCard(
    title: String,
    chapter: String,
    gradientIndex: Int,
    onResume: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit = {},
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPress(interactionSource)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with gradient artwork placeholder
            val coverShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 58.dp)
                    .clip(coverShape)
                    .background(
                        brush = Brush.verticalGradient(
                            CoverGradients[gradientIndex.mod(CoverGradients.size)]
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = coverShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val isRemoteImage = !thumbnailUrl.isNullOrBlank() &&
                    (thumbnailUrl.startsWith("http://", ignoreCase = true) || thumbnailUrl.startsWith("https://", ignoreCase = true))

                if (isRemoteImage) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = title.take(1),
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Chapter Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Download icon button & Resume Luxe White CTA
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelCircleIconButton(
                    icon = Icons.Default.FileDownload,
                    size = 36.dp,
                    iconSize = 18.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.18f),
                    onClick = onDownloadClick
                )

                val resumeInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .glassPress(resumeInteraction)
                        .clickable(
                            interactionSource = resumeInteraction,
                            indication = null,
                            onClick = onResume
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Resume",
                        color = Color(0xFF090D15),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = core.platform.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0L)
    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7L -> "${days}d ago"
        days < 30L -> "${days / 7L}w ago"
        else -> "${days / 30L}mo ago"
    }
}

@Composable
fun HistoryItemCard(
    title: String,
    chapter: String,
    timeText: String,
    progressText: String?,
    gradientIndex: Int,
    onResume: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPress(interactionSource)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 62.dp)
                    .clip(coverShape)
                    .background(
                        brush = Brush.verticalGradient(
                            CoverGradients[gradientIndex.mod(CoverGradients.size)]
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = coverShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val isRemoteImage = !thumbnailUrl.isNullOrBlank() &&
                    (thumbnailUrl.startsWith("http://", ignoreCase = true) || thumbnailUrl.startsWith("https://", ignoreCase = true))

                if (isRemoteImage) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = title.take(1),
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!progressText.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E88E5).copy(alpha = 0.22f))
                                .border(0.5.dp, Color(0xFF1E88E5).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = progressText,
                                color = Color(0xFF90CAF9),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelCircleIconButton(
                    icon = Icons.Default.DeleteOutline,
                    size = 36.dp,
                    iconSize = 18.dp,
                    backgroundColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                    borderColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                    tint = Color(0xFFFF6B6B),
                    onClick = onDelete
                )

                val resumeInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .glassPress(resumeInteraction)
                        .clickable(
                            interactionSource = resumeInteraction,
                            indication = null,
                            onClick = onResume
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Resume",
                        color = Color(0xFF090D15),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueReadingCompactCard(
    manga: core.source.SourceManga,
    chapter: core.source.SourceChapter,
    progressText: String,
    onResume: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .width(170.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2330)),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = manga.thumbnailUrl
                if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = manga.title.take(1),
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onResume),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF090D15),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = manga.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chapter.name,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (progressText.isNotBlank()) {
                    Text(
                        text = progressText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageDonutChart(
    usedGb: Float,
    totalGb: Float,
    mangaMb: Float,
    manhwaMb: Float,
    novelsMb: Float,
    othersMb: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background inactive track
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Scaled sweeps for visual elegance
            val totalMb = totalGb * 1024f
            val mangaSweep = ((mangaMb / totalMb) * 360f * 6.5f).coerceAtMost(220f)
            val manhwaSweep = ((manhwaMb / totalMb) * 360f * 6.5f).coerceAtMost(80f)
            val novelsSweep = ((novelsMb / totalMb) * 360f * 6.5f).coerceAtMost(40f)

            var startAngle = -90f
            // Manga arc (Electric Blue)
            drawArc(
                color = ElectricBlue,
                startAngle = startAngle,
                sweepAngle = mangaSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += mangaSweep

            // Manhwa arc (Cyan)
            drawArc(
                color = Color(0xFF06B6D4),
                startAngle = startAngle,
                sweepAngle = manhwaSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += manhwaSweep

            // Novels arc (Purple)
            drawArc(
                color = Color(0xFFA855F7),
                startAngle = startAngle,
                sweepAngle = novelsSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$usedGb GB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "used",
                fontSize = 9.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun DownloadQueueCard(
    title: String,
    chapter: String,
    progress: Float,
    downloadedMb: Long,
    totalMb: Long,
    etaMinutes: Int,
    isPaused: Boolean,
    gradientIndex: Int,
    onPauseToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    onCancel: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPress(interactionSource)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with gradient artwork placeholder
            val coverShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .clip(coverShape)
                    .background(
                        brush = Brush.verticalGradient(
                            CoverGradients[gradientIndex.mod(CoverGradients.size)]
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = coverShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar with percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PanelProgressBar(
                        progress = progress,
                        modifier = Modifier.weight(1f),
                        fillColor = Color.White,
                        trackColor = Color.White.copy(alpha = 0.12f),
                        height = 4.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status text
                Text(
                    text = statusText ?: "$downloadedMb MB of $totalMb MB • $etaMinutes mins left",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelCircleIconButton(
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    size = 36.dp,
                    iconSize = 18.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.18f),
                    onClick = onPauseToggle
                )
                if (onCancel != null) {
                    PanelCircleIconButton(
                        icon = Icons.Default.Close,
                        size = 36.dp,
                        iconSize = 16.dp,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.18f),
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CommonComponentsPreview() {
    PanelTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryPillRow(
                categories = listOf("All", "Manga", "Manhwa", "Novels"),
                selectedCategory = "All",
                onCategorySelected = {}
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge()
                RatingBadge()
            }
            PanelProgressBar(progress = 0.68f)
            Box(modifier = Modifier.width(110.dp).aspectRatio(0.7f)) {
                PlaceholderCover(
                    title = "Solo Leveling",
                    subtitle = "Ch. 179"
                )
            }
        }
    }
}
