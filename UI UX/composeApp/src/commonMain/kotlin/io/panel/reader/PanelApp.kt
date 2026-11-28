package io.panel.reader

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.SlideTransition
import core.di.appModule
import core.extensions.js.AntiBotChallengeManager
import data.settings.SettingsRepository
import org.koin.compose.KoinApplication
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatformTools
import org.koin.dsl.KoinAppDeclaration
import ui.components.AntiBotChallengeSheet
import ui.screens.library.LibraryScreen
import ui.screens.maintab.MainTabScreen
import ui.screens.splash.SplashScreenContent
import ui.screens.welcome.WelcomeScreen
import coil3.compose.setSingletonImageLoaderFactory
import core.image.newPanelImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.theme.PanelAccentColors
import ui.theme.PanelTheme

@Composable
fun PanelApp(
    appDeclaration: KoinAppDeclaration? = null
) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) {
        KoinContext {
            PanelAppRoot()
        }
    } else {
        KoinApplication(application = {
            appDeclaration?.invoke(this)
            modules(appModule)
        }) {
            PanelAppRoot()
        }
    }
}

@Composable
private fun InitSingletonImageLoader(httpClient: io.ktor.client.HttpClient) {
    setSingletonImageLoaderFactory { context ->
        newPanelImageLoader(context, httpClient)
    }
}

@OptIn(cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi::class)
@Composable
private fun PanelAppRoot() {
    val httpClient: io.ktor.client.HttpClient = koinInject()
    InitSingletonImageLoader(httpClient)

    val settingsRepository: SettingsRepository = koinInject()
    val antiBotManager = koinInject<AntiBotChallengeManager>()
    val koin = org.koin.compose.getKoin()
    val activeChallenge by antiBotManager.activeChallenge.collectAsState()

    val themeMode by settingsRepository.themeMode.collectAsState()
    val accentIndex by settingsRepository.accentColorIndex.collectAsState()
    val reduceGlass by settingsRepository.reduceGlass.collectAsState()
    val glassQuality by settingsRepository.glassQuality.collectAsState()
    val reduceMotion by settingsRepository.reduceMotion.collectAsState()
    val isOnboarded by settingsRepository.isOnboardingCompleted.collectAsState()

    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            koin.get<io.panel.reader.db.PanelDatabase>()
        }
    }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            koin.get<core.extensions.ExtensionManager>().ensureInitialized()
        }
    }

    val accentColor = PanelAccentColors.getOrElse(accentIndex) { PanelAccentColors[0] }

    PanelTheme(
        themeMode = themeMode,
        accentColor = accentColor,
        reduceGlass = reduceGlass,
        glassQualitySetting = glassQuality,
        reduceMotion = reduceMotion
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(targetState = showSplash, label = "SplashCrossfade") { isSplash ->
                if (isSplash) {
                    SplashScreenContent(
                        onFinished = {
                            showSplash = false
                        }
                    )
                } else {
                    val initialScreen = remember {
                        if (isOnboarded) MainTabScreen() else WelcomeScreen()
                    }
                    Navigator(initialScreen) { navigator ->
                        SlideTransition(
                            navigator = navigator,
                            disposeScreenAfterTransitionEnd = true
                        )
                    }
                }
            }

            AntiBotChallengeSheet(activeChallenge)
        }
    }
}
