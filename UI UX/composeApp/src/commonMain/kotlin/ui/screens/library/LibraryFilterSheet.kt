package ui.screens.library

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.PanelCircleIconButton
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryFilterSheet(
    filter: LibraryFilter,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onApply: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var draftFilter by remember(filter, isVisible) { mutableStateOf(filter) }
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .clip(sheetShape)
                        .background(Color(0xF50F1219))
                        .border(1.dp, GlassBorderStrong, sheetShape)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 16.dp)
                    ) {
                        // Top drag handle indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Filter & Sort",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (draftFilter.activeFilterCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${draftFilter.activeFilterCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (draftFilter.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CapsuleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { draftFilter = LibraryFilter() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Reset",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Reset",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                PanelCircleIconButton(
                                    icon = Icons.Default.Close,
                                    size = 34.dp,
                                    iconSize = 16.dp,
                                    onClick = onDismiss
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            color = GlassBorder,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Scrollable filter options
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                // SECTION 1: Sort By & Direction
                                FilterSectionHeader(title = "Sort By")
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LibrarySort.entries.forEach { sortOption ->
                                        FilterChoiceChip(
                                            label = sortOption.label,
                                            isSelected = draftFilter.sortBy == sortOption,
                                            onClick = {
                                                draftFilter = draftFilter.copy(sortBy = sortOption)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChoiceChip(
                                        label = if (draftFilter.sortBy == LibrarySort.TITLE) "A → Z" else "Ascending",
                                        isSelected = draftFilter.sortAscending,
                                        icon = Icons.Default.ArrowUpward,
                                        onClick = { draftFilter = draftFilter.copy(sortAscending = true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChoiceChip(
                                        label = if (draftFilter.sortBy == LibrarySort.TITLE) "Z → A" else "Descending",
                                        isSelected = !draftFilter.sortAscending,
                                        icon = Icons.Default.ArrowDownward,
                                        onClick = { draftFilter = draftFilter.copy(sortAscending = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            item {
                                // SECTION 2: Publication Status
                                FilterSectionHeader(title = "Status")
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MangaStatusFilter.entries.forEach { statusOption ->
                                        FilterChoiceChip(
                                            label = statusOption.label,
                                            isSelected = draftFilter.status == statusOption,
                                            onClick = { draftFilter = draftFilter.copy(status = statusOption) }
                                        )
                                    }
                                }
                            }

                            item {
                                // SECTION 3: Content Format
                                FilterSectionHeader(title = "Format")
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ContentTypeFilter.entries.forEach { typeOption ->
                                        FilterChoiceChip(
                                            label = typeOption.label,
                                            isSelected = draftFilter.contentType == typeOption,
                                            onClick = { draftFilter = draftFilter.copy(contentType = typeOption) }
                                        )
                                    }
                                }
                            }

                            item {
                                // SECTION 4: Reading Progress
                                FilterSectionHeader(title = "Reading Progress")
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ReadStatusFilter.entries.forEach { readOption ->
                                        FilterChoiceChip(
                                            label = readOption.label,
                                            isSelected = draftFilter.readStatus == readOption,
                                            onClick = { draftFilter = draftFilter.copy(readStatus = readOption) }
                                        )
                                    }
                                }
                            }

                            item {
                                // SECTION 5: Offline Downloaded Only
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            draftFilter = draftFilter.copy(downloadedOnly = !draftFilter.downloadedOnly)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Downloaded Only",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Show titles with downloaded chapters",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = draftFilter.downloadedOnly,
                                        onCheckedChange = {
                                            draftFilter = draftFilter.copy(downloadedOnly = it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Action Button: Apply Filter
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(CapsuleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    onApply(draftFilter)
                                    onDismiss()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (draftFilter.isActive) "Apply Filters (${draftFilter.activeFilterCount})" else "Apply",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
}

@Composable
fun FilterChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val shape = CapsuleShape
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActiveFilterPillRow(
    filter: LibraryFilter,
    onUpdateFilter: (LibraryFilter) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!filter.isActive) return

    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filter.status != MangaStatusFilter.ALL) {
            item {
                ActiveFilterChip(
                    label = filter.status.label,
                    onDismiss = { onUpdateFilter(filter.copy(status = MangaStatusFilter.ALL)) }
                )
            }
        }

        if (filter.contentType != ContentTypeFilter.ALL) {
            item {
                ActiveFilterChip(
                    label = filter.contentType.label,
                    onDismiss = { onUpdateFilter(filter.copy(contentType = ContentTypeFilter.ALL)) }
                )
            }
        }

        if (filter.readStatus != ReadStatusFilter.ALL) {
            item {
                ActiveFilterChip(
                    label = filter.readStatus.label,
                    onDismiss = { onUpdateFilter(filter.copy(readStatus = ReadStatusFilter.ALL)) }
                )
            }
        }

        if (filter.downloadedOnly) {
            item {
                ActiveFilterChip(
                    label = "Downloaded",
                    onDismiss = { onUpdateFilter(filter.copy(downloadedOnly = false)) }
                )
            }
        }

        if (filter.sortBy != LibrarySort.TITLE || !filter.sortAscending) {
            item {
                val dirLabel = if (filter.sortAscending) "Asc" else "Desc"
                ActiveFilterChip(
                    label = "${filter.sortBy.label} ($dirLabel)",
                    onDismiss = { onUpdateFilter(filter.copy(sortBy = LibrarySort.TITLE, sortAscending = true)) }
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CapsuleShape)
                    .clickable(onClick = onResetAll)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clear All",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CapsuleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CapsuleShape)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $label filter",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
