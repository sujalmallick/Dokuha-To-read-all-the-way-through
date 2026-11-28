package ui.screens.novelreader

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.ReaderPrefsRepository
import data.settings.ReaderSettingsScope
import data.settings.SettingsRepository
import data.settings.observeResolvedNovelSettings
import data.settings.resolveReaderSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.novel_settings_title
import ui.components.AdaptiveReaderOptionSelector
import ui.components.PanelSlider
import ui.components.ReaderIncognitoNotice
import ui.components.ReaderMaskedOverridesNotice
import ui.components.ReaderResetOverridesControl
import ui.components.ReaderScopeToggle
import ui.components.SettingLabelWithCustomBadge
import ui.components.adaptiveReaderSheetSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelSettingsBottomSheet(
    onDismiss: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    scope: ReaderSettingsScope,
    onScopeChanged: (ReaderSettingsScope) -> Unit,
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
        NovelSettingsSheetCard(
            settingsRepository = settingsRepository,
            titleKey = titleKey,
            readerPrefsRepository = readerPrefsRepository,
            scope = scope,
            onScopeChanged = onScopeChanged,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun NovelSettingsSheetCard(
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    scope: ReaderSettingsScope = ReaderSettingsScope.ALL_TITLES,
    onScopeChanged: (ReaderSettingsScope) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val repo = settingsRepository ?: remember { FakeSettingsRepository() }
    val prefsRepo = readerPrefsRepository ?: remember { FakeReaderPrefsRepository(repo) }
    val coroutineScope = rememberCoroutineScope()

    var pendingResetConfirm by rememberSaveable(titleKey) { mutableStateOf(false) }

    val resolvedFlow = remember(titleKey, repo, prefsRepo) {
        val overrideFlow = if (titleKey != null) prefsRepo.observePrefs(titleKey) else flowOf(null)
        observeResolvedNovelSettings(titleKey ?: "", repo, overrideFlow)
    }
    val resolved by resolvedFlow.collectAsState(
        initial = resolveReaderSettings(titleKey ?: "", repo, null)
    )

    val isIncognito by repo.incognitoMode.collectAsState()
    val isTitleScope = titleKey != null && scope == ReaderSettingsScope.THIS_TITLE
    val sheetShape = RoundedCornerShape(28.dp)

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
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CapsuleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.novel_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )

            if (titleKey != null) {
                Spacer(modifier = Modifier.height(10.dp))
                ReaderScopeToggle(
                    scope = scope,
                    overrideCount = resolved.novelOverrideCount,
                    onScopeSelected = { chosen ->
                        pendingResetConfirm = false
                        onScopeChanged(chosen)
                    }
                )
                ReaderMaskedOverridesNotice(
                    visible = !isTitleScope && resolved.novelOverrideCount > 0,
                    overrideCount = resolved.novelOverrideCount,
                    onSwitchToThisTitle = {
                        pendingResetConfirm = false
                        onScopeChanged(ReaderSettingsScope.THIS_TITLE)
                    }
                )
            }

            ReaderIncognitoNotice(visible = isIncognito && isTitleScope)

            Spacer(modifier = Modifier.height(14.dp))

            // Novel Theme
            SettingLabelWithCustomBadge(
                label = "Theme",
                isCustom = titleKey != null && resolved.isNovelThemeCustom,
                onReset = if (titleKey != null) {
                    {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(novelTheme = null) }
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            val themes = NovelReaderTheme.entries
            AdaptiveReaderOptionSelector(
                items = themes,
                selectedIndex = themes.indexOf(resolved.novelTheme).coerceAtLeast(0),
                onItemSelected = { idx ->
                    val chosen = themes[idx]
                    if (isTitleScope) {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(novelTheme = chosen) }
                        }
                    } else {
                        repo.setNovelTheme(chosen)
                    }
                },
                labelProvider = { it.displayName }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Novel Font
            SettingLabelWithCustomBadge(
                label = "Font",
                isCustom = titleKey != null && resolved.isNovelFontCustom,
                onReset = if (titleKey != null) {
                    {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(novelFont = null) }
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            val fonts = NovelFont.entries
            AdaptiveReaderOptionSelector(
                items = fonts,
                selectedIndex = fonts.indexOf(resolved.novelFont).coerceAtLeast(0),
                onItemSelected = { idx ->
                    val chosen = fonts[idx]
                    if (isTitleScope) {
                        coroutineScope.launch {
                            prefsRepo.updatePrefs(titleKey) { it.copy(novelFont = chosen) }
                        }
                    } else {
                        repo.setNovelFont(chosen)
                    }
                },
                labelProvider = { font ->
                    when (font) {
                        NovelFont.SYSTEM -> "System"
                        NovelFont.SERIF -> "Serif"
                        NovelFont.SANS_SERIF -> "Sans"
                        NovelFont.MONOSPACE -> "Mono"
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Font Size Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingLabelWithCustomBadge(
                        label = "Font Size",
                        isCustom = titleKey != null && resolved.isNovelFontSizeCustom,
                        onReset = if (titleKey != null) {
                            {
                                coroutineScope.launch {
                                    prefsRepo.updatePrefs(titleKey) { it.copy(novelFontSize = null) }
                                }
                            }
                        } else null
                    )
                    Text(
                        text = "${resolved.novelFontSize.roundToInt()} pt",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                PanelSlider(
                    value = ((resolved.novelFontSize - 12f) / 14f).coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        val size = (12f + fraction * 14f).roundToInt().toFloat().coerceIn(12f, 26f)
                        if (isTitleScope) {
                            coroutineScope.launch {
                                prefsRepo.updatePrefs(titleKey) { it.copy(novelFontSize = size) }
                            }
                        } else {
                            repo.setNovelFontSize(size)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Line Height Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingLabelWithCustomBadge(
                        label = "Line Height",
                        isCustom = titleKey != null && resolved.isNovelLineHeightCustom,
                        onReset = if (titleKey != null) {
                            {
                                coroutineScope.launch {
                                    prefsRepo.updatePrefs(titleKey) { it.copy(novelLineHeight = null) }
                                }
                            }
                        } else null
                    )
                    val roundedLineHeight = ((resolved.novelLineHeight * 10f).roundToInt() / 10f)
                    Text(
                        text = "${roundedLineHeight}x",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                PanelSlider(
                    value = ((resolved.novelLineHeight - 1.2f) / 1.0f).coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        val lh = (1.2f + fraction * 1.0f).coerceIn(1.2f, 2.2f)
                        if (isTitleScope) {
                            coroutineScope.launch {
                                prefsRepo.updatePrefs(titleKey) { it.copy(novelLineHeight = lh) }
                            }
                        } else {
                            repo.setNovelLineHeight(lh)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reset to defaults (visible ONLY when the title has overrides)
            if (titleKey != null && resolved.hasNovelOverrides) {
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun NovelSettingsSheetContent(
    settingsRepository: SettingsRepository? = null,
    titleKey: String? = null,
    readerPrefsRepository: ReaderPrefsRepository? = null,
    scope: ReaderSettingsScope = ReaderSettingsScope.ALL_TITLES,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        NovelSettingsSheetCard(
            settingsRepository = settingsRepository,
            titleKey = titleKey,
            readerPrefsRepository = readerPrefsRepository,
            scope = scope,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}
