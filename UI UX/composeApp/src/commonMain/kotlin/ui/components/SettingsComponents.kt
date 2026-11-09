package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassVariant
import ui.glass.glassSurface
import ui.motion.rememberControlThumbSpring
import ui.motion.rememberSegmentedIndicatorSpring
import ui.theme.BadgeRed
import ui.theme.BottomSheetShape
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelSurfaceSubtle
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

// Inset grouped corner radius (concentric 22dp)
val SettingsGroupShape = RoundedCornerShape(22.dp)
val SettingsGroupFill = Color.White.copy(alpha = 0.07f)

/**
 * Inset grouped container with quiet translucent fill and concentric corner radius.
 * Content layer: NO glass.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsGroupShape)
            .background(SettingsGroupFill)
            .border(1.dp, GlassBorder, SettingsGroupShape)
    ) {
        content()
    }
}

/**
 * Hairline separator inset from the leading edge.
 */
@Composable
fun SettingsSeparator(
    modifier: Modifier = Modifier,
    startIndent: Dp = 16.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent, end = 16.dp),
        thickness = 0.5.dp,
        color = GlassBorder
    )
}

/**
 * Small, clean section header (no chips, no pills, no background).
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = TextSecondary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

/**
 * Base row wrapper with press highlight fade (no scale on rows).
 */
@Composable
private fun BaseSettingsRow(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White.copy(alpha = 0.1f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(contentPadding)
    ) {
        content()
    }
}

object SettingsRow {

    /**
     * Navigation row variant: Title, optional subtitle, trailing secondary value, chevron.
     */
    @Composable
    fun Navigation(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        value: String? = null,
        icon: ImageVector? = null
    ) {
        BaseSettingsRow(onClick = onClick, modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value != null) {
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    /**
     * Toggle row variant: Knob moves with a spring, becomes lens glass while pressed, light haptic on change.
     */
    @Composable
    fun Toggle(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        enabled: Boolean = true,
        disabledNotice: String? = null
    ) {
        val haptic = LocalHapticFeedback.current

        BaseSettingsRow(
            onClick = if (enabled) {
                {
                    triggerLightHaptic(haptic)
                    onCheckedChange(!checked)
                }
            } else null,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) TextPrimary else TextMuted
                    )
                    if (subtitle != null || disabledNotice != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = disabledNotice ?: subtitle.orEmpty(),
                            fontSize = 12.sp,
                            color = if (disabledNotice != null) TextMuted else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                GlassSwitch(
                    checked = checked,
                    onCheckedChange = {
                        if (enabled) {
                            triggerLightHaptic(haptic)
                            onCheckedChange(it)
                        }
                    },
                    enabled = enabled
                )
            }
        }
    }

    /**
     * Value row variant: Plain secondary text trailing.
     */
    @Composable
    fun Value(
        title: String,
        value: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        BaseSettingsRow(onClick = onClick, modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }

    /**
     * Slider row variant: Numeric label + slider thumb turns lens glass while dragged.
     */
    @Composable
    fun Slider(
        title: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        modifier: Modifier = Modifier,
        valueDisplay: String = "${(value * 100).toInt()}%"
    ) {
        BaseSettingsRow(onClick = null, modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = valueDisplay,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    /**
     * Action row variant: Primary accent text.
     */
    @Composable
    fun Action(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        secondaryText: String? = null
    ) {
        BaseSettingsRow(onClick = onClick, modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                }
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    /**
     * Destructive row variant: Warning red text.
     */
    @Composable
    fun Destructive(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null
    ) {
        BaseSettingsRow(onClick = onClick, modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BadgeRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BadgeRed
                )
            }
        }
    }
}

/**
 * Tactile switch whose knob moves with a spring and transforms into lens glass while pressed.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    val switchWidth = 50.dp
    val switchHeight = 28.dp
    val thumbSize = 22.dp

    val springSpec = rememberControlThumbSpring<Dp>()
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (switchWidth - thumbSize - 3.dp) else 3.dp,
        animationSpec = springSpec
    )

    val trackColor = when {
        !enabled -> PanelSurfaceSubtle
        checked -> ElectricBlue
        else -> Color.White.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .size(width = switchWidth, height = switchHeight)
            .clip(CapsuleShape)
            .background(trackColor)
            .border(1.dp, GlassBorder, CapsuleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onCheckedChange(!checked)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Switch Knob
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (isPressed) Color.White.copy(alpha = 0.9f) else Color.White)
                .border(0.5.dp, if (isPressed) Color.White else GlassBorderStrong, CircleShape)
        )
    }
}

/**
 * Segmented control with a sliding lens glass indicator animated with a strict spring (no overshoot).
 */
@Composable
fun <T> GlassSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() }
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CapsuleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, GlassBorder, CapsuleShape)
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / items.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = rememberSegmentedIndicatorSpring()
        )

        // Sliding Lens Glass Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(34.dp)
                .glassSurface(
                    shape = CapsuleShape
                )
        )

        // Labels row
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (index != selectedIndex) {
                                    triggerLightHaptic(haptic)
                                    onItemSelected(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Top bar that fades in lens glass as the large title collapses on scroll.
 */
@Composable
fun GlassCollapsingTopBar(
    title: String,
    onBack: () -> Unit,
    scrollOffset: Float, // 0f (expanded) .. 1f (collapsed)
    modifier: Modifier = Modifier,
    collapseThreshold: Float = 60f,
    trailingActions: @Composable () -> Unit = {}
) {
    val collapseProgress = (scrollOffset / collapseThreshold).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Collapsed Glass Background surface (fades in via graphicsLayer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = collapseProgress }
                .glassSurface(
                    shape = RoundedCornerShape(0.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Lens Glass Back Button
            PanelCircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 38.dp,
                iconSize = 18.dp,
                onClick = onBack
            )

            // Collapsed title centered in bar
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer { alpha = collapseProgress }
            )

            trailingActions()
        }
    }
}

/**
 * Lens glass search field at the top of Settings.
 */
@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search settings..."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .glassSurface(
                shape = CapsuleShape
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

/**
 * Regular lens glass confirmation dialog (springs in, scrim fades).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .glassSurface(
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = confirmText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDestructive) BadgeRed else ElectricBlue,
                        modifier = Modifier
                            .clickable {
                                onConfirm()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsComponentsPreview() {
    PanelTheme {
        Column(
            modifier = Modifier
                .background(PanelBackground)
                .padding(16.dp)
        ) {
            SectionHeader(text = "Appearance")
            SettingsGroup {
                SettingsRow.Navigation(title = "Theme", value = "Dark", onClick = {})
                SettingsSeparator()
                SettingsRow.Toggle(title = "Reduce glass", checked = false, onCheckedChange = {})
                SettingsSeparator()
                SettingsRow.Action(text = "Reset defaults", onClick = {})
            }
        }
    }
}
