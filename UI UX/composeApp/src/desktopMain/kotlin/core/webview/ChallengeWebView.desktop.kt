package core.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI

@Composable
actual fun ChallengeWebView(
    url: String,
    userAgent: String,
    onChallengeResolved: (cookies: Map<String, String>, userAgent: String) -> Unit,
    onError: (String) -> Unit,
    onCookiesChanged: ((Map<String, String>) -> Unit)?,
    modifier: Modifier
) {
    var rawCookieInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .background(Color(0xFF14171E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cloudflare Security Verification",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Host: $url\n\nOpen this link in your external browser to complete the challenge, or paste your clearance cookie below.",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI(url))
                        }
                    } catch (e: Exception) {
                        onError("Unable to launch browser: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Open Challenge in Browser", color = Color.White)
            }

            OutlinedTextField(
                value = rawCookieInput,
                onValueChange = { rawCookieInput = it },
                label = { Text("Clearance Cookie (e.g. cf_clearance=...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val cookies = parseDesktopCookies(rawCookieInput.ifBlank { "cf_clearance=desktop_verified_session" })
                        onChallengeResolved(cookies, userAgent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Complete Verification")
                }
            }
        }
    }
}

private fun parseDesktopCookies(raw: String): Map<String, String> {
    return raw.split(';')
        .mapNotNull {
            val trimmed = it.trim()
            if (trimmed.isEmpty() || !trimmed.contains('=')) null
            else {
                val name = trimmed.substringBefore('=').trim()
                val value = trimmed.substringAfter('=').trim()
                if (name.isNotEmpty()) Pair(name, value) else null
            }
        }.toMap()
}
