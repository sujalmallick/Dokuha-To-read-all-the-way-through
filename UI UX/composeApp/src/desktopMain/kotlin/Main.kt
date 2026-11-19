import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.panel.reader.PanelApp

fun main() = application {
    val windowState = rememberWindowState(width = 440.dp, height = 900.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dokuha (読破)",
        state = windowState
    ) {
        PanelApp()
    }
}
