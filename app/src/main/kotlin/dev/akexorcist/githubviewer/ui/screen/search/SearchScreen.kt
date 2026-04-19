package dev.akexorcist.githubviewer.ui.screen.search

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.akexorcist.githubviewer.R
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.presentation.search.SearchSnackbarEvent
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel
import dev.akexorcist.githubviewer.presentation.search.SectionState
import dev.akexorcist.githubviewer.ui.theme.GithubViewerTheme

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onUserClick: (login: String) -> Unit,
    onRepoClick: (owner: String, repo: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val noInternetMessage = stringResource(R.string.error_no_internet_connection)

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            when (event) {
                is SearchSnackbarEvent.NoInternet ->
                    snackbarHostState.showSnackbar(noInternetMessage)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search GitHub...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = viewModel::onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }

            if (uiState.query.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Search for users or repositories",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SectionHeader("Users")
                    }
                    when {
                        uiState.users.isLoading -> item { SectionLoader() }
                        uiState.users.error != null -> item {
                            SectionError(onRetry = viewModel::onSearchClick)
                        }
                        uiState.users.items.isEmpty() -> item { SectionEmpty("No users found") }
                        else -> {
                            items(uiState.users.items, key = { it.id }) { user ->
                                UserResultItem(user = user, onClick = { onUserClick(user.login) })
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (uiState.users.hasNextPage) {
                                item {
                                    LoadMoreButton(
                                        isLoading = uiState.users.isLoadingMore,
                                        onClick = viewModel::onLoadMoreUsers,
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                    item { SectionHeader("Repositories") }

                    when {
                        uiState.repositories.isLoading -> item { SectionLoader() }
                        uiState.repositories.error != null -> item {
                            SectionError(onRetry = viewModel::onSearchClick)
                        }
                        uiState.repositories.items.isEmpty() -> item { SectionEmpty("No repositories found") }
                        else -> {
                            items(uiState.repositories.items, key = { it.id }) { repo ->
                                RepositoryResultItem(
                                    repo = repo,
                                    onClick = { onRepoClick(repo.ownerLogin, repo.name) },
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (uiState.repositories.hasNextPage) {
                                item {
                                    LoadMoreButton(
                                        isLoading = uiState.repositories.isLoadingMore,
                                        onClick = viewModel::onLoadMoreRepositories,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SectionEmpty(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun LoadMoreButton(isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onClick) { Text("Load more") }
        }
    }
}

@Composable
private fun UserResultItem(user: SearchUserItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "${user.login} avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = user.name ?: user.login,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.name != null) {
                Text(
                    text = user.login,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RepositoryResultItem(repo: Repository, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = repo.fullName,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

private val previewSearchUser = SearchUserItem(
    id = 1L,
    login = "akexorcist",
    name = "Akexorcist",
    avatarUrl = "",
)

private val previewSearchRepo = Repository(
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
    topics = listOf("android", "kotlin", "compose"),
    licenseName = "Apache 2.0",
    pushedAt = "2024-01-15T10:30:00Z",
)

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    GithubViewerTheme {
        SectionHeader("Users")
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionLoaderPreview() {
    GithubViewerTheme {
        SectionLoader()
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionEmptyPreview() {
    GithubViewerTheme {
        SectionEmpty("No users found")
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionErrorPreview() {
    GithubViewerTheme {
        SectionError(onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadMoreButtonPreview() {
    GithubViewerTheme {
        LoadMoreButton(isLoading = false, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadMoreButtonLoadingPreview() {
    GithubViewerTheme {
        LoadMoreButton(isLoading = true, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun UserResultItemPreview() {
    GithubViewerTheme {
        UserResultItem(user = previewSearchUser, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun UserResultItemNoNamePreview() {
    GithubViewerTheme {
        UserResultItem(user = previewSearchUser.copy(name = null), onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryResultItemPreview() {
    GithubViewerTheme {
        RepositoryResultItem(repo = previewSearchRepo, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryResultItemNoDescriptionPreview() {
    GithubViewerTheme {
        RepositoryResultItem(repo = previewSearchRepo.copy(description = null, language = null), onClick = {})
    }
}
