package dev.akexorcist.githubviewer.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun LastUpdatedText(instant: Instant, modifier: Modifier = Modifier) {
    Text(
        text = "Updated ${instant.toRelativeString()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private fun Instant.toRelativeString(): String {
    val diff = Clock.System.now() - this
    return when {
        diff < 1.minutes -> "just now"
        diff < 1.hours -> "${diff.inWholeMinutes} min ago"
        diff.inWholeHours < 24 -> "${diff.inWholeHours}h ago"
        else -> "${diff.inWholeDays}d ago"
    }
}
