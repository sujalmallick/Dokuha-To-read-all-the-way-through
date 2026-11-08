package ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.settings.DefaultReadingMode
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.REPOSITORY_SECURITY_WARNING
import data.settings.SettingsRepository
import data.settings.UpdateInterval
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassCollapsingTopBar
import ui.components.GlassConfirmDialog
import ui.components.GlassSegmentedControl
import ui.components.SectionHeader
import ui.components.SettingsGroup
import ui.components.SettingsRow
import ui.components.SettingsSeparator
import ui.theme.BottomSheetShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelAccentColors
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.ReadingCreamBg
import ui.theme.ReadingCreamText
import ui.theme.ReadingDarkBg
import ui.theme.ReadingOledBg
import ui.theme.ReadingOledText
import ui.theme.ReadingSepiaBg
import ui.theme.ReadingSepiaText
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.ThemeMode

// ==========================================
// 1. APPEARANCE SETTINGS SCREEN
// ==========================================
class AppearanceSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val themeMode by repo.themeMode.collectAsState()
        val accentIndex by repo.accentColorIndex.collectAsState()
        val reduceGlass by repo.reduceGlass.collectAsState()
        val reduceMotion by repo.reduceMotion.collectAsState()

        AppearanceSettingsScreenContent(
            themeMode = themeMode,
            accentIndex = accentIndex,
            reduceGlass = reduceGlass,
            reduceMotion = reduceMotion,
            onBack = { navigator.pop() },
            onSelectThemeMode = { repo.setThemeMode(it) },
            onSelectAccentIndex = { repo.setAccentColorIndex(it) },
            onToggleReduceGlass = { repo.setReduceGlass(it) },
            onToggleReduceMotion = { repo.setReduceMotion(it) }
        )
    }
}

@Composable
fun AppearanceSettingsScreenContent(
    themeMode: ThemeMode,
    accentIndex: Int,
    reduceGlass: Boolean,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccentIndex: (Int) -> Unit,
    onToggleReduceGlass: (Boolean) -> Unit,
    onToggleReduceMotion: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
        ) {
            // Theme Selection: Segmented (System / Dark / OLED Black)
            item {
                SectionHeader(text = "Theme Mode")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GlassSegmentedControl(
                        items = listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.OLED_BLACK),
                        selectedIndex = themeMode.ordinal,
                        onItemSelected = { onSelectThemeMode(ThemeMode.entries[it]) },
                        labelProvider = {
                            when (it) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.DARK -> "Dark Navy"
                                ThemeMode.OLED_BLACK -> "OLED Black"
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Accent Color Row (5 plain circles, selected gets a ring, not a pill)
            item {
                SectionHeader(text = "Accent Color")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PanelAccentColors.forEachIndexed { index, color ->
                                val isSelected = accentIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else GlassBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectAccentIndex(index) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Accessibility & Motion Toggles
            item {
                SectionHeader(text = "Accessibility & Motion")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Toggle(
                            title = "Reduce glass",
                            subtitle = "Replaces lens reflections with subtle opaque fills",
                            checked = reduceGlass,
                            onCheckedChange = onToggleReduceGlass
                        )
                        SettingsSeparator()
                        SettingsRow.Toggle(
                            title = "Reduce motion",
                            subtitle = "Disables spring physics and sliding transitions",
                            checked = reduceMotion,
                            onCheckedChange = onToggleReduceMotion
                        )
                    }
                }
            }
        }

        GlassCollapsingTopBar(
            title = "Appearance",
            onBack = onBack,
            scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
        )
    }
}

// ==========================================
// 2. READER SETTINGS SCREEN
// ==========================================
class ReaderSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val defaultReadingMode by repo.defaultReadingMode.collectAsState()
        val sidePadding by repo.sidePadding.collectAsState()
        val brightness by repo.brightness.collectAsState()
        val cropBorders by repo.cropBorders.collectAsState()
        val keepScreenOn by repo.keepScreenOn.collectAsState()

        val novelFont by repo.novelFont.collectAsState()
        val novelFontSize by repo.novelFontSize.collectAsState()
        val novelLineHeight by repo.novelLineHeight.collectAsState()
        val novelTheme by repo.novelTheme.collectAsState()

        GlobalReaderSettingsScreenContent(
            defaultReadingMode = defaultReadingMode,
            sidePadding = sidePadding,
            brightness = brightness,
            cropBorders = cropBorders,
            keepScreenOn = keepScreenOn,
            novelFont = novelFont,
            novelFontSize = novelFontSize,
            novelLineHeight = novelLineHeight,
            novelTheme = novelTheme,
            onBack = { navigator.pop() },
            onSelectReadingMode = { repo.setDefaultReadingMode(it) },
            onChangeSidePadding = { repo.setSidePadding(it) },
            onChangeBrightness = { repo.setBrightness(it) },
            onToggleCropBorders = { repo.setCropBorders(it) },
            onToggleKeepScreenOn = { repo.setKeepScreenOn(it) },
            onSelectNovelTheme = { repo.setNovelTheme(it) },
            onChangeNovelFontSize = { repo.setNovelFontSize(it) },
            onChangeNovelLineHeight = { repo.setNovelLineHeight(it) }
        )
    }
}

