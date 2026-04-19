package dev.akexorcist.githubviewer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PAGE_SIZE
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val userRepository: UserRepository,
    private val repositoryRepository: RepositoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<SearchSnackbarEvent>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var lastSearchedQuery: String = ""

    init {
        queryFlow
            .debounce(500L)
            .filter { it.isNotBlank() }
            .onEach { query -> if (query != lastSearchedQuery) executeSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(users = SectionState(), repositories = SectionState()) }
        }
    }

    fun onSearchClick() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        lastSearchedQuery = query
        searchJob?.cancel()
        executeSearch(query)
    }

    fun onLoadMoreUsers() {
        val state = _uiState.value
        if (!state.users.hasNextPage || state.users.isLoadingMore) return
        val nextPage = (state.users.items.size / PAGE_SIZE) + 1
        viewModelScope.launch { fetchUsers(state.query, nextPage, append = true) }
    }

    fun onLoadMoreRepositories() {
        val state = _uiState.value
        if (!state.repositories.hasNextPage || state.repositories.isLoadingMore) return
        val nextPage = (state.repositories.items.size / PAGE_SIZE) + 1
        viewModelScope.launch { fetchRepositories(state.query, nextPage, append = true) }
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    users = SectionState(isLoading = true),
                    repositories = SectionState(isLoading = true),
                )
            }
            launch { fetchUsers(query, page = 1, append = false) }
            launch { fetchRepositories(query, page = 1, append = false) }
        }
    }

    private suspend fun fetchUsers(query: String, page: Int, append: Boolean) {
        if (append) _uiState.update { it.copy(users = it.users.copy(isLoadingMore = true)) }

        when (val result = userRepository.searchUsers(query, page)) {
            is Result.Success -> {
                _uiState.update { state ->
                    val pageResult = result.data
                    val updatedItems = if (append) state.users.items + pageResult.items else pageResult.items
                    state.copy(
                        users = SectionState(
                            items = updatedItems,
                            isLoading = false,
                            hasNextPage = pageResult.hasNextPage,
                        )
                    )
                }
            }
            is Result.Error -> {
                if (result.error is AppError.NetworkError) {
                    _snackbarEvent.trySend(SearchSnackbarEvent.NoInternet)
                }
                _uiState.update { state ->
                    state.copy(users = state.users.copy(isLoading = false, isLoadingMore = false, error = result.error))
                }
            }
        }
    }

    private suspend fun fetchRepositories(query: String, page: Int, append: Boolean) {
        if (append) _uiState.update { it.copy(repositories = it.repositories.copy(isLoadingMore = true)) }

        when (val result = repositoryRepository.searchRepositories(query, page)) {
            is Result.Success -> {
                _uiState.update { state ->
                    val pageResult = result.data
                    val updatedItems = if (append) state.repositories.items + pageResult.items else pageResult.items
                    state.copy(
                        repositories = SectionState(
                            items = updatedItems,
                            isLoading = false,
                            hasNextPage = pageResult.hasNextPage,
                        )
                    )
                }
            }
            is Result.Error -> {
                if (result.error is AppError.NetworkError) {
                    _snackbarEvent.trySend(SearchSnackbarEvent.NoInternet)
                }
                _uiState.update { state ->
                    state.copy(repositories = state.repositories.copy(isLoading = false, isLoadingMore = false, error = result.error))
                }
            }
        }
    }
}
