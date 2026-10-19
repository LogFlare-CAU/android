package com.logflare.android.feature.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.logflare.android.ui.copyText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.KeywordChipInput
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.NavIcon
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.ThresholdLevelSelector
import com.example.logflare.core.designsystem.v2.components.TokenField
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.ui.VisualQaTags

@Composable
fun ProjectCreateRoute(
    onBack: () -> Unit,
    onOpenProject: (id: Int) -> Unit,
    viewModel: ProjectCreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val appContext = LocalContext.current
    val createFailed = stringResource(R.string.project_create_failed)
    val rotated = stringResource(R.string.project_create_token_rotated)
    val rotateFailed = stringResource(R.string.project_create_rotate_failed)
    val alertsFailed = stringResource(R.string.project_create_alerts_failed)
    val copied = stringResource(R.string.project_create_token_copied)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProjectCreateEvent.NavigateBack -> onBack()
                ProjectCreateEvent.CreateFailed -> toast.show(createFailed)
                ProjectCreateEvent.TokenRotated -> toast.show(rotated)
                ProjectCreateEvent.RotateFailed -> toast.show(rotateFailed)
                ProjectCreateEvent.AlertSettingsFailed -> toast.show(alertsFailed)
            }
        }
    }
    BackHandler(enabled = state.created == null && state.dirty, onBack = viewModel::close)
    val duplicate = stringResource(R.string.project_create_keyword_duplicate)
    ProjectCreateContent(
        state = state,
        onClose = viewModel::close,
        onName = viewModel::setName,
        onLevel = viewModel::setLevel,
        onAddKeyword = viewModel::addKeyword,
        onRemoveKeyword = viewModel::removeKeyword,
        onKeywordDuplicate = { toast.show(duplicate) },
        onCreate = viewModel::create,
        onToggleReveal = viewModel::toggleReveal,
        onCopy = {
            state.created?.let {
                appContext.copyText(it.token, sensitive = true)
                toast.show(copied)
            }
        },
        onOpenRotate = viewModel::openRotate,
        onConfirmRotate = viewModel::confirmRotate,
        onConfirmDiscard = viewModel::confirmDiscard,
        onDismissSheet = viewModel::dismissSheet,
        onGoToProject = { state.created?.let { onOpenProject(it.id) } },
        toast = toast,
    )
}

