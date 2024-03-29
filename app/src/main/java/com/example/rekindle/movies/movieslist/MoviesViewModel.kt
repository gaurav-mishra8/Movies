package com.example.rekindle.movies.movieslist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rekindle.Result
import com.example.rekindle.movies.data.MoviesRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val moviesRepo: MoviesRepo,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val searchMovies = MutableStateFlow("")
    private val latestMovies = MutableStateFlow(Unit)
    private val isSearchActive = savedStateHandle.getStateFlow("isSearchActive", false)

    init {
        val query = savedStateHandle.get<String?>("query")
        query?.let {
            searchMovies(query)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MoviesState> =
        combine(latestMovies, searchMovies, isSearchActive) { _, searchText, _ ->
            if (searchText.isNotEmpty()) {
                searchMovies
                    .debounce(200)
                    .flatMapLatest { query ->
                        if (query.length < 3) {
                            return@flatMapLatest flowOf(Result.Success(emptyList()))
                        } else {
                            return@flatMapLatest moviesRepo.searchMovie(query)
                        }
                    }
            } else {
                moviesRepo.getLatestMovies()
            }
        }.flatMapLatest { resultFlow ->
            resultFlow.map { result ->
                when (result) {
                    is Result.Success -> {
                        MoviesState(
                            movieList = result.data,
                            isLoading = false,
                            error = null,
                            isSearchMode = isSearchActive.value
                        )
                    }

                    is Result.Loading -> {
                        MoviesState(
                            movieList = emptyList(),
                            isLoading = true,
                            error = null,
                            isSearchMode = isSearchActive.value
                        )
                    }

                    is Result.Error -> {
                        MoviesState(
                            movieList = emptyList(),
                            isLoading = false,
                            error = result.exception?.message ?: "Something went wrong",
                            isSearchMode = isSearchActive.value
                        )
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoviesState()
        )

    fun searchMovies(query: String?) {
        query?.let {
            searchMovies.value = it
            savedStateHandle["query"] = it
        }
    }

    fun toggleSearch() {
        savedStateHandle["isSearchActive"] = !isSearchActive.value
        if (!isSearchActive.value) {
            searchMovies.value = ""
            savedStateHandle["query"] = ""
        }
    }
}