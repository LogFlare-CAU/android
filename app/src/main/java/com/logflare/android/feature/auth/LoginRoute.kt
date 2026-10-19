package com.logflare.android.feature.auth

import androidx.activity.compose.BackHandler
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
    // Confirm goes back to the step it came from; scan and manual entry go back to the form.
    BackHandler(enabled = state.step != LoginStep.Form, onBack = viewModel::back)
    LoginContent(
        state = state,
        onUrl = viewModel::onUrl,
        onUsername = viewModel::onUsername,
        onPassword = viewModel::onPassword,
        onOpenRecent = viewModel::openRecent,
        onPickRecent = viewModel::pickRecent,
        onDismissRecent = viewModel::dismissRecent,
        onSubmit = viewModel::submit,
        onSelectMode = viewModel::selectMode,
        onCommitUrl = viewModel::commitUrl,
        onScan = viewModel::scan,
        onEnterManually = viewModel::enterManual,
        onQr = viewModel::onQr,
        onManualContinue = viewModel::submitManual,
        onManualEdit = viewModel::onManualEdit,
        onManualServer = viewModel::onManualServer,
        onKeepLoggedIn = viewModel::setKeepLoggedIn,
        onConfirmAccess = viewModel::confirmAccess,
        onBack = viewModel::back,
    )
}
