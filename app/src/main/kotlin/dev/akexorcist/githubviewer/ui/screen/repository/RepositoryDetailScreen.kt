package dev.akexorcist.githubviewer.ui.screen.repository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import dev.akexorcist.githubviewer.R
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailSnackbarEvent
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailUiState
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import dev.akexorcist.githubviewer.ui.component.LastUpdatedText
import dev.akexorcist.githubviewer.ui.component.ScreenErrorState
import dev.akexorcist.githubviewer.ui.theme.GithubViewerTheme
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDetailScreen(
    viewModel: RepositoryDetailViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val noInternetMessage = stringResource(R.string.error_no_internet_connection)

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            when (event) {
                is RepositoryDetailSnackbarEvent.NoInternet ->
                    snackbarHostState.showSnackbar(noInternetMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.repository?.name ?: "",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.content_description_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null -> ScreenErrorState(
                message = stringResource(R.string.error_failed_to_load_repository),
                onRetry = viewModel::onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            uiState.repository != null -> {
                val repository = checkNotNull(uiState.repository)
                RepositoryDetail(
                    repository = repository,
                    lastUpdatedAt = uiState.lastUpdatedAt,
                    readmeContent = uiState.readmeContent,
                    isReadmeLoading = uiState.isReadmeLoading,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepositoryDetail(
    repository: Repository,
    lastUpdatedAt: Instant?,
    readmeContent: String?,
    isReadmeLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = repository.fullName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = repository.ownerLogin,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            repository.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StatItem(label = "Stars", value = repository.stars.toString())
            StatItem(label = "Forks", value = repository.forks.toString())
            StatItem(label = "Issues", value = repository.openIssues.toString())
            StatItem(label = "Watchers", value = repository.watchers.toString())
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repository.language?.let { InfoRow(label = "Language", value = it) }
            repository.licenseName?.let { InfoRow(label = "License", value = it) }
            repository.pushedAt?.let { InfoRow(label = "Last pushed", value = it.take(10)) }
        }

        if (repository.topics.isNotEmpty()) {
            HorizontalDivider()
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repository.topics.forEach { topic ->
                    AssistChip(onClick = {}, label = { Text(topic) })
                }
            }
        }

        lastUpdatedAt?.let {
            HorizontalDivider()
            LastUpdatedText(
                instant = it,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        HorizontalDivider()
        ReadmeSection(
            readmeContent = readmeContent,
            isReadmeLoading = isReadmeLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadmeSection(
    readmeContent: String?,
    isReadmeLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.readme_section_title),
            style = MaterialTheme.typography.titleMedium,
        )
        when {
            isReadmeLoading && readmeContent == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("readme_loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            readmeContent == "" -> Text(
                text = stringResource(R.string.readme_not_available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("readme_empty"),
            )
            readmeContent != null -> Markdown(
                content = readmeContent,
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("readme_content"),
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

private val previewRepository = Repository(
    id = 1L,
    name = "Github-Viewer",
    fullName = "akexorcist/Github-Viewer",
    ownerLogin = "akexorcist",
    ownerAvatarUrl = "",
    description = "A GitHub viewer app built with Kotlin Multiplatform and Jetpack Compose",
    stars = 128,
    forks = 24,
    openIssues = 5,
    watchers = 128,
    language = "Kotlin",
    topics = listOf("android", "kotlin", "compose", "kmp"),
    licenseName = "Apache 2.0",
    pushedAt = "2024-01-15T10:30:00Z",
)

@Preview(showBackground = true)
@Composable
private fun RepositoryDetailPreview() {
    GithubViewerTheme {
        RepositoryDetail(
            repository = previewRepository,
            lastUpdatedAt = null,
            readmeContent = "# Github Viewer\n\nA GitHub viewer app built with **Kotlin Multiplatform**.",
            isReadmeLoading = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryDetailNoTopicsPreview() {
    GithubViewerTheme {
        RepositoryDetail(
            repository = previewRepository.copy(topics = emptyList(), description = null),
            lastUpdatedAt = null,
            readmeContent = null,
            isReadmeLoading = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatItemPreview() {
    GithubViewerTheme {
        StatItem(label = "Stars", value = "128")
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoRowPreview() {
    GithubViewerTheme {
        InfoRow(label = "Language", value = "Kotlin")
    }
}