@Composable
fun GlobalReaderSettingsScreenContent(
    defaultReadingMode: DefaultReadingMode,
    sidePadding: Float,
    brightness: Float,
    cropBorders: Boolean,
    keepScreenOn: Boolean,
    novelFont: NovelFont,
    novelFontSize: Float,
    novelLineHeight: Float,
    novelTheme: NovelReaderTheme,
    onBack: () -> Unit,
    onSelectReadingMode: (DefaultReadingMode) -> Unit,
    onChangeSidePadding: (Float) -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onToggleCropBorders: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onSelectNovelTheme: (NovelReaderTheme) -> Unit,
    onChangeNovelFontSize: (Float) -> Unit,
    onChangeNovelLineHeight: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
        ) {
            // Reading Mode Segmented
            item {
                SectionHeader(text = "Default Reading Mode")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GlassSegmentedControl(
                        items = DefaultReadingMode.entries,
                        selectedIndex = defaultReadingMode.ordinal,
                        onItemSelected = { onSelectReadingMode(DefaultReadingMode.entries[it]) },
                        labelProvider = { it.label }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Layout & Display Controls
            item {
                SectionHeader(text = "Viewer Adjustments")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Slider(
                            title = "Side Padding",
                            value = sidePadding,
                            valueRange = 0f..0.3f,
                            onValueChange = onChangeSidePadding,
                            valueDisplay = "${(sidePadding * 100).toInt()}%"
                        )
                        SettingsSeparator()
                        SettingsRow.Slider(
                            title = "Screen Brightness",
                            value = brightness,
                            valueRange = 0.1f..1.0f,
                            onValueChange = onChangeBrightness,
                            valueDisplay = "${(brightness * 100).toInt()}%"
                        )
                        SettingsSeparator()
                        // Crop borders: shown disabled with plain text "Coming soon" (no badge)
                        SettingsRow.Toggle(
                            title = "Crop borders",
                            checked = cropBorders,
                            onCheckedChange = onToggleCropBorders,
                            enabled = false,
                            disabledNotice = "Coming soon"
                        )
                        SettingsSeparator()
                        SettingsRow.Toggle(
                            title = "Keep screen on",
                            checked = keepScreenOn,
                            onCheckedChange = onToggleKeepScreenOn
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Tap zones",
                            value = "Standard",
                            onClick = { /* Placeholder page */ }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Novel Reader Typography & Themes
            item {
                SectionHeader(text = "Novel Typography & Colorway")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        // Novel Theme Segmented
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reading Theme", fontSize = 15.sp, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NovelReaderTheme.entries.forEach { theme ->
                                    val isSelected = novelTheme == theme
                                    val bg = when (theme) {
                                        NovelReaderTheme.LIGHT -> Color(0xFFF7F1DF)
                                        NovelReaderTheme.DARK -> Color(0xFF141926)
                                        NovelReaderTheme.SEPIA -> Color(0xFF261D17)
                                        NovelReaderTheme.OLED -> Color(0xFF000000)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(bg)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) ElectricBlue else GlassBorder,
                                                shape = CircleShape
                                            )
                                            .clickable { onSelectNovelTheme(theme) }
                                    )
                                }
                            }
                        }
                        SettingsSeparator()
                        SettingsRow.Slider(
                            title = "Font Size",
                            value = novelFontSize,
                            valueRange = 12f..26f,
                            onValueChange = onChangeNovelFontSize,
                            valueDisplay = "${novelFontSize.toInt()} pt"
                        )
                        SettingsSeparator()
                        SettingsRow.Slider(
                            title = "Line Height",
                            value = novelLineHeight,
                            valueRange = 1.2f..2.2f,
                            onValueChange = onChangeNovelLineHeight,
                            valueDisplay = "${(novelLineHeight * 10).toInt() / 10f}x"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Novel Live Preview Box
            item {
                SectionHeader(text = "Novel Live Preview")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val previewBg = when (novelTheme) {
                        NovelReaderTheme.LIGHT -> ReadingCreamBg
                        NovelReaderTheme.DARK -> ReadingDarkBg
                        NovelReaderTheme.SEPIA -> ReadingSepiaBg
                        NovelReaderTheme.OLED -> ReadingOledBg
                    }
                    val previewText = when (novelTheme) {
                        NovelReaderTheme.LIGHT -> ReadingCreamText
                        NovelReaderTheme.DARK -> Color(0xFFE2E8F0)
                        NovelReaderTheme.SEPIA -> ReadingSepiaText
                        NovelReaderTheme.OLED -> ReadingOledText
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(previewBg)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "The silver airship drifted soundlessly beneath the fractured clouds. From this height, the ruins of the forgotten city looked like broken teeth against the midnight horizon. He adjusted his gloves and stared into the dark.",
                            fontSize = novelFontSize.sp,
                            lineHeight = (novelFontSize * novelLineHeight).sp,
                            color = previewText
                        )
                    }
                }
            }
        }

        GlassCollapsingTopBar(
            title = "Reader Settings",
            onBack = onBack,
            scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
        )
    }
}

