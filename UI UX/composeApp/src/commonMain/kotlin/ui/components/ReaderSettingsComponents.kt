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
import panelreader.composeapp.generated.resources.reader_reset_to_defaults
import panelreader.composeapp.generated.resources.reader_scope_all_titles
import panelreader.composeapp.generated.resources.reader_scope_this_title
import ui.glass.PanelGlassState
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSurface
import ui.theme.CapsuleShape

/**
 * Applies adaptive sheet container styling: dark liquid-glass scrim in dark/OLED themes
 * and high-contrast frosted surface in light themes.
 */
@Composable
fun Modifier.adaptiveReaderSheetSurface(
    glassState: PanelGlassState,
    shape: Shape
): Modifier {
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return if (isLightSurface) {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        this
            .glassDimming(alpha = 0.30f, shape = shape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = shape
            )
    }
}

/**
 * Shared "This title" vs "All titles" segmented toggle with theme-safe contrast
 * and >= 48dp touch height.
 */
@Composable
fun ReaderScopeToggle(
    scope: ReaderSettingsScope,
    onScopeSelected: (ReaderSettingsScope) -> Unit,
    modifier: Modifier = Modifier
) {
    val scopeOptions = listOf(
        ReaderSettingsScope.THIS_TITLE,
        ReaderSettingsScope.ALL_TITLES
    )
    val thisTitleLabel = stringResource(Res.string.reader_scope_this_title)
    val allTitlesLabel = stringResource(Res.string.reader_scope_all_titles)

    AdaptiveReaderOptionSelector(
        items = scopeOptions,
        selectedIndex = scopeOptions.indexOf(scope).coerceAtLeast(0),
        onItemSelected = { onScopeSelected(scopeOptions[it]) },
        labelProvider = { item ->
            when (item) {
                ReaderSettingsScope.THIS_TITLE -> thisTitleLabel
                ReaderSettingsScope.ALL_TITLES -> allTitlesLabel
            }
        },
        modifier = modifier
    )
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(role = Role.RadioButton) { onItemSelected(index) }
                                .semantics { selected = isSelected }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelProvider(item),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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
    fontSize: Int = 13
) {
    val customLabel = stringResource(Res.string.reader_custom_override_badge)
    val a11yDescription = if (isCustom) {
        stringResource(Res.string.reader_custom_setting_a11y, label)
    } else {
        label
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
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
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = customLabel,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
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
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.reader_incognito_temp_notice),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
