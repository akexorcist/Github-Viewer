package dev.akexorcist.githubviewer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PagingState
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class UserProfileViewModel(
    private val login: String,
    private val userRepository: UserRepository,
    private val repositoryRepository: RepositoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<UserProfileSnackbarEvent>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    init {
        loadProfile(forceRefresh = false)
    }

    fun onRefresh() = loadProfile(forceRefresh = true)

    fun onLoadMoreRepositories() {
        val state = _uiState.value
        if (!state.repositories.hasNextPage || state.repositories.isLoadingMore) return
        val nextPage = state.repositories.currentPage + 1
        viewModelScope.launch { fetchRepositories(page = nextPage, append = true) }
    }

    private fun loadProfile(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.user == null, error = null) }

            userRepository.getUser(login, forceRefresh)
                .onEach { result ->
                    when (result) {
                        is Result.Success -> _uiState.update {
                            it.copy(user = result.data, isLoading = false, lastUpdatedAt = Clock.System.now())
                        }
                        is Result.Error -> {
                            handleError(result.error)
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                }
                .launchIn(this)

            fetchRepositories(page = 1, append = false)
        }
    }

    private suspend fun fetchRepositories(page: Int, append: Boolean) {
        if (append) _uiState.update { it.copy(repositories = it.repositories.loadingMore()) }

        val forceRefresh = append || _uiState.value.repositories.items.isEmpty()
        repositoryRepository.getUserRepositories(login, page, forceRefresh)
            .onEach { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            val pageResult = result.data
                            val updated = if (append) state.repositories.appendPage(pageResult)
                            else PagingState(
                                items = pageResult.items,
                                currentPage = pageResult.page,
                                hasNextPage = pageResult.hasNextPage,
                            )
                            state.copy(repositories = updated)
                        }
                    }
                    is Result.Error -> handleError(result.error)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleError(error: AppError) {
        if (error is AppError.NetworkError) {
            _snackbarEvent.trySend(UserProfileSnackbarEvent.NoInternet)
        } else {
            _uiState.update { it.copy(error = error) }
        }
    }
}
