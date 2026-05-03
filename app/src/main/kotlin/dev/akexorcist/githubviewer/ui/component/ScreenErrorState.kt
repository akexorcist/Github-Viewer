package dev.akexorcist.githubviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.akexorcist.githubviewer.ui.theme.GithubViewerTheme

@Composable
fun ScreenErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun ScreenErrorStatePreview() {
    GithubViewerTheme {
        ScreenErrorState(
            message = "Failed to load data",
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
