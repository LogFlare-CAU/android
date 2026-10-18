package com.logflare.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginRoute(onSignedIn: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == LoginEvent.SignedIn) signedIn() }
    }
    LoginContent(
        state = state,
        onUrl = viewModel::onUrl,
        onUsername = viewModel::onUsername,
        onPassword = viewModel::onPassword,
        onOpenRecent = viewModel::openRecent,
        onPickRecent = viewModel::pickRecent,
        onDismissRecent = viewModel::dismissRecent,
        onSubmit = viewModel::submit,
    )
}
