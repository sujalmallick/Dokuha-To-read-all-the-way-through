package ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import data.profile.ProfileRepository
import data.profile.UserProfile
import data.settings.SettingsRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.components.SectionHeader
import ui.components.SettingsGroup
import ui.components.SettingsRow
import ui.components.SettingsSeparator
import ui.screens.browse.BrowseScreen
import ui.screens.glasslab.GlassLabScreen
import ui.screens.library.LibraryScreen
import ui.screens.profile.ProfileScreen
import ui.screens.settings.AboutSettingsScreen
import ui.screens.settings.AppearanceSettingsScreen
import ui.screens.settings.BackupSettingsScreen
import ui.screens.settings.DownloadsSettingsScreen
import ui.screens.settings.LibrarySettingsScreen
import ui.screens.settings.ReaderSettingsScreen
import ui.screens.settings.SourcesSettingsScreen
import ui.screens.settings.StorageSettingsScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class MoreScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val profileRepository: ProfileRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()

        val profile by profileRepository.profile.collectAsState()
        val themeMode by settingsRepository.themeMode.collectAsState()
        val cacheSize by settingsRepository.cacheSize.collectAsState()

        MoreScreenContent(
            profile = profile,
            themeModeName = when (themeMode) {
                ui.theme.ThemeMode.SYSTEM -> "System"
                ui.theme.ThemeMode.DARK -> "Dark Navy"
                ui.theme.ThemeMode.OLED_BLACK -> "OLED Black"
            },
            cacheSize = cacheSize,
            onOpenProfile = { navigator.push(ProfileScreen()) },
            onNavigateAppearance = { navigator.push(AppearanceSettingsScreen()) },
            onNavigateReader = { navigator.push(ReaderSettingsScreen()) },
            onNavigateLibrary = { navigator.push(LibrarySettingsScreen()) },
            onNavigateDownloads = { navigator.push(DownloadsSettingsScreen()) },
            onNavigateSources = { navigator.push(SourcesSettingsScreen()) },
            onNavigateBackup = { navigator.push(BackupSettingsScreen()) },
            onNavigateStorage = { navigator.push(StorageSettingsScreen()) },
            onNavigateAbout = { navigator.push(AboutSettingsScreen()) },
            onNavigateGlassLab = { navigator.push(GlassLabScreen()) },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> {} // Current
                }
            }
        )
    }
}

@Composable
fun MoreScreenContent(
    profile: UserProfile,
    themeModeName: String,
    cacheSize: String,
    onOpenProfile: () -> Unit,
    onNavigateAppearance: () -> Unit,
    onNavigateReader: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSources: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateStorage: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateGlassLab: () -> Unit = {},
    onTabSelected: (PanelTab) -> Unit,
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Screen Header Title
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "More",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Profile Header Row (avatar + name + chevron, whole row tappable)
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    SettingsGroup {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White.copy(alpha = 0.1f)),
                                    onClick = onOpenProfile
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(profile.avatarColorHex))
                                        .border(1.5.dp, GlassBorderStrong, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.initials,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = profile.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = profile.memberSince,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "View Profile",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Experience Settings Group
            item {
                SectionHeader(text = "Preferences")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Navigation(
                            title = "Appearance",
                            subtitle = "Theme, accent colors, glass & motion",
                            value = themeModeName,
                            icon = Icons.Default.Palette,
                            onClick = onNavigateAppearance
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Reader",
                            subtitle = "Modes, padding, typography & novel",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            onClick = onNavigateReader
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Library",
                            subtitle = "Categories, unread counts & intervals",
                            icon = Icons.Default.ViewCarousel,
                            onClick = onNavigateLibrary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Content & Storage Group
            item {
                SectionHeader(text = "Data & Connectivity")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Navigation(
                            title = "Downloads",
                            subtitle = "Queue concurrency & storage rules",
                            icon = Icons.Default.CloudDownload,
                            onClick = onNavigateDownloads
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Sources & Extensions",
                            subtitle = "Repositories, languages & filters",
                            icon = Icons.Default.Extension,
                            onClick = onNavigateSources
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Backup & Restore",
                            subtitle = "Import and export reading archives",
                            icon = Icons.Default.Save,
                            onClick = onNavigateBackup
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Storage",
                            subtitle = "Cache cleanup and database management",
                            value = cacheSize,
                            icon = Icons.Default.Storage,
                            onClick = onNavigateStorage
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Application
            item {
                SectionHeader(text = "About")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Navigation(
                            title = "About Panel",
                            value = "v1.0.0 (Dokuha)",
                            icon = Icons.Default.Info,
                            onClick = onNavigateAbout
                        )
                        SettingsSeparator()
                        SettingsRow.Navigation(
                            title = "Liquid Glass Lab",
                            subtitle = "iOS 26 optical lens parameter tuner",
                            value = "Debug",
                            icon = Icons.Default.Science,
                            onClick = onNavigateGlassLab
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview
@Composable
fun MoreScreenPreview() {
    PanelTheme {
        MoreScreenContent(
            profile = UserProfile(),
            themeModeName = "Dark Navy",
            cacheSize = "324 MB",
            onOpenProfile = {},
            onNavigateAppearance = {},
            onNavigateReader = {},
            onNavigateLibrary = {},
            onNavigateDownloads = {},
            onNavigateSources = {},
            onNavigateBackup = {},
            onNavigateStorage = {},
            onNavigateAbout = {},
            onTabSelected = {}
        )
    }
}
