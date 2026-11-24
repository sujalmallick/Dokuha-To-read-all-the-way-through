package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import core.extensions.js.AntiBotChallengeRequest
import core.extensions.js.AntiBotResolutionResult
import core.webview.ChallengeWebView
import ui.theme.TextPrimary
import ui.theme.TextSecondary

/**
 * Modal sheet presenting an interactive anti-bot / Cloudflare Turnstile challenge.
 * Embeds the platform [ChallengeWebView] to let users verify without leaving Dokuha,
 * automatically capturing clearance cookies and resuming suspended scraper coroutines.
 */
@Composable
fun AntiBotChallengeSheet(
    challenge: AntiBotChallengeRequest?,
    modifier: Modifier = Modifier
) {
    if (challenge == null) return

    Dialog(
        onDismissRequest = { challenge.onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF161A22))
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Security Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = challenge.domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = { challenge.onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The source host requires a human verification check to proceed. Please complete the challenge below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                var currentCookies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
                var currentUa by remember { mutableStateOf(challenge.userAgent) }

                // Embedded Multiplatform Challenge WebView
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1218))
                ) {
                    ChallengeWebView(
                        url = challenge.url,
                        userAgent = challenge.userAgent,
                        onChallengeResolved = { cookies, userAgent ->
                            currentCookies = cookies
                            currentUa = userAgent
                            challenge.onCompleted(
                                AntiBotResolutionResult(
                                    success = true,
                                    cookies = cookies,
                                    userAgent = userAgent
                                )
                            )
                        },
                        onError = { _ ->
                            // Errors logged; user can retry or cancel
                        },
                        onCookiesChanged = { cookies ->
                            currentCookies = cookies
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { challenge.onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (currentCookies.isNotEmpty()) {
                                challenge.onCompleted(
                                    AntiBotResolutionResult(
                                        success = true,
                                        cookies = currentCookies,
                                        userAgent = currentUa
                                    )
                                )
                            } else {
                                // If no clearance was collected, complete with failure to prevent infinite retry loops
                                challenge.onCompleted(
                                    AntiBotResolutionResult(
                                        success = false,
                                        errorMessage = "Verification challenge was not completed"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "I'm Verified",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
