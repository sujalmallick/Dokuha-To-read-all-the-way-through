package ui.screens.maintab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.library.CategoryRepository
import data.mock.PanelRepository
import data.profile.ProfileRepository
import data.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.glassSource
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
import ui.screens.settings.GlobalReaderSettingsScreen
import ui.screens.settings.SourcesSettingsScreen
import ui.screens.settings.StorageSettingsScreen
import ui.screens.updates.UpdatesScreenContent
import ui.screens.webtoonreader.WebtoonReaderScreen
import ui.theme.GlassBorderStrong
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.ThemeMode

class MainTabScreen(
    private val initialTab: PanelTab = PanelTab.LIBRARY
) : Screen {

    @Composable
    override fun Content() {
        var currentTabName by rememberSaveable { mutableStateOf(initialTab.name) }
        val currentTab = PanelTab.entries.firstOrNull { it.name == currentTabName } ?: PanelTab.LIBRARY
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()

        val repository: PanelRepository = koinInject()
        val categoryRepository: CategoryRepository = koinInject()
        val profileRepository: ProfileRepository = koinInject()
        val settingsRepository: SettingsRepository = koinInject()

        val glassState = rememberPanelGlassState()
        val tabStateHolder = rememberSaveableStateHolder()
        var isModalSheetOpen by remember { mutableStateOf(false) }
        var incognitoSplashState by remember { mutableStateOf<Boolean?>(null) }
        var incognitoSplashTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(currentTab) {
            isModalSheetOpen = false
        }

        LaunchedEffect(incognitoSplashTrigger) {
            if (incognitoSplashTrigger > 0) {
                delay(1500)
                incognitoSplashState = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Smooth seamless tab content switching with preserved per-tab scroll state
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(140),
                label = "MainTabCrossfade",
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(glassState)
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab.name) {
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
                                val sid = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                                if (manga.isNovel) {
                                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name, manga.url, sid))
                                } else {
                                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name, sid))
                                }
                            },
                            onTabSelected = { currentTabName = it.name },
                            showBottomBar = false,
                            onModalSheetVisibilityChanged = { isModalSheetOpen = it }
                        )
                    }
                    PanelTab.UPDATES -> {
                        val updateGroups by repository.getUpdates().collectAsState(initial = emptyList())
                        val historyGroups by repository.getHistory().collectAsState(initial = emptyList())
                        var isRefreshingUpdates by remember { mutableStateOf(false) }
                        val updatesSnackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                        UpdatesScreenContent(
                            updateGroups = updateGroups,
                            historyGroups = historyGroups,
                            isRefreshingUpdates = isRefreshingUpdates,
                            onRefreshUpdates = {
                                if (!isRefreshingUpdates) {
                                    isRefreshingUpdates = true
                                    coroutineScope.launch {
                                        try {
                                            val newChapters = repository.refreshLibraryUpdates()
                                            val msg = if (newChapters > 0) {
                                                "Found $newChapters new chapter${if (newChapters == 1) "" else "s"}"
                                            } else {
                                                "All caught up — no new updates"
                                            }
                                            updatesSnackbarHostState.showSnackbar(msg)
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (_: Throwable) {
                                            updatesSnackbarHostState.showSnackbar("Failed to refresh updates")
                                        } finally {
                                            isRefreshingUpdates = false
                                        }
                                    }
                                }
                            },
                            // TODO: re-enable onDownloadUpdate once the blank-page download-integrity bug is fixed
                            onDownloadUpdate = null,
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
                                val sid = if (manga.sourceId != 0L) manga.sourceId.toString() else null
                                if (manga.isNovel) {
                                    navigator.push(NovelReaderScreen(chapter.url, "Chapter ${chapter.chapterNumber}", chapter.name, manga.url, sid))
                                } else {
                                    navigator.push(WebtoonReaderScreen(manga.url, chapter.url, chapter.name, sid))
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
                            onTabSelected = { currentTabName = it.name },
                            showBottomBar = false,
                            snackbarHostState = updatesSnackbarHostState
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
                            onTabSelected = { currentTabName = it.name },
                            showBottomBar = false,
                            onModalSheetVisibilityChanged = { isModalSheetOpen = it }
                        )
                    }
                    PanelTab.MORE -> {
                        val profile by profileRepository.profile.collectAsState()
                        val themeMode by settingsRepository.themeMode.collectAsState()
                        val cacheSize by settingsRepository.cacheSize.collectAsState()
                        val isIncognito by settingsRepository.incognitoMode.collectAsState()

                        MoreScreenContent(
                            profile = profile,
                            themeModeName = when (themeMode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.DARK -> "Dark Navy"
                                ThemeMode.OLED_BLACK -> "OLED Black"
                            },
                            cacheSize = cacheSize,
                            isIncognito = isIncognito,
                            onToggleIncognito = { enabled ->
                                settingsRepository.setIncognitoMode(enabled)
                                incognitoSplashState = enabled
                                incognitoSplashTrigger++
                            },
                            onOpenProfile = { navigator.push(ProfileScreen()) },
                            onNavigateAppearance = { navigator.push(AppearanceSettingsScreen()) },
                            onNavigateReader = { navigator.push(GlobalReaderSettingsScreen()) },
                            onNavigateLibrary = { navigator.push(LibrarySettingsScreen()) },
                            onNavigateDownloads = { navigator.push(DownloadsSettingsScreen()) },
                            onNavigateSources = { navigator.push(SourcesSettingsScreen()) },
                            onNavigateBackup = { navigator.push(BackupSettingsScreen()) },
                            onNavigateStorage = { navigator.push(StorageSettingsScreen()) },
                            onNavigateAbout = { navigator.push(AboutSettingsScreen()) },
                            onNavigateGlassLab = { navigator.push(GlassLabScreen()) },
                            onTabSelected = { currentTabName = it.name },
                            showBottomBar = false
                        )
                    }
                }
                }
            }

            // Single, persistent bottom nav bar dock across all tabs (auto-hides when modal sheets/filter sheets are open)
            AnimatedVisibility(
                visible = !isModalSheetOpen,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PanelBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { currentTabName = it.name },
                    glassState = glassState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            // Centered Incognito Mode splash overlay
            AnimatedVisibility(
                visible = incognitoSplashState != null,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.86f, animationSpec = tween(220)),
                exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                val enabled = incognitoSplashState == true
                val accent = if (enabled) MaterialTheme.colorScheme.primary else TextSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xEB0E1320))
                        .border(1.dp, GlassBorderStrong, RoundedCornerShape(22.dp))
                        .padding(horizontal = 28.dp, vertical = 22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (enabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (enabled) "Incognito Mode On" else "Incognito Mode Off",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (enabled) "Reading history & progress paused" else "Reading history tracking resumed",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
