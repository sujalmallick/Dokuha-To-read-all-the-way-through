package io.panel.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.SlideTransition
import core.di.appModule
import data.settings.SettingsRepository
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.KoinAppDeclaration
import ui.screens.welcome.WelcomeScreen
import ui.theme.PanelAccentColors
import ui.theme.PanelTheme

@Composable
fun PanelApp(
    appDeclaration: KoinAppDeclaration? = null
) {
    KoinApplication(application = {
        appDeclaration?.invoke(this)
        modules(appModule)
    }) {
        PanelAppRoot()
    }
}

@Composable
private fun PanelAppRoot() {
    val settingsRepository: SettingsRepository = koinInject()
    val themeMode by settingsRepository.themeMode.collectAsState()
    val accentIndex by settingsRepository.accentColorIndex.collectAsState()
    val reduceGlass by settingsRepository.reduceGlass.collectAsState()
    val reduceMotion by settingsRepository.reduceMotion.collectAsState()

    val accentColor = PanelAccentColors.getOrElse(accentIndex) { PanelAccentColors[0] }

    PanelTheme(
        themeMode = themeMode,
        accentColor = accentColor,
        reduceGlass = reduceGlass,
        reduceMotion = reduceMotion
    ) {
        Navigator(WelcomeScreen()) { navigator ->
            if (reduceMotion) {
                FadeTransition(navigator)
            } else {
                SlideTransition(navigator)
            }
        }
    }
}
