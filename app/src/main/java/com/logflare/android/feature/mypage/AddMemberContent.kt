package com.logflare.android.feature.mypage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.NavIcon
import com.example.logflare.core.designsystem.v2.components.PasswordField
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SecondaryButton
import com.example.logflare.core.designsystem.v2.components.SegmentedControl
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem

@Composable
fun AddMemberRoute(
    onBack: () -> Unit,
    viewModel: AddMemberViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val failed = stringResource(R.string.member_add_failed)
    val taken = stringResource(R.string.member_name_taken)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                AddMemberEvent.NavigateBack -> onBack()
                AddMemberEvent.AddFailed -> toast.show(failed)
                AddMemberEvent.NameTaken -> toast.show(taken)
            }
        }
    }
    BackHandler(enabled = state.dirty, onBack = viewModel::close)
    AddMemberContent(
        state = state,
        actions = AddMemberActions(
            onClose = viewModel::close,
            onName = viewModel::setName,
            onPassword = viewModel::setPassword,
            onRole = viewModel::setRole,
            onAdd = viewModel::add,
            onBackToMy = onBack,
            onAddAnother = viewModel::addAnother,
            onConfirmDiscard = viewModel::confirmDiscard,
            onDismissSheet = viewModel::dismissSheet,
        ),
        toast = toast,
    )
}

data class AddMemberActions(
    val onClose: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onPassword: (String) -> Unit = {},
    val onRole: (MemberRole) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onBackToMy: () -> Unit = {},
    val onAddAnother: () -> Unit = {},
    val onConfirmDiscard: () -> Unit = {},
    val onDismissSheet: () -> Unit = {},
)

@Composable
fun AddMemberContent(
    state: AddMemberUiState,
    actions: AddMemberActions,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // After adding, Close just leaves (nothing to lose).
            SubpageTopBar(
                title = stringResource(R.string.member_add_title),
                onNav = if (state.added == null) actions.onClose else actions.onBackToMy,
                navIcon = NavIcon.Close,
            )
            val added = state.added
            if (added == null) AddForm(state, actions, Modifier.weight(1f)) else Added(state, added, actions, Modifier.weight(1f))
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (state.discardOpen) {
        ConfirmSheet(
            title = stringResource(R.string.member_discard_title),
            body = stringResource(R.string.member_discard_body),
            confirmLabel = stringResource(R.string.member_discard),
            dismissLabel = stringResource(R.string.member_keep_editing),
            destructive = true,
            onConfirm = actions.onConfirmDiscard,
            onDismiss = actions.onDismissSheet,
        )
    }
}

@Composable
private fun AddForm(state: AddMemberUiState, actions: AddMemberActions, modifier: Modifier) {
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
                    placeholder = stringResource(R.string.member_name_placeholder),
                    error = (state.nameCheck as? NameCheck.Invalid)?.let { memberNameProblemText(it.reason) },
                    success = if (state.nameCheck == NameCheck.Valid) stringResource(R.string.member_name_available) else null,
                    keyboardOptions = MemberNameKeyboard,
                )
                PasswordField(
                    value = state.password,
                    onValueChange = actions.onPassword,
                    label = stringResource(R.string.member_password_label),
                    placeholder = stringResource(R.string.member_password_placeholder),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.padding(top = 20.dp),
                )
                PasswordRuleList(state.passwordRules, Modifier.padding(top = 10.dp))
            }
            RoleCard(state.role, actions.onRole)
        }
        PrimaryButton(
            text = stringResource(R.string.member_add_submit),
            onClick = actions.onAdd,
            enabled = state.canAdd || state.submitting,
            loading = state.submitting,
            modifier = BottomButtonModifier,
        )
    }
}

@Composable
private fun Added(state: AddMemberUiState, added: AddedMember, actions: AddMemberActions, modifier: Modifier) {
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Lf.colors.primaryAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Lf.colors.onPrimaryAction, modifier = Modifier.size(30.dp))
                }
                Text(
                    stringResource(R.string.member_added_title, added.name),
                    style = Lf.type.largeTitle,
                    color = Lf.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.member_added_body),
                    style = Lf.type.body,
                    color = Lf.colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
            GroupedCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                SummaryRow(stringResource(R.string.member_added_name), added.name)
                SummaryRow(stringResource(R.string.member_added_role), roleLabel(added.role.permission))
                if (state.serverHost.isNotEmpty()) SummaryRow(stringResource(R.string.member_added_server), state.serverHost)
            }
        }
        Column(BottomButtonModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = stringResource(R.string.member_back_to_my),
                onClick = actions.onBackToMy,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.member_add_another),
                onClick = actions.onAddAnother,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lf.type.body, color = Lf.colors.textMuted)
        Text(
            value,
            style = Lf.type.bodyStrong,
            color = Lf.colors.textPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

internal val MemberNameKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Ascii,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Next,
)

internal val BottomButtonModifier = Modifier
    .fillMaxWidth()
    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)

/** The password checklist: each rule with a check once met, else an empty circle. */
@Composable
internal fun PasswordRuleList(rules: PasswordRules, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RuleRow(stringResource(R.string.member_rule_length), rules.length)
        RuleRow(stringResource(R.string.member_rule_letter), rules.letter)
        RuleRow(stringResource(R.string.member_rule_number), rules.number)
    }
}

@Composable
private fun RuleRow(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val status = stringResource(if (met) R.string.member_rule_met else R.string.member_rule_unmet)
        if (met) {
            Icon(Icons.Filled.Check, contentDescription = status, tint = Lf.colors.textPrimary, modifier = Modifier.size(16.dp))
        } else {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(12.dp).border(2.dp, Lf.colors.icon, CircleShape).semantics { this.contentDescription = status })
            }
        }
        Text(
            label,
            style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold),
            color = if (met) Lf.colors.textPrimary else Lf.colors.textMuted,
        )
    }
}

/** Role: Member / Admin segments with what the selected role can do. */
@Composable
internal fun RoleCard(role: MemberRole, onRole: (MemberRole) -> Unit) {
    GroupedCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            stringResource(R.string.member_role),
            style = Lf.type.sectionTitle,
            color = Lf.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        SegmentedControl(
            options = MemberRole.entries.map { roleLabel(it.permission) },
            selectedIndex = role.ordinal,
            onSelect = { onRole(MemberRole.entries[it]) },
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(
                when (role) {
                    MemberRole.Member -> R.string.member_role_member_body
                    MemberRole.Admin -> R.string.member_role_admin_body
                },
            ),
            style = Lf.type.secondary,
            color = Lf.colors.textMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
internal fun memberNameProblemText(problem: NameProblem): String = stringResource(
    when (problem) {
        NameProblem.Duplicate -> R.string.member_name_taken
        // Spaces, characters and length all break the same rule for member names.
        NameProblem.Format, NameProblem.Spaces, NameProblem.Characters, NameProblem.TooLong -> R.string.member_name_format
    },
)
