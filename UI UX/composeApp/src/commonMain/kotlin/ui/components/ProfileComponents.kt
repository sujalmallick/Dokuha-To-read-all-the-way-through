package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.profile.DayReadingActivity
import data.profile.ReadingStats
import data.profile.TrackerAccount
import data.profile.UserProfile
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.theme.BottomSheetShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

/**
 * Large avatar (initials on solid color, no gradient) with a circular glass edit button.
 */
@Composable
fun ProfileHeader(
    profile: UserProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with edit badge
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Solid color avatar circle with initials
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(profile.avatarColorHex))
                    .border(2.dp, GlassBorderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.initials,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Circular lens glass edit button
            PanelCircleIconButton(
                icon = Icons.Default.Edit,
                size = 30.dp,
                iconSize = 14.dp,
                onClick = onEditClick,
                backgroundColor = PanelSurfaceGlassHigh,
                borderColor = GlassBorderStrong
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = profile.memberSince,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Stats displayed as a plain row of 3 numbers with small labels beneath.
 * (Chapters read, Titles, Hours). No cards, no pills, no icons per stat.
 */
@Composable
fun ProfileStatsRow(
    stats: ReadingStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            value = stats.chaptersRead.toString(),
            label = "Chapters read"
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(GlassBorder)
        )
        StatItem(
            value = stats.titlesCount.toString(),
            label = "Titles"
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(GlassBorder)
        )
        StatItem(
            value = "${stats.hoursRead.toInt()}h",
            label = "Hours"
        )
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * Flat weekly reading activity bar chart drawn in Canvas.
 * Single accent color, value label only on selected bar, no glow, no gradient fill.
 */
@Composable
fun ActivityBarChart(
    weeklyActivity: List<DayReadingActivity>,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricBlue
) {
    var selectedIndex by remember { mutableIntStateOf(weeklyActivity.size - 2) } // default Saturday selected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WEEKLY READING",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = TextSecondary
            )
            val selectedActivity = weeklyActivity.getOrNull(selectedIndex)
            if (selectedActivity != null) {
                Text(
                    text = "${selectedActivity.minutes} mins",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Flat Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(weeklyActivity) {
                        detectTapGestures { offset ->
                            val barSlotWidth = size.width / weeklyActivity.size
                            val tappedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, weeklyActivity.size - 1)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                val maxMinutes = weeklyActivity.maxOfOrNull { it.minutes }?.coerceAtLeast(60) ?: 120
                val barWidth = 24.dp.toPx()
                val totalWidth = size.width
                val slotWidth = totalWidth / weeklyActivity.size
                val chartHeight = size.height - 24.dp.toPx()

                weeklyActivity.forEachIndexed { index, activity ->
                    val isSelected = index == selectedIndex
                    val barHeight = (activity.minutes.toFloat() / maxMinutes) * chartHeight
                    val x = index * slotWidth + (slotWidth - barWidth) / 2
                    val y = chartHeight - barHeight

                    // Flat bar (single accent color, slightly dimmer when not selected)
                    val barColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.35f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        // Days labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weeklyActivity.forEachIndexed { index, activity ->
                val isSelected = index == selectedIndex
                Text(
                    text = activity.dayLabel,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextMuted
                )
            }
        }
    }
}

/**
 * Service tracker row: service name, secondary text, and plain accent text button ("Connect" / "Disconnect").
 * No status badge.
 */
@Composable
fun TrackerRow(
    tracker: TrackerAccount,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tracker.service.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (tracker.isConnected) "Connected as ${tracker.username}" else "Not connected",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Text(
            text = if (tracker.isConnected) "Disconnect" else "Connect",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ElectricBlue,
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Edit profile modal sheet: Name field and avatar solid color choice only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    profile: UserProfile,
    onSave: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedColor by remember { mutableStateOf(profile.avatarColorHex) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val colorOptions = listOf(
        0xFF007AFF, // Electric Blue
        0xFFA855F7, // Amethyst
        0xFFEF4444, // Ruby
        0xFF10B981, // Emerald
        0xFFF59E0B, // Amber
        0xFF3B82F6  // Cobalt
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = PanelSurfaceGlassHigh.copy(alpha = 0.96f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = ElectricBlue,
                    unfocusedLabelColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Avatar Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Avatar Color choices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colorOptions.forEach { colorHex ->
                    val isSelected = selectedColor == colorHex
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else GlassBorder,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = colorHex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue,
                    modifier = Modifier
                        .clickable {
                            onSave(name, selectedColor)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
fun ProfileComponentsPreview() {
    PanelTheme {
        Column(
            modifier = Modifier
                .background(PanelBackground)
                .padding(16.dp)
        ) {
            ProfileHeader(
                profile = UserProfile(),
                onEditClick = {}
            )
            ProfileStatsRow(stats = ReadingStats())
        }
    }
}
