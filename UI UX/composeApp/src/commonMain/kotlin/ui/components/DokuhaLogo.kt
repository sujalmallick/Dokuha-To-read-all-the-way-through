package ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import data.settings.AppLogoStyle
import data.settings.SettingsRepository
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic

/**
 * Returns the compose drawable resource associated with this [AppLogoStyle].
 */
fun AppLogoStyle.drawableResource(): DrawableResource = when (this) {
    AppLogoStyle.GLASSMORPHIC -> Res.drawable.logo_glassmorphic
    AppLogoStyle.CLASSIC -> Res.drawable.logo_classic
}

/**
 * Centralized Dokuha Logo component.
 *
 * If [style] is null, this component observes [SettingsRepository.appLogoStyle]
 * and dynamically reacts to changes across the app with zero restarts.
 */
@Composable
fun DokuhaLogo(
    modifier: Modifier = Modifier,
    style: AppLogoStyle? = null,
    contentDescription: String? = "Dokuha Logo",
    contentScale: ContentScale = ContentScale.Fit
) {
    val effectiveStyle = if (style != null) {
        style
    } else {
        val repo: SettingsRepository = koinInject()
        val currentStyle by repo.appLogoStyle.collectAsState()
        currentStyle
    }

    Image(
        painter = painterResource(effectiveStyle.drawableResource()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
