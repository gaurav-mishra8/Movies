package com.example.rekindle.movies

import com.example.rekindle.movies.model.Movie

data class MoviesState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
