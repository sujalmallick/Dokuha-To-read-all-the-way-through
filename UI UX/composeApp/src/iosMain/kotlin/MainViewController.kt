package io.panel.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import core.crash.IosCrashHandler
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    IosCrashHandler.install()
    val initialCrash = IosCrashHandler.readLastCrash()

    return ComposeUIViewController {
        var crashLog by remember { mutableStateOf(initialCrash) }
        var copied by remember { mutableStateOf(false) }

        val currentCrash = crashLog
        if (currentCrash != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .statusBarsPadding()
                    .navigationBarsPadding()
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
                                text = currentCrash,
                                color = Color(0xFF58A6FF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                IosCrashHandler.copyToClipboard(currentCrash)
                                copied = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(if (copied) "Copied" else "Copy", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { IosCrashHandler.shareCrashTrace(currentCrash) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Share", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            IosCrashHandler.clearLastCrash()
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
            PanelApp()
        }
    }
}
