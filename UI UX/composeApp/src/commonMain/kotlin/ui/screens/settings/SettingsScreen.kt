package ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.settings.SettingsRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassCollapsingTopBar
import ui.components.GlassSearchField
import ui.components.SectionHeader
import ui.components.SettingsGroup
import ui.components.SettingsRow
import ui.components.SettingsSeparator
import ui.theme.PanelBackground
import ui.theme.PanelTheme

class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: SettingsRepository = koinInject()

        val themeMode by repository.themeMode.collectAsState()
        val defaultReadingMode by repository.defaultReadingMode.collectAsState()
        val cacheSize by repository.cacheSize.collectAsState()

        SettingsScreenContent(
            themeModeName = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
            readingModeName = defaultReadingMode.label,
            cacheSize = cacheSize,
            onBack = { navigator.pop() },
            onNavigateAppearance = { navigator.push(AppearanceSettingsScreen()) },
            onNavigateReader = { navigator.push(GlobalReaderSettingsScreen()) },
            onNavigateLibrary = { navigator.push(LibrarySettingsScreen()) },
            onNavigateDownloads = { navigator.push(DownloadsSettingsScreen()) },
            onNavigateSources = { navigator.push(SourcesSettingsScreen()) },
            onNavigateBackup = { navigator.push(BackupSettingsScreen()) },
            onNavigateStorage = { navigator.push(StorageSettingsScreen()) },
            onNavigateAbout = { navigator.push(AboutSettingsScreen()) }
        )
    }
}

@Composable
fun SettingsScreenContent(
    themeModeName: String,
    readingModeName: String,
    cacheSize: String,
    onBack: () -> Unit,
    onNavigateAppearance: () -> Unit,
    onNavigateReader: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSources: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateStorage: () -> Unit,
    onNavigateAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

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
            // Glass search field at top of Settings
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    GlassSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val q = searchQuery.trim()
            val showAppearance = q.isEmpty() || "Appearance Theme accent colors glass motion".contains(q, ignoreCase = true)
            val showReader = q.isEmpty() || "Reader Reading modes typography paging novel".contains(q, ignoreCase = true)
            val showLibrary = q.isEmpty() || "Library Categories unread counts auto-update".contains(q, ignoreCase = true)
            val showDownloads = q.isEmpty() || "Downloads Wi-Fi queue limits disk".contains(q, ignoreCase = true)
            val showSources = q.isEmpty() || "Sources Extensions Repositories language NSFW".contains(q, ignoreCase = true)
            val showBackup = q.isEmpty() || "Backup Restore Export Tachiyomi Mihon".contains(q, ignoreCase = true)
            val showStorage = q.isEmpty() || "Storage Cache cleanup database".contains(q, ignoreCase = true)
            val showAbout = q.isEmpty() || "About v1.0.0 Dokuha licenses".contains(q, ignoreCase = true)
            val anyMatch = showAppearance || showReader || showLibrary || showDownloads || showSources || showBackup || showStorage || showAbout

            if (!anyMatch) {
                item {
                    androidx.compose.material3.Text(
                        text = "No matching settings for \"$q\"",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = ui.theme.TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                    )
                }
            }

            // General & Experience Group
            if (showAppearance || showReader || showLibrary) {
                item {
                    SectionHeader(text = "Experience")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            var needsSep = false
                            if (showAppearance) {
                                SettingsRow.Navigation(
                                    title = "Appearance",
                                    subtitle = "Theme, accent colors, glass & motion",
                                    value = themeModeName,
                                    icon = Icons.Default.Palette,
                                    onClick = onNavigateAppearance
                                )
                                needsSep = true
                            }
                            if (showReader) {
                                if (needsSep) SettingsSeparator()
                                SettingsRow.Navigation(
                                    title = "Reader",
                                    subtitle = "Reading modes, typography, paging & novel",
                                    value = readingModeName,
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    onClick = onNavigateReader
                                )
                                needsSep = true
                            }
                            if (showLibrary) {
                                if (needsSep) SettingsSeparator()
                                SettingsRow.Navigation(
                                    title = "Library",
                                    subtitle = "Categories, unread counts & auto-update",
                                    icon = Icons.Default.ViewCarousel,
                                    onClick = onNavigateLibrary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Content & Storage Group
            if (showDownloads || showSources || showBackup || showStorage) {
                item {
                    SectionHeader(text = "Content & Storage")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            var needsSep = false
                            if (showDownloads) {
                                SettingsRow.Navigation(
                                    title = "Downloads",
                                    subtitle = "Wi-Fi rules, queue limits & disk destination",
                                    icon = Icons.Default.CloudDownload,
                                    onClick = onNavigateDownloads
                                )
                                needsSep = true
                            }
                            if (showSources) {
                                if (needsSep) SettingsSeparator()
                                SettingsRow.Navigation(
                                    title = "Sources & Extensions",
                                    subtitle = "Repositories, language filters & NSFW",
                                    icon = Icons.Default.Extension,
                                    onClick = onNavigateSources
                                )
                                needsSep = true
                            }
                            if (showBackup) {
                                if (needsSep) SettingsSeparator()
                                SettingsRow.Navigation(
                                    title = "Backup & Restore",
                                    subtitle = "Export or restore Tachiyomi/Mihon libraries",
                                    icon = Icons.Default.Save,
                                    onClick = onNavigateBackup
                                )
                                needsSep = true
                            }
                            if (showStorage) {
                                if (needsSep) SettingsSeparator()
                                SettingsRow.Navigation(
                                    title = "Storage",
                                    subtitle = "Cache cleanup and database management",
                                    value = cacheSize,
                                    icon = Icons.Default.Storage,
                                    onClick = onNavigateStorage
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Information
            if (showAbout) {
                item {
                    SectionHeader(text = "Application")
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsGroup {
                            SettingsRow.Navigation(
                                title = "About",
                                subtitle = "v1.0.0 (Dokuha) • Open source licenses",
                                icon = Icons.Default.Info,
                                onClick = onNavigateAbout
                            )
                        }
                    }
                }
            }
        }

        // Collapsing Top Bar
        val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat() +
            (scrollState.firstVisibleItemIndex * 80f)

        GlassCollapsingTopBar(
            title = "Settings",
            onBack = onBack,
            scrollOffset = scrollOffset
        )
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    PanelTheme {
        SettingsScreenContent(
            themeModeName = "Dark",
            readingModeName = "Webtoon",
            cacheSize = "324 MB",
            onBack = {},
            onNavigateAppearance = {},
            onNavigateReader = {},
            onNavigateLibrary = {},
            onNavigateDownloads = {},
            onNavigateSources = {},
            onNavigateBackup = {},
            onNavigateStorage = {},
            onNavigateAbout = {}
        )
    }
}
