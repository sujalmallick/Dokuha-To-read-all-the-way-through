package io.panel.reader

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.settings.SettingsRepository
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import ui.volume.LocalVolumeKeyState
import ui.volume.VolumeKeyState

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    // Shared mutable state so the reader composable can subscribe to volume key events
    internal val volumeUpEvents = mutableStateOf(0)
    internal val volumeDownEvents = mutableStateOf(0)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            settingsRepository.volumeKeyNavigation.value
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    volumeUpEvents.value++
                    return true   // consume — suppress system volume UI
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeDownEvents.value++
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure compose resources context is set
        DokuhaApp.initComposeResourcesContext(this.applicationContext)

        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {
            // Graceful fallback for custom OEM ROMs
        }

        val prefs = getSharedPreferences("dokuha_crash", Context.MODE_PRIVATE)
        val initialCrash = prefs.getString("last_crash", null)

        setContent {
            var crashLog by remember { mutableStateOf(initialCrash) }

            if (crashLog != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1117))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Dokuha Crash Diagnostic",
                                color = Color(0xFFFF5252),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The previous launch stopped due to the following exception:",
                                color = Color(0xFFC9D1D9),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF161B22), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = crashLog ?: "",
                                    color = Color(0xFF58A6FF),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                prefs.edit().remove("last_crash").apply()
                                crashLog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Clear & Launch Dokuha", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                val volumeKeyState = VolumeKeyState(
                    upPresses = volumeUpEvents,
                    downPresses = volumeDownEvents
                )
                CompositionLocalProvider(LocalVolumeKeyState provides volumeKeyState) {
                    PanelApp(
                        appDeclaration = {
                            if (GlobalContext.getOrNull() == null) {
                                androidContext(this@MainActivity.applicationContext)
                            }
                        }
                    )
                }
            }
        }
    }
}
