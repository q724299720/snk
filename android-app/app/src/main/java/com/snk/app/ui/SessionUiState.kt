package com.snk.app.ui

import com.snk.app.data.auth.AnonymousSession

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Remote(val session: AnonymousSession) : SessionUiState
    data class Cached(val session: AnonymousSession, val reason: String) : SessionUiState
    data class Failure(val reason: String) : SessionUiState
}
