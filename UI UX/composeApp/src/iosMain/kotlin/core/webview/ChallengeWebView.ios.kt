package core.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun ChallengeWebView(
    url: String,
    userAgent: String,
    onChallengeResolved: (cookies: Map<String, String>, userAgent: String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF14171E))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "iOS Verification ($url)",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