// ==========================================
// 3. LIBRARY SETTINGS SCREEN
// ==========================================
class LibrarySettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val defaultCategory by repo.defaultCategory.collectAsState()
        val showUnreadCount by repo.showUnreadCount.collectAsState()
        val updateOnAppLaunch by repo.updateOnAppLaunch.collectAsState()
        val updateInterval by repo.updateInterval.collectAsState()

        var isIntervalSheetVisible by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Shelves & Badges")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Value(
                                title = "Default Category",
                                value = defaultCategory
                            )
                            SettingsSeparator()
                            SettingsRow.Toggle(
                                title = "Show unread count",
                                subtitle = "Displays numeric unread chapter count on covers",
                                checked = showUnreadCount,
                                onCheckedChange = { repo.setShowUnreadCount(it) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    SectionHeader(text = "Automated Updates")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Toggle(
                                title = "Update on app launch",
                                checked = updateOnAppLaunch,
                                onCheckedChange = { repo.setUpdateOnAppLaunch(it) }
                            )
                            SettingsSeparator()
                            SettingsRow.Navigation(
                                title = "Update interval",
                                value = updateInterval.label,
                                onClick = { isIntervalSheetVisible = true }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Library Settings",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )

            if (isIntervalSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { isIntervalSheetVisible = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = PanelSurfaceGlassHigh.copy(alpha = 0.96f),
                    shape = BottomSheetShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Update Interval",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        UpdateInterval.entries.forEach { interval ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repo.setUpdateInterval(interval)
                                        isIntervalSheetVisible = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = interval.label,
                                    fontSize = 15.sp,
                                    color = if (interval == updateInterval) ElectricBlue else TextPrimary
                                )
                                if (interval == updateInterval) {
                                    Text("✓", color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. DOWNLOADS SETTINGS SCREEN
// ==========================================
class DownloadsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val downloadOnlyOnWifi by repo.downloadOnlyOnWifi.collectAsState()
        val deleteAfterReading by repo.deleteAfterReading.collectAsState()
        val concurrentDownloads by repo.concurrentDownloads.collectAsState()
        val downloadLocation by repo.downloadLocation.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Network & Storage Rules")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Toggle(
                                title = "Download only on Wi-Fi",
                                checked = downloadOnlyOnWifi,
                                onCheckedChange = { repo.setDownloadOnlyOnWifi(it) }
                            )
                            SettingsSeparator()
                            SettingsRow.Toggle(
                                title = "Delete after reading",
                                subtitle = "Automatically removes chapter files upon finishing",
                                checked = deleteAfterReading,
                                onCheckedChange = { repo.setDeleteAfterReading(it) }
                            )
                            SettingsSeparator()
                            SettingsRow.Value(
                                title = "Download location",
                                value = downloadLocation
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    SectionHeader(text = "Concurrency Limit")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        GlassSegmentedControl(
                            items = listOf(1, 2, 3),
                            selectedIndex = concurrentDownloads - 1,
                            onItemSelected = { repo.setConcurrentDownloads(it + 1) },
                            labelProvider = { "$it concurrent" }
                        )
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Downloads",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )
        }
    }
}

// ==========================================
// 5. SOURCES & EXTENSIONS SETTINGS SCREEN
// ==========================================
class SourcesSettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val extensionRepos by repo.extensionRepos.collectAsState()
        val languageFilters by repo.languageFilters.collectAsState()
        val showNsfw by repo.showNsfw.collectAsState()

        var isAddRepoVisible by remember { mutableStateOf(false) }
        var newRepoUrl by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Repositories")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            extensionRepos.forEachIndexed { index, extRepo ->
                                SettingsRow.Value(
                                    title = extRepo.name,
                                    subtitle = extRepo.url,
                                    value = if (extRepo.isOfficial) "Official" else "Community"
                                )
                                SettingsSeparator()
                            }
                            SettingsRow.Action(
                                text = "Add repository",
                                onClick = {
                                    newRepoUrl = ""
                                    errorMessage = null
                                    isAddRepoVisible = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    SectionHeader(text = "Filters & Content")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            listOf("en" to "English", "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese").forEachIndexed { index, lang ->
                                val isChecked = languageFilters.contains(lang.first)
                                SettingsRow.Toggle(
                                    title = lang.second,
                                    checked = isChecked,
                                    onCheckedChange = { repo.toggleLanguageFilter(lang.first) }
                                )
                                SettingsSeparator()
                            }
                            SettingsRow.Toggle(
                                title = "Show NSFW sources",
                                subtitle = "Off by default • 18+ content filters",
                                checked = showNsfw,
                                onCheckedChange = { repo.setShowNsfw(it) }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Sources & Extensions",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )

            // Add repository glass sheet with warning text
            if (isAddRepoVisible) {
                ModalBottomSheet(
                    onDismissRequest = { isAddRepoVisible = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = PanelSurfaceGlassHigh.copy(alpha = 0.96f),
                    shape = BottomSheetShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Add Extension Repository",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Security Warning (Mandatory per rule)
                        Text(
                            text = REPOSITORY_SECURITY_WARNING,
                            fontSize = 12.sp,
                            color = Color(0xFFFBBF24),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newRepoUrl,
                            onValueChange = {
                                newRepoUrl = it
                                errorMessage = null
                            },
                            label = { Text("Repository URL (https:// only)") },
                            singleLine = true,
                            isError = errorMessage != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier
                                    .clickable { isAddRepoVisible = false }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricBlue,
                                modifier = Modifier
                                    .clickable {
                                        val result = repo.addExtensionRepo("Custom Extensions", newRepoUrl)
                                        if (result.isSuccess) {
                                            isAddRepoVisible = false
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Invalid URL"
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. BACKUP & RESTORE SETTINGS SCREEN
// ==========================================
class BackupSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val lastBackup by repo.lastBackupTimestamp.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Backup Archive")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Action(
                                text = "Create backup",
                                secondaryText = "Last: $lastBackup",
                                onClick = { repo.createBackup() }
                            )
                            SettingsSeparator()
                            SettingsRow.Action(
                                text = "Restore from backup",
                                secondaryText = ".tachibk / JSON",
                                onClick = { repo.restoreBackup("file://tachiyomi_backup.tachibk") }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Backup & Restore",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )
        }
    }
}

// ==========================================
// 7. STORAGE SETTINGS SCREEN
// ==========================================
class StorageSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()

        val cacheSize by repo.cacheSize.collectAsState()

        var isClearCacheDialogVisible by remember { mutableStateOf(false) }
        var isClearHistoryDialogVisible by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Cache & Footprint")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Value(
                                title = "Cache size",
                                value = cacheSize
                            )
                            SettingsSeparator()
                            SettingsRow.Destructive(
                                text = "Clear cache",
                                onClick = { isClearCacheDialogVisible = true }
                            )
                            SettingsSeparator()
                            SettingsRow.Destructive(
                                text = "Clear reading history",
                                onClick = { isClearHistoryDialogVisible = true }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Storage Management",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )

            if (isClearCacheDialogVisible) {
                GlassConfirmDialog(
                    title = "Clear Cache?",
                    message = "This will delete all temporary chapter slices and downloaded images from local cache. Bookmarks and downloads will remain intact.",
                    confirmText = "Clear",
                    isDestructive = true,
                    onConfirm = { repo.clearCache() },
                    onDismiss = { isClearCacheDialogVisible = false }
                )
            }

            if (isClearHistoryDialogVisible) {
                GlassConfirmDialog(
                    title = "Clear Reading History?",
                    message = "Are you sure you want to clear your entire reading timeline? This action cannot be undone.",
                    confirmText = "Clear History",
                    isDestructive = true,
                    onConfirm = { repo.clearReadingHistory() },
                    onDismiss = { isClearHistoryDialogVisible = false }
                )
            }
        }
    }
}

// ==========================================
// 8. ABOUT SETTINGS SCREEN
// ==========================================
class AboutSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
            ) {
                item {
                    SectionHeader(text = "Build Information")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Value(
                                title = "Version",
                                value = "1.0.0 (Dokuha)"
                            )
                            SettingsSeparator()
                            SettingsRow.Navigation(
                                title = "Open-source licenses",
                                value = "Apache 2.0",
                                onClick = { /* View licenses */ }
                            )
                            SettingsSeparator()
                            SettingsRow.Navigation(
                                title = "Source code",
                                value = "GitHub",
                                onClick = { /* External browser stub */ }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "About Panel",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )
        }
    }
}
