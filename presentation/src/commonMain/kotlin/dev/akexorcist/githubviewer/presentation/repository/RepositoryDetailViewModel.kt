package dev.akexorcist.githubviewer.presentation.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
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

class RepositoryDetailViewModel(
    private val owner: String,
    private val repo: String,
    private val repositoryRepository: RepositoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoryDetailUiState())
    val uiState: StateFlow<RepositoryDetailUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<RepositoryDetailSnackbarEvent>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    init {
        loadRepository(forceRefresh = false)
    }

    fun onRefresh() = loadRepository(forceRefresh = true)

    private fun loadRepository(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.repository == null, error = null) }

            repositoryRepository.getRepository(owner, repo, forceRefresh)
                .onEach { result ->
                    when (result) {
                        is Result.Success -> _uiState.update {
                            it.copy(
                                repository = result.data,
                                isLoading = false,
                                lastUpdatedAt = Clock.System.now(),
                            )
                        }
                        is Result.Error -> {
                            if (result.error is AppError.NetworkError) {
                                _snackbarEvent.trySend(RepositoryDetailSnackbarEvent.NoInternet)
                            } else {
                                _uiState.update { it.copy(error = result.error, isLoading = false) }
                            }
                        }
                    }
                }
                .launchIn(this)
        }
    }
}
