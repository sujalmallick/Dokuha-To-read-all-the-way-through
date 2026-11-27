package ui.screens.webtoonreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.ReaderPrefsRepository
import data.settings.ReaderSettingsScope
import data.settings.SettingsRepository
import data.settings.observeResolvedMangaSettings
import data.settings.resolveReaderSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.reader_cancel
import panelreader.composeapp.generated.resources.reader_confirm_reset
import panelreader.composeapp.generated.resources.reader_custom_override_badge
import panelreader.composeapp.generated.resources.reader_custom_setting_a11y
import panelreader.composeapp.generated.resources.reader_incognito_temp_notice
import panelreader.composeapp.generated.resources.reader_reset_to_defaults
import panelreader.composeapp.generated.resources.reader_scope_all_titles
import panelreader.composeapp.generated.resources.reader_scope_this_title
import panelreader.composeapp.generated.resources.reader_settings_title
import ui.components.GlassSegmentedControl
import ui.components.PanelSlider
import ui.components.PanelSwitch
import ui.components.PanelTab
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

/**
 * In-place Modal Bottom Sheet for Reader Settings.
 * Renders directly over the live, interactive reader canvas with real-time feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(
    onDismiss: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.50f),
        modifier = modifier
    ) {
        ReaderSettingsSheetCard(
            settingsRepository = settingsRepository,
            titleKey = titleKey,
            readerPrefsRepository = readerPrefsRepository,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

/**
 * Liquid glass card containing all reader setting controls with per-title / global scope.
 */
