package ui.screens.webtoonreader

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import panelreader.composeapp.generated.resources.reader_global_settings_section
import panelreader.composeapp.generated.resources.reader_settings_title
import ui.components.AdaptiveReaderOptionSelector
import ui.components.PanelSlider
import ui.components.PanelSwitch
import ui.components.PanelTab
import ui.components.ReaderIncognitoNotice
import ui.components.ReaderMaskedOverridesNotice
import ui.components.ReaderResetOverridesControl
import ui.components.ReaderScopeToggle
import ui.components.SettingLabelWithCustomBadge
import ui.components.adaptiveReaderSheetSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.PanelTheme

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
 * Liquid glass card containing manga/webtoon reader setting controls with per-title / global scope
 * and a clearly separated global-only section.
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

    // Open on "All titles" by default so users don't create hidden overrides accidentally,
    // unless the title already has per-title overrides.
    var scope by rememberSaveable(titleKey) {
        mutableStateOf(ReaderSettingsScope.ALL_TITLES)
    }
    var hasInitializedScope by rememberSaveable(titleKey) { mutableStateOf(false) }
    var pendingResetConfirm by rememberSaveable(titleKey) { mutableStateOf(false) }

    LaunchedEffect(titleKey, prefsRepo) {
        if (!hasInitializedScope && titleKey != null) {
            val initialPrefs = prefsRepo.getPrefs(titleKey)
            if (initialPrefs?.hasMangaOverrides == true) {
                scope = ReaderSettingsScope.THIS_TITLE
            }
            hasInitializedScope = true
        }
    }

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
            .adaptiveReaderSheetSurface(glassState = glassState, shape = sheetShape)
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
                ReaderScopeToggle(
                    scope = scope,
                    overrideCount = resolved.mangaOverrideCount,
                    onScopeSelected = { chosen ->
                        scope = chosen
                        hasInitializedScope = true
                        pendingResetConfirm = false
                    }
                )
                ReaderMaskedOverridesNotice(
                    visible = !isTitleScope && resolved.mangaOverrideCount > 0,
                    overrideCount = resolved.mangaOverrideCount,
                    onSwitchToThisTitle = {
                        scope = ReaderSettingsScope.THIS_TITLE
                        hasInitializedScope = true
                        pendingResetConfirm = false
                    }
                )
            }

            // Incognito notice when editing per-title settings
            ReaderIncognitoNotice(visible = isIncognito && isTitleScope)

            Spacer(modifier = Modifier.height(18.dp))

            // Reading Mode Section
            SettingLabelWithCustomBadge(
                label = "Reading Mode",
                isCustom = titleKey != null && resolved.isReadingModeCustom,
                onReset = if (titleKey != null) {
                    {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(readingMode = null) }
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdaptiveReaderOptionSelector(
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
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Color Filter Section (OLED, Sepia, High Contrast)
            SettingLabelWithCustomBadge(
                label = "Color Filter",
                isCustom = titleKey != null && resolved.isColorFilterCustom,
                onReset = if (titleKey != null) {
                    {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(colorFilter = null) }
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(10.dp))

            val colorFilters = ReaderColorFilter.entries
            AdaptiveReaderOptionSelector(
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
                }
            )

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
                        isCustom = titleKey != null && resolved.isSidePaddingCustom,
                        onReset = if (titleKey != null) {
                            {
                                coroutineScope.launch {
                                    prefsRepo.updatePrefs(titleKey) { it.copy(sidePadding = null) }
                                }
                            }
                        } else null
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
                        fontSize = 14,
                        onReset = if (titleKey != null) {
                            {
                                coroutineScope.launch {
                                    prefsRepo.updatePrefs(titleKey) { it.copy(splitDoubleSpread = null) }
                                }
                            }
                        } else null
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
                            fontSize = 14,
                            onReset = if (titleKey != null) {
                                {
                                    coroutineScope.launch {
                                        prefsRepo.updatePrefs(titleKey) { it.copy(dualPageOffset = null) }
                                    }
                                }
                            } else null
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

            // Reset to defaults (visible ONLY when the title has active overrides)
            if (titleKey != null && resolved.hasMangaOverrides) {
                Spacer(modifier = Modifier.height(16.dp))
                ReaderResetOverridesControl(
                    pendingConfirm = pendingResetConfirm,
                    onRequestReset = { pendingResetConfirm = true },
                    onCancelReset = { pendingResetConfirm = false },
                    onConfirmReset = {
                        pendingResetConfirm = false
                        coroutineScope.launch {
                            prefsRepo.resetPrefs(titleKey)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Clearly labeled Global-only section (never follows "This title" toggle)
            Text(
                text = stringResource(Res.string.reader_global_settings_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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
            AdaptiveReaderOptionSelector(
                items = preloadOptions,
                selectedIndex = preloadOptions.indexOf(readerPreloadPages).let { if (it >= 0) it else 2 },
                onItemSelected = { repo.setReaderPreloadPages(preloadOptions[it]) },
                labelProvider = { "$it pgs" }
            )

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

            Spacer(modifier = Modifier.height(8.dp))
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