@Composable
fun ProjectCreateContent(
    state: ProjectCreateUiState,
    onClose: () -> Unit,
    onName: (String) -> Unit,
    onLevel: (LfLevel) -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onKeywordDuplicate: () -> Unit,
    onCreate: () -> Unit,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onOpenRotate: () -> Unit,
    onConfirmRotate: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissSheet: () -> Unit,
    onGoToProject: () -> Unit,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.ProjectCreate)) {
        Column(Modifier.fillMaxSize()) {
            val created = state.created
            SubpageTopBar(
                title = if (created == null) stringResource(R.string.project_create_title) else "",
                onNav = onClose,
                navIcon = NavIcon.Close,
            )
            if (created == null) {
                Form(state, onName, onLevel, onAddKeyword, onRemoveKeyword, onKeywordDuplicate, onCreate, Modifier.weight(1f))
            } else {
                Created(state, created, onToggleReveal, onCopy, onOpenRotate, onGoToProject, Modifier.weight(1f))
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    when (state.sheet) {
        CreateSheet.Discard -> ConfirmSheet(
            title = stringResource(R.string.project_create_discard_title),
            body = stringResource(R.string.project_create_discard_body),
            confirmLabel = stringResource(R.string.project_create_discard),
            dismissLabel = stringResource(R.string.project_create_keep_editing),
            destructive = true,
            onConfirm = onConfirmDiscard,
            onDismiss = onDismissSheet,
        )
        CreateSheet.Rotate -> ConfirmSheet(
            title = stringResource(R.string.project_create_rotate_title),
            body = stringResource(R.string.project_create_rotate_body),
            confirmLabel = stringResource(R.string.project_create_rotate),
            destructive = false,
            onConfirm = onConfirmRotate,
            onDismiss = onDismissSheet,
        )
        null -> Unit
    }
}

@Composable
private fun Form(
    state: ProjectCreateUiState,
    onName: (String) -> Unit,
    onLevel: (LfLevel) -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onKeywordDuplicate: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier,
) {
    val canCreate = state.nameCheck == NameCheck.Valid && !state.submitting
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                val invalid = (state.nameCheck as? NameCheck.Invalid)?.let { nameProblemText(it.reason) }
                AppTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = stringResource(R.string.project_create_name_label),
                    placeholder = stringResource(R.string.project_create_name_placeholder),
                    helper = if (state.nameCheck == NameCheck.Empty) stringResource(R.string.project_create_name_helper) else null,
                    error = invalid,
                    success = if (state.nameCheck == NameCheck.Valid) stringResource(R.string.project_create_name_available) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canCreate) onCreate() }),
                )
            }
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                SectionHeader(R.string.project_create_level_title, R.string.project_create_level_body)
                ThresholdLevelSelector(
                    selected = state.level,
                    onSelect = onLevel,
                    summary = if (state.level == LfLevel.Critical) {
                        stringResource(R.string.alerts_for_critical_only)
                    } else {
                        stringResource(R.string.alerts_for_level_and_above, state.level.name)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                SectionHeader(R.string.project_create_keywords_title, R.string.project_create_keywords_body)
                KeywordChipInput(
                    keywords = state.keywords,
                    onAdd = onAddKeyword,
                    onRemove = onRemoveKeyword,
                    placeholder = stringResource(R.string.project_create_keyword_placeholder),
                    onDuplicate = onKeywordDuplicate,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                stringResource(R.string.project_create_token_hint),
                style = Lf.type.secondary,
                color = Lf.colors.textMuted,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        PrimaryButton(
            text = stringResource(R.string.project_create_submit),
            onClick = onCreate,
            enabled = canCreate,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        )
    }
}

@Composable
internal fun nameProblemText(problem: NameProblem): String = stringResource(
    when (problem) {
        NameProblem.Spaces -> R.string.project_name_spaces
        NameProblem.Characters -> R.string.project_name_characters
        NameProblem.Duplicate -> R.string.project_name_duplicate
        NameProblem.TooLong -> R.string.project_name_too_long
        NameProblem.Format -> R.string.member_name_format
    },
)

@Composable
private fun SectionHeader(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(title), style = Lf.type.sectionTitle, color = Lf.colors.textPrimary)
        Text(stringResource(body), style = Lf.type.caption, color = Lf.colors.textMuted)
    }
}

@Composable
private fun Created(
    state: ProjectCreateUiState,
    created: CreatedProject,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onOpenRotate: () -> Unit,
    onGoToProject: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Lf.colors.primaryAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Lf.colors.onPrimaryAction, modifier = Modifier.size(28.dp))
                }
                Text(
                    stringResource(R.string.project_create_ready, created.name),
                    style = Lf.type.largeTitle,
                    color = Lf.colors.textPrimary,
                )
                Text(stringResource(R.string.project_create_ready_body), style = Lf.type.body, color = Lf.colors.textSecondary)
            }
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                TokenField(
                    token = created.token,
                    revealed = state.tokenRevealed,
                    onToggleReveal = onToggleReveal,
                    onCopy = onCopy,
                    onRotate = onOpenRotate,
                )
            }
            if (state.serverUrl.isNotBlank()) GroupedCard(contentPadding = PaddingValues(20.dp)) {
                Text(stringResource(R.string.project_create_first_log), style = Lf.type.sectionTitle, color = Lf.colors.textPrimary)
                Text(
                    curlSnippet(state.serverUrl, created.name, created.token),
                    style = Lf.type.caption.copy(fontFamily = FontFamily.Monospace),
                    color = Lf.colors.textSecondary,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(Lf.shapes.input)
                        .background(Lf.colors.background)
                        .padding(14.dp),
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.project_create_go_to_project),
            onClick = onGoToProject,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        )
    }
}
