package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.settings.ReaderSettingsScope
import org.jetbrains.compose.resources.stringResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.reader_cancel
import panelreader.composeapp.generated.resources.reader_confirm_reset
import panelreader.composeapp.generated.resources.reader_custom_override_badge
import panelreader.composeapp.generated.resources.reader_custom_setting_a11y
import panelreader.composeapp.generated.resources.reader_incognito_temp_notice
import panelreader.composeapp.generated.resources.reader_reset_single_setting
import panelreader.composeapp.generated.resources.reader_reset_to_defaults
import panelreader.composeapp.generated.resources.reader_scope_all_titles
import panelreader.composeapp.generated.resources.reader_scope_all_titles_hint
import panelreader.composeapp.generated.resources.reader_scope_masked_notice_many
import panelreader.composeapp.generated.resources.reader_scope_masked_notice_one
import panelreader.composeapp.generated.resources.reader_scope_switch_to_this_title
import panelreader.composeapp.generated.resources.reader_scope_this_title
import panelreader.composeapp.generated.resources.reader_scope_this_title_hint
import ui.glass.PanelGlassState
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSurface
import ui.theme.CapsuleShape
import ui.theme.LocalGlassTokens

/**
 * Applies adaptive sheet container styling: dark liquid-glass scrim in dark/OLED themes
 * and high-contrast frosted surface in light themes.
 */
@Composable
fun Modifier.adaptiveReaderSheetSurface(
    glassState: PanelGlassState,
    shape: Shape
): Modifier {
    val tokens = LocalGlassTokens.current
    val specularBorder = Brush.linearGradient(
        colors = listOf(
            tokens.borderHighlightTopLeft,
            tokens.borderMid,
            tokens.borderBottomRight
        )
    )
    return this
        .clip(shape)
        .background(tokens.sheetTint, shape)
        .border(tokens.borderWidth, specularBorder, shape)
}

/**
 * Shared "This title" vs "All titles" segmented toggle with theme-safe contrast
 * and >= 48dp touch height.
 */
@Composable
fun ReaderScopeToggle(
    scope: ReaderSettingsScope,
    onScopeSelected: (ReaderSettingsScope) -> Unit,
    modifier: Modifier = Modifier,
    overrideCount: Int = 0
) {
    val scopeOptions = listOf(
        ReaderSettingsScope.THIS_TITLE,
        ReaderSettingsScope.ALL_TITLES
    )
    val thisTitleLabel = stringResource(Res.string.reader_scope_this_title)
    val allTitlesLabel = stringResource(Res.string.reader_scope_all_titles)
    val hintText = when (scope) {
        ReaderSettingsScope.THIS_TITLE -> stringResource(Res.string.reader_scope_this_title_hint)
        ReaderSettingsScope.ALL_TITLES -> stringResource(Res.string.reader_scope_all_titles_hint)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scopeOptions.forEach { item ->
                val isSelected = item == scope
                val label = when (item) {
                    ReaderSettingsScope.THIS_TITLE -> if (overrideCount > 0) "$thisTitleLabel ($overrideCount)" else thisTitleLabel
                    ReaderSettingsScope.ALL_TITLES -> allTitlesLabel
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Tab) { onScopeSelected(item) }
                        .semantics { selected = isSelected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 45.dp)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hintText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

/**
 * Adaptive option selector: uses [GlassSegmentedControl] on dark themes at normal scale,
 * and falls back to a wrapping theme-tokenized pill grid when fontScale >= 1.3x
 * or in light themes where dark-glass white overlays lack contrast.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> AdaptiveReaderOptionSelector(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useWrappedPills = isLightSurface || (items.size > 2 && (fontScale >= 1.25f || maxWidth < 280.dp))

        if (!useWrappedPills) {
            GlassSegmentedControl(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
                labelProvider = labelProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )
        } else {
            val useSingleRow = items.size <= 2 && fontScale < 1.8f
            if (useSingleRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CapsuleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CapsuleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(CapsuleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable(role = Role.RadioButton) { onItemSelected(index) }
                                .semantics { selected = isSelected }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelProvider(item),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val pillBackground = when {
                            isLightSurface && isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isLightSurface -> MaterialTheme.colorScheme.surfaceVariant
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        }
                        val pillBorderColor = when {
                            isLightSurface && isSelected -> MaterialTheme.colorScheme.primary
                            isLightSurface -> MaterialTheme.colorScheme.outline
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                        }
                        val pillTextColor = when {
                            isLightSurface && isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            isLightSurface -> MaterialTheme.colorScheme.onSurfaceVariant
                            isSelected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pillBackground)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = pillBorderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(role = Role.RadioButton) { onItemSelected(index) }
                                .semantics { selected = isSelected }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelProvider(item),
                                color = pillTextColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingLabelWithCustomBadge(
    label: String,
    isCustom: Boolean,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: Int = 13,
    onReset: (() -> Unit)? = null
) {
    val customLabel = stringResource(Res.string.reader_custom_override_badge)
    val resetLabel = stringResource(Res.string.reader_reset_single_setting)
    val a11yDescription = if (isCustom) {
        stringResource(Res.string.reader_custom_setting_a11y, label)
    } else {
        label
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = onReset == null) {
            contentDescription = a11yDescription
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium
        )
        if (isCustom) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "· $customLabel",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (onReset != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(role = Role.Button, onClick = onReset)
                        .semantics { contentDescription = "$resetLabel $label" }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resetLabel,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderMaskedOverridesNotice(
    visible: Boolean,
    overrideCount: Int,
    onSwitchToThisTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible && overrideCount > 0, modifier = modifier) {
        val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        val noticeBg = if (isLightSurface) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        }
        val noticeText = if (overrideCount == 1) {
            stringResource(Res.string.reader_scope_masked_notice_one)
        } else {
            stringResource(Res.string.reader_scope_masked_notice_many, overrideCount)
        }
        val actionLabel = stringResource(Res.string.reader_scope_switch_to_this_title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(noticeBg)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onSwitchToThisTitle)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ReaderIncognitoNotice(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        val noticeBg = if (isLightSurface) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        }
        val noticeText = if (isLightSurface) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(noticeBg)
                    .then(
                        if (!isLightSurface) {
                            Modifier.border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = noticeText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.reader_incognito_temp_notice),
                    color = noticeText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ReaderResetOverridesControl(
    pendingConfirm: Boolean,
    onRequestReset: () -> Unit,
    onCancelReset: () -> Unit,
    onConfirmReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!pendingConfirm) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(CapsuleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CapsuleShape)
                .clickable(role = Role.Button, onClick = onRequestReset)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.reader_reset_to_defaults),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CapsuleShape)
                    .clickable(role = Role.Button, onClick = onCancelReset)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.reader_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(role = Role.Button, onClick = onConfirmReset)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.reader_confirm_reset),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
