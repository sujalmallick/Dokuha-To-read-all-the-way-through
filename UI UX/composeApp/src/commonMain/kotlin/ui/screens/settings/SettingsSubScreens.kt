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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import ui.theme.BadgeRed
import ui.theme.GlassBorder
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.extensions.ExtensionRepoValidator
import core.platform.PlatformInfo
import data.settings.AppLogoStyle
import data.settings.SplashMotionStyle
import ui.screens.splash.SplashScreen
import ui.screens.splash.SplashScreenContent
import androidx.compose.runtime.key
import ui.theme.CapsuleShape
import data.settings.ExtensionRepo
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
import ui.components.DokuhaLogo
import ui.components.SectionHeader
import ui.components.SettingsGroup
import ui.components.SettingsRow
import ui.components.SettingsSeparator
import data.mock.PanelRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import ui.theme.BottomSheetShape
import ui.theme.GlassBorder
import ui.theme.PanelAccentColors
import ui.theme.PanelBackground
import ui.theme.PanelSurface
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.ReadingCreamBg
import ui.theme.ReadingCreamText
import ui.theme.ReadingDarkBg
import ui.theme.ReadingOledBg
import ui.theme.ReadingOledText
import ui.theme.ReadingSepiaBg
import ui.theme.ReadingSepiaText
import ui.theme.TextMuted
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
        val appLogoStyle by repo.appLogoStyle.collectAsState()

        AppearanceSettingsScreenContent(
            themeMode = themeMode,
            accentIndex = accentIndex,
            reduceGlass = reduceGlass,
            reduceMotion = reduceMotion,
            appLogoStyle = appLogoStyle,
            onBack = { navigator.pop() },
            onNavigateAppLogo = { navigator.push(AppLogoSettingsScreen()) },
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
    appLogoStyle: AppLogoStyle = AppLogoStyle.GLASSMORPHIC,
    onNavigateAppLogo: () -> Unit = {},
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

            // App Logo Section
            item {
                SectionHeader(text = "App Logo & Splash Screen")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Navigation(
                            title = "App Logo & Splash Motion",
                            value = appLogoStyle.displayName,
                            onClick = onNavigateAppLogo
                        )
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
// 1b. APP LOGO SETTINGS SCREEN
// ==========================================
class AppLogoSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()
        val currentStyle by repo.appLogoStyle.collectAsState()
        val currentMotion by repo.splashMotionStyle.collectAsState()

        AppLogoSettingsScreenContent(
            selectedStyle = currentStyle,
            selectedMotion = currentMotion,
            onBack = { navigator.pop() },
            onSelectStyle = { repo.setAppLogoStyle(it) },
            onSelectMotion = { repo.setSplashMotionStyle(it) },
            onPreviewSplash = {
                navigator.push(SplashScreen(motionStyle = currentMotion, logoStyle = currentStyle, isPreview = true))
            }
        )
    }
}

