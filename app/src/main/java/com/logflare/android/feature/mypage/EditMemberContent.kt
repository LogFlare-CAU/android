package com.logflare.android.feature.mypage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.Avatar
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.DestructiveTextButton
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.PasswordField
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.RoleChip
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.ui.VisualQaTags

@Composable
fun EditMemberRoute(
    onBack: () -> Unit,
    /** Saved, and the viewer can no longer edit the member: back to My, which shows "Saved". */
    onSavedAndLeft: () -> Unit,
    viewModel: EditMemberViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val saved = stringResource(R.string.member_saved)
    val saveFailed = stringResource(R.string.member_save_failed)
    val taken = stringResource(R.string.member_name_taken)
    val deleteFailed = stringResource(R.string.member_delete_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                EditMemberEvent.Saved -> toast.show(saved)
                EditMemberEvent.SaveFailed -> toast.show(saveFailed)
                EditMemberEvent.NameTaken -> toast.show(taken)
                EditMemberEvent.DeleteFailed -> toast.show(deleteFailed)
                EditMemberEvent.Deleted -> onBack()
                EditMemberEvent.SavedAndLeft -> onSavedAndLeft()
            }
        }
    }
    EditMemberContent(
        state = state,
        actions = EditMemberActions(
            onBack = onBack,
            onName = viewModel::setName,
            onPassword = viewModel::setPassword,
            onRole = viewModel::setRole,
            onSave = viewModel::save,
            onDelete = viewModel::openDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissSheet = viewModel::dismissSheet,
            onRetry = viewModel::retry,
        ),
        toast = toast,
    )
}

data class EditMemberActions(
    val onBack: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onPassword: (String) -> Unit = {},
    val onRole: (MemberRole) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onDismissSheet: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
fun EditMemberContent(
    state: EditMemberUiState,
    actions: EditMemberActions,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    val member = state.member
    Box(modifier.fillMaxSize().testTag(VisualQaTags.EditMember)) {
        Column(Modifier.fillMaxSize()) {
            SubpageTopBar(title = member?.username.orEmpty(), onNav = actions.onBack) {
                if (state.dirty) RoleChip(stringResource(R.string.member_unsaved), Modifier.padding(end = 12.dp))
            }
            when {
                member == null -> Unavailable(state.load, actions.onRetry)
                state.lock != null -> Locked(member, state.lock)
                else -> EditForm(state, actions, Modifier.weight(1f))
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (state.deleteOpen && member != null) {
        ConfirmSheet(
            title = stringResource(R.string.member_delete_title, member.username),
            body = stringResource(R.string.member_delete_body),
            confirmLabel = stringResource(R.string.member_delete_confirm),
            dismissLabel = stringResource(R.string.member_delete_cancel),
            destructive = true,
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissSheet,
        )
    }
}

@Composable
private fun EditForm(state: EditMemberUiState, actions: EditMemberActions, modifier: Modifier) {
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                AppTextField(
                    value = state.name,
                    onValueChange = actions.onName,
                    label = stringResource(R.string.member_name_label),
                    error = when (val check = state.nameCheck) {
                        NameCheck.Empty -> stringResource(R.string.member_name_empty)
                        is NameCheck.Invalid -> memberNameProblemText(check.reason)
                        NameCheck.Valid -> null
                    },
                    keyboardOptions = MemberNameKeyboard,
                )
                PasswordField(
                    value = state.password,
                    onValueChange = actions.onPassword,
                    label = stringResource(R.string.member_new_password_label),
                    placeholder = stringResource(R.string.member_new_password_placeholder),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.padding(top = 20.dp),
                )
                if (state.password.isNotEmpty()) PasswordRuleList(state.passwordRules, Modifier.padding(top = 10.dp))
            }
            RoleCard(state.role, actions.onRole)
            GroupedCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                DestructiveTextButton(stringResource(R.string.member_delete), onClick = actions.onDelete)
            }
        }
        PrimaryButton(
            text = stringResource(R.string.member_save),
            onClick = actions.onSave,
            enabled = state.canSave || state.saving,
            loading = state.saving,
            modifier = BottomButtonModifier,
        )
    }
}

/** Who the member is, and why the viewer can't change them. */
@Composable
private fun Locked(member: Member, lock: MemberLock) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupedCard {
            ListRow(
                title = member.username,
                leading = { Avatar(member.username) },
                trailing = { RoleChip(roleLabel(member.permission)) },
            )
        }
        Text(
            stringResource(
                when (lock) {
                    MemberLock.SuperAdmin -> R.string.member_locked_super_admin
                    MemberLock.Self -> R.string.member_locked_self
                    MemberLock.Role -> R.string.member_locked_role
                },
            ),
            style = Lf.type.secondary,
            color = Lf.colors.textMuted,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun Unavailable(load: EditLoad, onRetry: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        when (load) {
            EditLoad.Loading, EditLoad.Ready -> SkeletonCard(rows = 2)
            EditLoad.Failed -> EmptyState(
                title = stringResource(R.string.member_load_failed),
                body = null,
                actionLabel = stringResource(R.string.member_load_retry),
                onAction = onRetry,
            )
            EditLoad.Missing -> EmptyState(
                title = stringResource(R.string.member_missing),
                body = stringResource(R.string.member_missing_body),
            )
        }
    }
}
