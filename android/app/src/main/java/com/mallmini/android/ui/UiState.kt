package com.mallmini.android.ui

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null,
) {
    fun withLoading(): UiState<T> = copy(loading = true, error = null)

    fun withError(message: String): UiState<T> = copy(loading = false, error = message)
}