@Composable
fun AppLogoSettingsScreenContent(
    selectedStyle: AppLogoStyle,
    selectedMotion: SplashMotionStyle = SplashMotionStyle.LUNAR_SILVER_GLOW,
    onBack: () -> Unit,
    onSelectStyle: (AppLogoStyle) -> Unit,
    onSelectMotion: (SplashMotionStyle) -> Unit = {},
    onPreviewSplash: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    var replayKey by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 74.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECTION 1: LIVE MOTION PREVIEW HERO CARD
            item {
                SectionHeader(text = "Live Motion Preview")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF080808))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                ) {
                    key(replayKey, selectedStyle, selectedMotion) {
                        SplashScreenContent(
                            onFinished = {},
                            autoFinish = false,
                            logoStyle = selectedStyle,
                            motionStyle = selectedMotion,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating replay pill in top-right
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.5.dp, GlassBorder, CapsuleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedMotion.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.5.dp, GlassBorder, CircleShape)
                                .clickable { replayKey++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Replay Motion",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // SECTION 2: LOGO EDITIONS
            item {
                SectionHeader(text = "Logo Edition")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppLogoStyle.entries.forEach { style ->
                        val isSelected = style == selectedStyle
                        MinimalLogoCard(
                            style = style,
                            isSelected = isSelected,
                            onClick = { onSelectStyle(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // SECTION 3: SPLASH SCREEN MOTION (6 CONCEPTS)
            item {
                SectionHeader(text = "Splash Screen Motion (6 Concepts)")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SplashMotionStyle.entries.forEachIndexed { index, motion ->
                            val isSelected = motion == selectedMotion
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectMotion(motion) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = motion.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary
                                        )
                                        if (motion.isMobileOnly) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f), CapsuleShape)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Android & iOS Only",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = motion.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (index < SplashMotionStyle.entries.size - 1) {
                                SettingsSeparator()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // SECTION 4: PREVIEW FULLSCREEN SPLASH BUTTON
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPreviewSplash)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview Fullscreen Splash",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "The selected logo and motion style trigger automatically upon launch or preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        GlassCollapsingTopBar(
            title = "App Logo & Splash",
            onBack = onBack,
            scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
        )
    }
}

@Composable
private fun MinimalLogoCard(
    style: AppLogoStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primaryColor else GlassBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) PanelSurface else Color(0xFF0F0F12)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prominent Logo preview
            DokuhaLogo(
                style = style,
                modifier = Modifier.size(88.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Edition Title
            Text(
                text = style.displayName.replace(" Edition", ""),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) TextPrimary else TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Active Pill or Subtle Status
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ==========================================
// 2. READER SETTINGS SCREEN (GLOBAL)
// ==========================================
typealias ReaderSettingsScreen = GlobalReaderSettingsScreen

class GlobalReaderSettingsScreen : Screen {
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
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(bg)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) primaryColor else GlassBorder,
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
                                        when (interval) {
                                            UpdateInterval.MANUAL -> core.updates.LibraryUpdateScheduler.cancel()
                                            UpdateInterval.EVERY_12_HOURS -> core.updates.LibraryUpdateScheduler.schedulePeriodicUpdates(12L)
                                            UpdateInterval.DAILY -> core.updates.LibraryUpdateScheduler.schedulePeriodicUpdates(24L)
                                            UpdateInterval.EVERY_2_DAYS -> core.updates.LibraryUpdateScheduler.schedulePeriodicUpdates(48L)
                                            UpdateInterval.WEEKLY -> core.updates.LibraryUpdateScheduler.schedulePeriodicUpdates(168L)
                                        }
                                        isIntervalSheetVisible = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                Text(
                                    text = interval.label,
                                    fontSize = 15.sp,
                                    color = if (interval == updateInterval) primaryColor else TextPrimary
                                )
                                if (interval == updateInterval) {
                                    Text("✓", color = primaryColor, fontWeight = FontWeight.Bold)
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
        var newRepoName by remember { mutableStateOf("") }
        var newRepoUrl by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var repoToRemove by remember { mutableStateOf<ExtensionRepo?>(null) }

        val isAndroid = PlatformInfo.isAndroid

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
                                if (extensionRepos.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = "No extension repositories added. Tap 'Add repository' to add trusted sources.",
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    SettingsSeparator()
                                } else {
                                    extensionRepos.forEach { extRepo ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = extRepo.name,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary
                                                    )
                                                    if (extRepo.isOfficial) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Official",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = extRepo.url,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                                if (!extRepo.fingerprint.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "SHA-256: ${extRepo.fingerprint}",
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = TextSecondary.copy(alpha = 0.8f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Remove",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { repoToRemove = extRepo }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                        SettingsSeparator()
                                    }
                                }
                                SettingsRow.Action(
                                    text = "Add repository",
                                    onClick = {
                                        newRepoName = ""
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
                val validation = remember(newRepoUrl) {
                    if (newRepoUrl.isBlank()) null
                    else ExtensionRepoValidator.validateAndNormalize(newRepoUrl)
                }

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

                        val primaryColor = MaterialTheme.colorScheme.primary
                        OutlinedTextField(
                            value = newRepoName,
                            onValueChange = { newRepoName = it },
                            label = { Text("Repository Name (optional)") },
                            placeholder = { Text("e.g. Community Extensions") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newRepoUrl,
                            onValueChange = {
                                newRepoUrl = it
                                errorMessage = null
                            },
                            label = { Text("Repository URL (https:// only)") },
                            singleLine = true,
                            isError = errorMessage != null || (validation != null && !validation.isValid),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (validation != null && validation.isValid) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF10B981).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "SHA-256 Fingerprint",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = validation.fingerprint.orEmpty(),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else if (validation != null && !validation.isValid) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = validation.errorMessage.orEmpty(),
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val canAdd = validation != null && validation.isValid

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
                            val primaryColor = MaterialTheme.colorScheme.primary
                            Text(
                                text = "Add",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canAdd) primaryColor else TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .clickable(enabled = canAdd) {
                                        val targetName = newRepoName.trim().ifEmpty { "Community Extensions" }
                                        val result = repo.addExtensionRepo(targetName, newRepoUrl)
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

            if (repoToRemove != null) {
                GlassConfirmDialog(
                    title = "Remove Repository?",
                    message = "Are you sure you want to remove '" + (repoToRemove?.name ?: "") + "'? Extensions from this repository will no longer receive updates.",
                    confirmText = "Remove",
                    isDestructive = true,
                    onConfirm = {
                        repoToRemove?.let { repo.removeExtensionRepo(it.id) }
                        repoToRemove = null
                    },
                    onDismiss = { repoToRemove = null }
                )
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
        val panelRepo: PanelRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val lastBackup by repo.lastBackupTimestamp.collectAsState()

        // Status feedback state
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var isExporting by remember { mutableStateOf(false) }
        var isImporting by remember { mutableStateOf(false) }

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
                                text = if (isExporting) "Exporting…" else "Export library to JSON",
                                secondaryText = "Last: $lastBackup",
                                onClick = {
                                    if (!isExporting) {
                                        isExporting = true
                                        statusMessage = null
                                        coroutineScope.launch {
                                            runCatching {
                                                val json = panelRepo.exportLibraryJson()
                                                val fileName = repo.createBackup()
                                                val savedPath = data.backup.BackupFileStore.writeBackup(fileName, json).getOrThrow()
                                                statusMessage = "✓ Exported ${json.length} bytes to $savedPath"
                                            }.onFailure { e ->
                                                statusMessage = "✗ Export failed: ${e.message}"
                                            }
                                            isExporting = false
                                        }
                                    }
                                }
                            )
                            SettingsSeparator()
                            SettingsRow.Action(
                                text = if (isImporting) "Importing…" else "Restore from latest backup",
                                secondaryText = "Restores library titles, categories & history",
                                onClick = {
                                    if (!isImporting) {
                                        isImporting = true
                                        statusMessage = null
                                        coroutineScope.launch {
                                            runCatching {
                                                val latestName = data.backup.BackupFileStore.listBackups().firstOrNull()
                                                    ?: "dokuha_backup.json"
                                                val raw = data.backup.BackupFileStore.readBackup(latestName).getOrThrow()
                                                val restored = panelRepo.importLibraryJson(raw)
                                                statusMessage = if (restored >= 0)
                                                    "✓ Restored $restored title(s) from $latestName"
                                                else
                                                    "✗ Invalid backup file"
                                            }.onFailure { e ->
                                                statusMessage = "✗ Import failed: ${e.message}"
                                            }
                                            isImporting = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Status feedback pill
                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = msg,
                                color = if (msg.startsWith("✓")) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
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
// 7. STORAGE SETTINGS SCREEN (MINIMAL & MODERN)
// ==========================================
class StorageSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo: SettingsRepository = koinInject()
        val downloadManager: data.download.DownloadManager = koinInject()
        val storageStats by downloadManager.storageInfo.collectAsState()
        val primaryColor = MaterialTheme.colorScheme.primary

        val cacheSize by repo.cacheSize.collectAsState()
        val totalDownloadsMb = (storageStats.mangaMb + storageStats.manhwaMb + storageStats.novelsMb + storageStats.othersMb)
        val formattedDownloads = if (totalDownloadsMb >= 1024f) {
            val gbTenths = ((totalDownloadsMb / 1024f) * 10).toInt()
            "${gbTenths / 10}.${gbTenths % 10} GB"
        } else {
            "${totalDownloadsMb.toInt()} MB"
        }
        val freeGb = (storageStats.totalGb - storageStats.usedGb).coerceAtLeast(0f)
        val freeGbText = "${freeGb.toInt()} GB free of ${storageStats.totalGb.toInt()} GB"

        var isClearCacheDialogVisible by remember { mutableStateOf(false) }
        var isClearHistoryDialogVisible by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 74.dp, bottom = 48.dp)
            ) {
                // Storage Overview Hero Card
                item {
                    SectionHeader(text = "App Footprint")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(PanelSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL ESTIMATED FOOTPRINT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formattedDownloads,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "downloaded · $freeGbText",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Multi-segment Storage Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CapsuleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    val isCacheEmpty = cacheSize == "0 MB" || cacheSize.isBlank()
                                    if (!isCacheEmpty) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.70f)
                                                .fillMaxHeight()
                                                .background(primaryColor)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(0.20f)
                                            .fillMaxHeight()
                                            .background(Color(0xFF10B981))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(0.10f)
                                            .fillMaxHeight()
                                            .background(Color(0xFF60A5FA))
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Legend Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StorageLegendItem(color = primaryColor, label = "Cache", value = cacheSize)
                                    StorageLegendItem(color = Color(0xFF10B981), label = "Extensions", value = "~4.8 MB")
                                    StorageLegendItem(color = Color(0xFF60A5FA), label = "Database", value = "~1.2 MB")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Itemized Detailed Storage Rows
                item {
                    SectionHeader(text = "Storage Breakdown")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            // Row 1: Image Cache
                            StorageActionRow(
                                title = "Temporary Image Cache",
                                subtitle = "Cached covers, chapter slices, and web reader pages",
                                sizeLabel = cacheSize,
                                actionLabel = "Clear",
                                onAction = { isClearCacheDialogVisible = true }
                            )
                            SettingsSeparator()

                            // Row 2: Reading History
                            StorageActionRow(
                                title = "Reading History & Progress",
                                subtitle = "Chronological reading events and completion timestamps",
                                sizeLabel = "~1.2 MB",
                                actionLabel = "Clear",
                                onAction = { isClearHistoryDialogVisible = true }
                            )
                            SettingsSeparator()

                            // Row 3: Extensions
                            StorageActionRow(
                                title = "Installed Extensions",
                                subtitle = "Compiled JavaScript sources and local repository caches",
                                sizeLabel = "~4.8 MB",
                                actionLabel = null,
                                onAction = null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Advisory Note
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Offline downloads and library bookmarks are protected in secure persistent storage and will not be lost when clearing cache.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
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

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun StorageActionRow(
    title: String,
    subtitle: String,
    sizeLabel: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = sizeLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (actionLabel != null && onAction != null) {
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeRed
                    )
                }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DokuhaLogo(
                            modifier = Modifier.size(88.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "D O K U H A",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 8.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Next-Generation Reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                item {
                    SectionHeader(text = "Build Information")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Value(
                                title = "Version",
                                value = "1.0.0 (Dokuha)"
                            )
                            SettingsSeparator()
                            SettingsRow.Value(
                                title = "Open-source licenses",
                                value = "Apache 2.0"
                            )
                            SettingsSeparator()
                            SettingsRow.Value(
                                title = "Source code",
                                value = "GitHub (sujalmallick/Dokuha)"
                            )
                            SettingsSeparator()
                            SettingsRow.Navigation(
                                title = "Diagnostics & Logs",
                                value = "View / Export",
                                onClick = { navigator.push(LogViewerScreen()) }
                            )
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "About Dokuha",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )
        }
    }
}
