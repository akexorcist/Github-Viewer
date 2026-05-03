package dev.akexorcist.githubviewer.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.akexorcist.githubviewer.R
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PagingState
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.User
import dev.akexorcist.githubviewer.presentation.profile.UserProfileSnackbarEvent
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import kotlin.time.Instant
import dev.akexorcist.githubviewer.ui.component.LastUpdatedText
import dev.akexorcist.githubviewer.ui.component.ScreenErrorState
import dev.akexorcist.githubviewer.ui.theme.GithubViewerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onRepoClick: (owner: String, repo: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val noInternetMessage = stringResource(R.string.error_no_internet_connection)

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            when (event) {
                is UserProfileSnackbarEvent.NoInternet ->
                    snackbarHostState.showSnackbar(noInternetMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.login ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                message = stringResource(R.string.error_failed_to_load_profile),
                onRetry = viewModel::onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            uiState.user != null -> {
                val user = uiState.user ?: return@Scaffold
                UserProfileLoaded(
                    user = user,
                    repositories = uiState.repositories,
                    lastUpdatedAt = uiState.lastUpdatedAt,
                    onRefresh = viewModel::onRefresh,
                    onLoadMoreRepositories = viewModel::onLoadMoreRepositories,
                    onRepoClick = onRepoClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun UserProfileLoaded(
    user: User,
    repositories: PagingState<Repository>,
    lastUpdatedAt: Instant?,
    onRefresh: () -> Unit,
    onLoadMoreRepositories: () -> Unit,
    onRepoClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item { UserHeader(user = user) }

        lastUpdatedAt?.let {
            item {
                LastUpdatedText(
                    instant = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item {
            Text(
                text = "Repositories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        items(repositories.items, key = { it.id }) { repo ->
            RepositoryItem(repo = repo, onClick = { onRepoClick(repo.ownerLogin, repo.name) })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (repositories.error != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.error_failed_to_load_repositories))
                    Button(onClick = onRefresh) { Text("Retry") }
                }
            }
        }

        if (repositories.hasNextPage) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (repositories.isLoadingMore) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = onLoadMoreRepositories) { Text("Load more") }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserHeader(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "${user.login} avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                user.name?.let {
                    Text(text = it, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = user.login,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        user.bio?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        user.location?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${user.publicRepos} repos",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${user.followers} followers",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${user.following} following",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun RepositoryItem(repo: Repository, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = repo.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        repo.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repo.language?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "★ ${repo.stars}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

private val previewUser = User(
    id = 1L,
    login = "akexorcist",
    name = "Akexorcist",
    avatarUrl = "",
    bio = "Android Developer @ Bangkok",
    location = "Bangkok, Thailand",
    blog = null,
    publicRepos = 42,
    followers = 1200,
    following = 80,
)

private val previewRepo = Repository(
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UserProfileScreenLoadingPreview() {
    GithubViewerTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UserProfileScreenErrorPreview() {
    GithubViewerTheme {
        ScreenErrorState(
            message = "Failed to load profile",
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UserProfileScreenContentPreview() {
    GithubViewerTheme {
        UserProfileLoaded(
            user = previewUser,
            repositories = PagingState(
                items = listOf(previewRepo, previewRepo.copy(id = 2, name = "kotlin-extensions", description = null, language = null)),
                hasNextPage = true,
            ),
            lastUpdatedAt = null,
            onRefresh = {},
            onLoadMoreRepositories = {},
            onRepoClick = { _, _ -> },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UserProfileScreenRepoErrorPreview() {
    GithubViewerTheme {
        UserProfileLoaded(
            user = previewUser,
            repositories = PagingState(error = AppError.UnknownError),
            lastUpdatedAt = null,
            onRefresh = {},
            onLoadMoreRepositories = {},
            onRepoClick = { _, _ -> },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserHeaderPreview() {
    GithubViewerTheme {
        UserHeader(user = previewUser)
    }
}

@Preview(showBackground = true)
@Composable
private fun UserHeaderNoBioPreview() {
    GithubViewerTheme {
        UserHeader(user = previewUser.copy(name = null, bio = null, location = null))
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryItemPreview() {
    GithubViewerTheme {
        RepositoryItem(repo = previewRepo, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryItemNoDescriptionPreview() {
    GithubViewerTheme {
        RepositoryItem(repo = previewRepo.copy(description = null, language = null), onClick = {})
    }
}

