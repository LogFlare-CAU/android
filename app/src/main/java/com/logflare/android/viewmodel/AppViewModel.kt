package com.logflare.android.viewmodel

import androidx.lifecycle.ViewModel
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    /** [SessionState.Loading] until the stored session is read; the splash screen waits on it. */
    val session: StateFlow<SessionState> = sessionRepository.state
}
