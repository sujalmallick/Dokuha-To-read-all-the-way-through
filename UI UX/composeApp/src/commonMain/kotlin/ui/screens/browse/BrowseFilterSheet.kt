package ui.screens.browse

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import core.source.MangaStatus
import data.mock.AvailableBrowseGenres
import data.mock.BrowseFilters
import data.mock.BrowseSortOrder
import ui.components.GlassSegmentedControl
import ui.components.PanelCircleIconButton
import ui.glass.GlassVariant
import ui.glass.PanelGlassState
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSurface
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.TextPrimary
import ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseFilterBottomSheet(
    visible: Boolean,
    currentFilters: BrowseFilters,
    glassState: PanelGlassState,
    onApplyFilters: (BrowseFilters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempSortBy by remember(currentFilters, visible) { mutableStateOf(currentFilters.sortBy) }
    var tempStatus by remember(currentFilters, visible) { mutableStateOf(currentFilters.status) }
    var tempIsNovel by remember(currentFilters, visible) { mutableStateOf(currentFilters.isNovel) }
    var tempGenres by remember(currentFilters, visible) { mutableStateOf(currentFilters.genres) }

    val sortOptions = listOf(
        BrowseSortOrder.POPULARITY,
        BrowseSortOrder.LATEST,
        BrowseSortOrder.ALPHABETICAL
    )

    val statusOptions = listOf(null, MangaStatus.ONGOING, MangaStatus.COMPLETED)
    val statusLabels = listOf("All", "Ongoing", "Completed")

    val typeOptions = listOf(null, false, true)
    val typeLabels = listOf("All", "Manga", "Novels")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
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
                            onClick = { /* Intercept clicks */ }
                        )
                        .glassDimming(alpha = 0.45f, shape = sheetShape)
                        .glassSurface(
                            state = glassState,
                            variant = GlassVariant.Regular,
                            shape = sheetShape
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Sheet Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Filter & Sort",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Refine catalog search results",
                                    fontSize = 13.sp,
                                    color = TextSecondary
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 1: Sort By
                        Text(
                            text = "SORT BY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassSegmentedControl(
                            items = sortOptions,
                            selectedIndex = sortOptions.indexOf(tempSortBy).coerceAtLeast(0),
                            onItemSelected = { tempSortBy = sortOptions[it] },
                            labelProvider = { it.displayName },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 2: Status
                        Text(
                            text = "STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassSegmentedControl(
                            items = statusOptions,
                            selectedIndex = statusOptions.indexOf(tempStatus).coerceAtLeast(0),
                            onItemSelected = { tempStatus = statusOptions[it] },
                            labelProvider = { status ->
                                when (status) {
                                    null -> "All"
                                    MangaStatus.ONGOING -> "Ongoing"
                                    MangaStatus.COMPLETED -> "Completed"
                                    else -> status.name
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 3: Content Format
                        Text(
                            text = "FORMAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassSegmentedControl(
                            items = typeOptions,
                            selectedIndex = typeOptions.indexOf(tempIsNovel).coerceAtLeast(0),
                            onItemSelected = { tempIsNovel = typeOptions[it] },
                            labelProvider = { isNovel ->
                                when (isNovel) {
                                    null -> "All"
                                    false -> "Manga"
                                    true -> "Novels"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 4: Genres Multi-Select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GENRES (${tempGenres.size} selected)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondary
                            )
                            if (tempGenres.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.clickable { tempGenres = emptySet() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AvailableBrowseGenres.forEach { genre ->
                                val isSelected = tempGenres.contains(genre)
                                val chipInteraction = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .clip(CapsuleShape)
                                        .glassPress(chipInteraction)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.55f) else GlassBorder,
                                            shape = CapsuleShape
                                        )
                                        .clickable(
                                            interactionSource = chipInteraction,
                                            indication = null,
                                            onClick = {
                                                tempGenres = if (isSelected) {
                                                    tempGenres - genre
                                                } else {
                                                    tempGenres + genre
                                                }
                                            }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = genre,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // SECTION 5: Bottom Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reset Button
                            val resetInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(CapsuleShape)
                                    .glassPress(resetInteraction)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, GlassBorderStrong, CapsuleShape)
                                    .clickable(
                                        interactionSource = resetInteraction,
                                        indication = null,
                                        onClick = {
                                            tempSortBy = BrowseSortOrder.POPULARITY
                                            tempStatus = null
                                            tempIsNovel = null
                                            tempGenres = emptySet()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Reset",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            // Apply Button
                            val applyInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .clip(CapsuleShape)
                                    .glassPress(applyInteraction)
                                    .background(Color.White)
                                    .clickable(
                                        interactionSource = applyInteraction,
                                        indication = null,
                                        onClick = {
                                            onApplyFilters(
                                                BrowseFilters(
                                                    genres = tempGenres,
                                                    status = tempStatus,
                                                    isNovel = tempIsNovel,
                                                    sortBy = tempSortBy
                                                )
                                            )
                                            onDismiss()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Apply Filters",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF090D15)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