@Composable
fun ReaderSettingsSheetCard(
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val repo = settingsRepository ?: remember { FakeSettingsRepository() }
    val prefsRepo = readerPrefsRepository ?: remember { FakeReaderPrefsRepository(repo) }
    val coroutineScope = rememberCoroutineScope()

    var scope by rememberSaveable(titleKey) {
        mutableStateOf(
            if (titleKey != null) ReaderSettingsScope.THIS_TITLE else ReaderSettingsScope.ALL_TITLES
        )
    }
    var pendingResetConfirm by rememberSaveable(titleKey) { mutableStateOf(false) }

    val resolvedFlow = remember(titleKey, repo, prefsRepo) {
        val overrideFlow = if (titleKey != null) prefsRepo.observePrefs(titleKey) else flowOf(null)
        observeResolvedMangaSettings(titleKey ?: "", repo, overrideFlow)
    }
    val resolved by resolvedFlow.collectAsState(
        initial = resolveReaderSettings(titleKey ?: "", repo, null)
    )

    val isIncognito by repo.incognitoMode.collectAsState()
    val selectedMode = resolved.readingMode
    val readerColorFilter = resolved.colorFilter
    val sidePadding = resolved.sidePadding
    val splitDoubleSpread = resolved.splitDoubleSpread
    val dualPageOffset = resolved.dualPageOffset

    val brightness = resolved.brightness
    val autoDownloadNextChapter = resolved.autoDownloadNextChapter
    val volumeKeyNavigation = resolved.volumeKeyNavigation
    val readerPreloadPages = resolved.readerPreloadPages

    val isTitleScope = titleKey != null && scope == ReaderSettingsScope.THIS_TITLE

    val sheetShape = RoundedCornerShape(28.dp)
    val readingModes = listOf(
        DefaultReadingMode.WEBTOON,
        DefaultReadingMode.RIGHT_TO_LEFT,
        DefaultReadingMode.LEFT_TO_RIGHT,
        DefaultReadingMode.VERTICAL
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .glassDimming(alpha = 0.30f, shape = sheetShape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = sheetShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Drag Handle affordance at top center
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = stringResource(Res.string.reader_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )

            // Scope Switcher ("This title" vs "All titles")
            if (titleKey != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val scopeOptions = listOf(
                    ReaderSettingsScope.THIS_TITLE,
                    ReaderSettingsScope.ALL_TITLES
                )
                val thisTitleLabel = stringResource(Res.string.reader_scope_this_title)
                val allTitlesLabel = stringResource(Res.string.reader_scope_all_titles)
                GlassSegmentedControl(
                    items = scopeOptions,
                    selectedIndex = scopeOptions.indexOf(scope).coerceAtLeast(0),
                    onItemSelected = {
                        scope = scopeOptions[it]
                        pendingResetConfirm = false
                    },
                    labelProvider = { item ->
                        when (item) {
                            ReaderSettingsScope.THIS_TITLE -> thisTitleLabel
                            ReaderSettingsScope.ALL_TITLES -> allTitlesLabel
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                )
            }

            // Incognito notice when editing per-title settings
            AnimatedVisibility(visible = isIncognito && isTitleScope) {
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

            Spacer(modifier = Modifier.height(18.dp))

            // Reading Mode Section
            SettingLabelWithCustomBadge(
                label = "Reading Mode",
                isCustom = titleKey != null && resolved.isReadingModeCustom
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Unified Glass Segmented Control with sliding lens indicator
            GlassSegmentedControl(
                items = readingModes,
                selectedIndex = readingModes.indexOf(selectedMode).coerceAtLeast(0),
                onItemSelected = { idx ->
                    val chosen = readingModes[idx]
                    if (isTitleScope) {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(readingMode = chosen) }
                        }
                    } else {
                        repo.setDefaultReadingMode(chosen)
                    }
                },
                labelProvider = { mode ->
                    when (mode) {
                        DefaultReadingMode.WEBTOON -> "Webtoon"
                        DefaultReadingMode.RIGHT_TO_LEFT -> "RTL"
                        DefaultReadingMode.LEFT_TO_RIGHT -> "LTR"
                        DefaultReadingMode.VERTICAL -> "Vertical"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Color Filter Section (OLED, Sepia, High Contrast)
            SettingLabelWithCustomBadge(
                label = "Color Filter",
                isCustom = titleKey != null && resolved.isColorFilterCustom
            )

            Spacer(modifier = Modifier.height(10.dp))

            val colorFilters = ReaderColorFilter.entries
            GlassSegmentedControl(
                items = colorFilters,
                selectedIndex = colorFilters.indexOf(readerColorFilter).coerceAtLeast(0),
                onItemSelected = { idx ->
                    val chosen = colorFilters[idx]
                    if (isTitleScope) {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(colorFilter = chosen) }
                        }
                    } else {
                        repo.setReaderColorFilter(chosen)
                    }
                },
                labelProvider = { filter ->
                    when (filter) {
                        ReaderColorFilter.DEFAULT -> "Normal"
                        ReaderColorFilter.INVERTED_OLED -> "OLED"
                        ReaderColorFilter.SEPIA -> "Sepia"
                        ReaderColorFilter.HIGH_CONTRAST -> "Contrast"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Background Page Preloading Buffer (Global)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Preload Buffer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = "$readerPreloadPages pages ahead",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val preloadOptions = listOf(2, 4, 6, 10)
            GlassSegmentedControl(
                items = preloadOptions,
                selectedIndex = preloadOptions.indexOf(readerPreloadPages).let { if (it >= 0) it else 2 },
                onItemSelected = { repo.setReaderPreloadPages(preloadOptions[it]) },
                labelProvider = { "$it pgs" },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Brightness Slider (Global)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Brightness",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Brightness",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                PanelSlider(
                    value = brightness,
                    onValueChange = { repo.setBrightness(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Side Padding Slider (Per-title overridable)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingLabelWithCustomBadge(
                        label = "Side Padding",
                        isCustom = titleKey != null && resolved.isSidePaddingCustom
                    )
                    Text(
                        text = "${(sidePadding * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                PanelSlider(
                    value = sidePadding,
                    onValueChange = { newPadding ->
                        if (isTitleScope) {
                            coroutineScope.launch {
                                prefsRepo.updatePrefs(titleKey) { it.copy(sidePadding = newPadding) }
                            }
                        } else {
                            repo.setSidePadding(newPadding)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual-Page Spread Toggle Switch (Per-title overridable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    SettingLabelWithCustomBadge(
                        label = "Dual-page spread",
                        isCustom = titleKey != null && resolved.isSplitDoubleSpreadCustom,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14
                    )
                    Text(
                        text = "Facing pages on tablets & landscape",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                PanelSwitch(
                    checked = splitDoubleSpread,
                    onCheckedChange = { enabled ->
                        if (isTitleScope) {
                            coroutineScope.launch {
                                prefsRepo.updatePrefs(titleKey) { it.copy(splitDoubleSpread = enabled) }
                            }
                        } else {
                            repo.setSplitDoubleSpread(enabled)
                        }
                    }
                )
            }

            if (splitDoubleSpread) {
                Spacer(modifier = Modifier.height(12.dp))

                // Offset Cover Page Toggle Switch (Per-title overridable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        SettingLabelWithCustomBadge(
                            label = "Offset cover page",
                            isCustom = titleKey != null && resolved.isDualPageOffsetCustom,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14
                        )
                        Text(
                            text = "First page as standalone cover",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    PanelSwitch(
                        checked = dualPageOffset,
                        onCheckedChange = { enabled ->
                            if (isTitleScope) {
                                coroutineScope.launch {
                                    prefsRepo.updatePrefs(titleKey) { it.copy(dualPageOffset = enabled) }
                                }
                            } else {
                                repo.setDualPageOffset(enabled)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Download Next Chapter Toggle Switch (Global)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-download next chapter",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Pre-download next chapter at 80% progress",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                PanelSwitch(
                    checked = autoDownloadNextChapter,
                    onCheckedChange = { repo.setAutoDownloadNextChapter(it) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Key Navigation Toggle (Global)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Volume key navigation",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Use volume buttons to scroll pages",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                PanelSwitch(
                    checked = volumeKeyNavigation,
                    onCheckedChange = { repo.setVolumeKeyNavigation(it) }
                )
            }

            // Reset to defaults (visible ONLY when the title has active overrides)
            if (titleKey != null && resolved.hasMangaOverrides) {
                Spacer(modifier = Modifier.height(16.dp))
                if (!pendingResetConfirm) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(CapsuleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CapsuleShape)
                            .clickable(role = Role.Button) { pendingResetConfirm = true }
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(CapsuleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CapsuleShape)
                                .clickable(role = Role.Button) { pendingResetConfirm = false }
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
                                .clickable(role = Role.Button) {
                                    pendingResetConfirm = false
                                    coroutineScope.launch {
                                        prefsRepo.resetPrefs(titleKey)
                                    }
                                }
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingLabelWithCustomBadge(
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

/**
 * Screen wrapper for Voyager navigation (retains compatibility for existing routes/tests).
 */
class ReaderSettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()
        val readerPrefsRepository: ReaderPrefsRepository = koinInject()

        ReaderSettingsScreenContent(
            settingsRepository = settingsRepository,
            readerPrefsRepository = readerPrefsRepository,
            onDismiss = { navigator.pop() },
            onTabSelected = { navigator.pop() }
        )
    }
}

/**
 * Fullscreen card content for tests and standalone previews.
 */
@Composable
fun ReaderSettingsScreenContent(
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    onDismiss: () -> Unit,
    onTabSelected: (PanelTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        ReaderSettingsSheetCard(
            settingsRepository = settingsRepository,
            titleKey = titleKey,
            readerPrefsRepository = readerPrefsRepository,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Preview
@Composable
fun ReaderSettingsScreenPreview() {
    PanelTheme {
        ReaderSettingsScreenContent(
            onDismiss = {},
            onTabSelected = {}
        )
    }
}
