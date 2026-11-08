package io.panel.reader

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import core.di.appModule
import org.koin.compose.KoinApplication
import ui.screens.welcome.WelcomeScreen
import ui.theme.PanelTheme

@Composable
fun PanelApp() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        PanelTheme {
            Navigator(WelcomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
