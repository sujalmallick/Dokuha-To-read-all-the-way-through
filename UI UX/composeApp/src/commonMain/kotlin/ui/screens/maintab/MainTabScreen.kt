package ui.screens.maintab

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.library.CategoryRepository
import data.mock.PanelRepository
import data.profile.ProfileRepository
import data.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreenContent
import ui.screens.browse.SourceDetailScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.extensions.ExtensionsScreen
import ui.screens.glasslab.GlassLabScreen
import ui.screens.library.LibraryScreenContent
import ui.screens.more.MoreScreenContent
import ui.screens.novelreader.NovelReaderScreen
import ui.screens.profile.ProfileScreen
import ui.screens.settings.AboutSettingsScreen
import ui.screens.settings.AppearanceSettingsScreen
import ui.screens.settings.BackupSettingsScreen
import ui.screens.settings.DownloadsSettingsScreen
import ui.screens.settings.LibrarySettingsScreen
import ui.screens.settings.ReaderSettingsScreen
import ui.screens.settings.SourcesSettingsScreen
import ui.screens.settings.StorageSettingsScreen
import ui.screens.updates.UpdatesScreenContent
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.ThemeMode

class MainTabScreen(
    private val initialTab: PanelTab = PanelTab.LIBRARY
) : Screen {

    @Composable
    override fun Content() {
        var currentTab by rememberSaveable { mutableStateOf(initialTab) }
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()

        val repository: PanelRepository = koinInject()
        val categoryRepository: CategoryRepository = koinInject()
        val profileRepository: ProfileRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()

        val glassState = rememberPanelGlassState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Smooth seamless tab content switching with zero white flash
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(140),
                label = "MainTabCrossfade",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> {
                        LibraryScreenContent(
                            repository = repository,
                            categoryRepository = categoryRepository,
                            settingsRepository = settingsRepository,
                            onOpenManga = { manga ->
                                navigator.push(
                                    TitleDetailsScreen(
                                        mangaUrl = manga.url,
                                        initialTitle = manga.title,
                                        initialThumbnailUrl = manga.thumbnailUrl,
                                        sourceId = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                                    )
                                )
                            },
                            onResumeChapter = { manga, chapter ->
                                if (manga.isNovel) {
                                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name))
                                } else {
                                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name))
                                }
                            },
                            onTabSelected = { currentTab = it },
                            showBottomBar = false
                        )
                    }
                    PanelTab.UPDATES -> {
                        val updateGroups by repository.getUpdates().collectAsState(initial = emptyList())
                        val historyGroups by repository.getHistory().collectAsState(initial = emptyList())
                        UpdatesScreenContent(
                            updateGroups = updateGroups,
                            historyGroups = historyGroups,
                            onOpenManga = { manga ->
                                navigator.push(
                                    TitleDetailsScreen(
                                        mangaUrl = manga.url,
                                        initialTitle = manga.title,
                                        initialThumbnailUrl = manga.thumbnailUrl,
                                        sourceId = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                                    )
                                )
                            },
                            onResumeChapter = { manga, chapter ->
                                if (manga.isNovel) {
                                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name))
                                } else {
                                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name))
                                }
                            },
                            onDeleteHistoryItem = { historyId ->
                                coroutineScope.launch {
                                    repository.deleteHistoryItem(historyId)
                                }
                            },
                            onClearAllHistory = {
                                coroutineScope.launch {
                                    repository.clearAllHistory()
                                }
                            },
                            onTabSelected = { currentTab = it },
                            showBottomBar = false
                        )
                    }
                    PanelTab.BROWSE -> {
                        val sources by repository.getSources().collectAsState(initial = emptyList())
                        val trending by repository.getTrending().collectAsState(initial = emptyList())
                        BrowseScreenContent(
                            sources = sources,
                            trending = trending,
                            repository = repository,
                            onOpenSource = { source -> navigator.push(SourceDetailScreen(source.id, source.name)) },
                            onSeeAllSources = { navigator.push(ExtensionsScreen()) },
                            onOpenManga = { manga ->
                                navigator.push(
                                    TitleDetailsScreen(
                                        mangaUrl = manga.url,
                                        initialTitle = manga.title,
                                        initialThumbnailUrl = manga.thumbnailUrl,
                                        sourceId = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                                    )
                                )
                            },
                            onTabSelected = { currentTab = it },
                            showBottomBar = false
                        )
                    }
                    PanelTab.MORE -> {
                        val profile by profileRepository.profile.collectAsState()
                        val themeMode by settingsRepository.themeMode.collectAsState()
                        val cacheSize by settingsRepository.cacheSize.collectAsState()

                        MoreScreenContent(
                            profile = profile,
                            themeModeName = when (themeMode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.DARK -> "Dark Navy"
                                ThemeMode.OLED_BLACK -> "OLED Black"
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
                            onTabSelected = { currentTab = it },
                            showBottomBar = false
                        )
                    }
                }
            }

            // Single, persistent bottom nav bar dock across all tabs
            PanelBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                glassState = glassState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
