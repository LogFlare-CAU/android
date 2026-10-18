package com.logflare.android.feature.mypage

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfIcons
import com.example.logflare.core.designsystem.v2.components.Avatar
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.LargeTitleHeader
import com.example.logflare.core.designsystem.v2.components.LfPullToRefresh
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.RoleChip
import com.example.logflare.core.designsystem.v2.components.SettingRow
import com.example.logflare.core.designsystem.v2.components.SkeletonRow
import com.example.logflare.core.designsystem.v2.components.SwitchRow
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.store.model.Member
import com.logflare.android.push.GlobalAlert
import com.logflare.android.ui.VisualQaTags

/** The My tab. [onSignedOut] leaves for Login once the sign-out finished. */
@Composable
fun MyRoute(
    onAddMember: () -> Unit,
    onEditMember: (idx: Int) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val clipboard = LocalClipboardManager.current
    val copied = stringResource(R.string.my_server_copied)
    val quietOn = stringResource(R.string.my_quiet_on)
    val quietOff = stringResource(R.string.my_quiet_off)
    val refreshFailed = stringResource(R.string.my_refresh_failed)
    val logoutFailed = stringResource(R.string.my_logout_failed)
    val alertLabels = GlobalAlert.entries.associateWith { globalAlertLabel(it) }
    val pushChanged = stringResource(R.string.my_push_changed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MyEvent.CopyServer -> {
                    clipboard.setText(AnnotatedString(event.url))
                    toast.show(copied)
                }
                is MyEvent.PushAlertsChanged -> toast.show(pushChanged.format(alertLabels.getValue(event.value)))
                is MyEvent.QuietHoursChanged -> toast.show(if (event.on) quietOn else quietOff)
                MyEvent.RefreshFailed -> toast.show(refreshFailed)
                MyEvent.LogoutFailed -> toast.show(logoutFailed)
                MyEvent.SignedOut -> onSignedOut()
            }
        }
    }
    MyContent(
        state = state,
        actions = MyActions(
            onCopyServer = viewModel::copyServer,
            onOpenSheet = viewModel::openSheet,
            onDismissSheet = viewModel::dismissSheet,
            onSelectTheme = viewModel::setTheme,
            onSelectGlobalAlert = viewModel::setGlobalAlert,
            onQuietHours = viewModel::setQuietHours,
            onToggleShowAll = viewModel::toggleShowAll,
            onRetryMembers = viewModel::retryMembers,
            onAddMember = onAddMember,
            onEditMember = onEditMember,
            onConfirmLogout = viewModel::confirmLogout,
            onRefresh = viewModel::refresh,
        ),
        toast = toast,
    )
}

/** Everything the My tab and its sheets can do. */
class MyActions(
    val onCopyServer: () -> Unit = {},
    val onOpenSheet: (MySheet) -> Unit = {},
    val onDismissSheet: () -> Unit = {},
    val onSelectTheme: (ThemePreference) -> Unit = {},
    val onSelectGlobalAlert: (GlobalAlert) -> Unit = {},
    val onQuietHours: (Boolean) -> Unit = {},
    val onToggleShowAll: () -> Unit = {},
    val onRetryMembers: () -> Unit = {},
    val onAddMember: () -> Unit = {},
    val onEditMember: (idx: Int) -> Unit = {},
    val onConfirmLogout: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/** [scrollState] is a parameter so a test can show the bottom of the page. */
@Composable
fun MyContent(
    state: MyUiState,
    actions: MyActions,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.MyPage)) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleHeader(stringResource(R.string.my_title))
            LfPullToRefresh(
                refreshing = state.refreshing,
                onRefresh = actions.onRefresh,
                modifier = Modifier.weight(1f),
            ) {
                // The whole page scrolls (the prototype's first draft didn't).
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileCard(state, actions.onCopyServer)
                    SettingsCard(state, actions)
                    if (state.canManageMembers || state.members.isNotEmpty()) MembersCard(state, actions)
                    LogoutCard(actions)
                    Text(
                        stringResource(R.string.my_version, state.version),
                        style = Lf.type.caption,
                        color = Lf.colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }
    MySheets(state, actions)
}

@Composable
private fun ProfileCard(state: MyUiState, onCopyServer: () -> Unit) {
    val me = state.me
    GroupedCard(contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(initial = me?.username.orEmpty().ifEmpty { "?" }, size = 52.dp, strong = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    me?.username.orEmpty(),
                    style = Lf.type.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
                    color = Lf.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.my_profile_subtitle, roleLabel(me?.permission ?: 0), state.serverHost),
                    style = Lf.type.secondary,
                    color = Lf.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val description = stringResource(R.string.my_copy_server_description)
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lf.colors.fill)
                    .clickable(role = Role.Button, onClick = onCopyServer)
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.my_copy_server),
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(state: MyUiState, actions: MyActions) {
    GroupedCard {
        SettingRow(
            stringResource(R.string.my_appearance),
            themeLabel(state.theme),
            { actions.onOpenSheet(MySheet.Appearance) },
        )
        SettingRow(
            stringResource(R.string.my_push_alerts),
            globalAlertLabel(state.globalAlert),
            { actions.onOpenSheet(MySheet.PushAlerts) },
        )
        SwitchRow(
            label = stringResource(R.string.my_quiet_hours),
            description = stringResource(R.string.my_quiet_hours_body),
            checked = state.quietHours,
            onCheckedChange = actions.onQuietHours,
        )
    }
}

@Composable
private fun MembersCard(state: MyUiState, actions: MyActions) {
    GroupedCard {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.my_members))
                    if (!state.membersLoading && !state.membersFailed) {
                        append(" ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Lf.colors.textMuted)) {
                            append(state.members.size.toString())
                        }
                    }
                },
                style = Lf.type.sectionTitle,
                color = Lf.colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (state.canManageMembers) AddMemberAction(actions.onAddMember)
        }
        when {
            state.membersLoading -> repeat(MEMBERS_PREVIEW) { SkeletonRow() }
            state.membersFailed -> ListRow(
                title = stringResource(R.string.my_members_failed),
                trailing = {
                    Text(
                        stringResource(R.string.my_members_retry),
                        style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                        color = Lf.colors.textSecondary,
                    )
                },
                onClick = actions.onRetryMembers,
            )
            else -> state.visibleMembers.forEach { member ->
                MemberRow(member, if (state.canManageMembers) ({ actions.onEditMember(member.idx) }) else null)
            }
        }
        if (state.hasMoreMembers && !state.membersLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                    .heightIn(min = 48.dp)
                    .clip(Lf.shapes.input)
                    .clickable(role = Role.Button, onClick = actions.onToggleShowAll),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.showAllMembers) {
                        stringResource(R.string.my_members_show_less)
                    } else {
                        stringResource(R.string.my_members_show_all, state.members.size)
                    },
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AddMemberAction(onAdd: () -> Unit) {
    val description = stringResource(R.string.my_members_add_description)
    Row(
        Modifier
            .testTag(VisualQaTags.AddMember)
            .heightIn(min = 44.dp)
            .clip(Lf.shapes.input)
            .clickable(role = Role.Button, onClick = onAdd)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(LfIcons.Add, contentDescription = null, tint = Lf.colors.textPrimary, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.my_members_add),
            style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
            color = Lf.colors.textPrimary,
        )
    }
}

/** [onClick] null: a plain row without chevron (the viewer can't edit members). */
@Composable
private fun MemberRow(member: Member, onClick: (() -> Unit)?) {
    ListRow(
        title = member.username,
        leading = { Avatar(member.username) },
        trailing = { RoleChip(roleLabel(member.permission)) },
        showChevron = onClick != null,
        onClick = onClick,
        modifier = Modifier.testTag(VisualQaTags.memberRow(member.username)),
    )
}

@Composable
private fun LogoutCard(actions: MyActions) {
    GroupedCard {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button) { actions.onOpenSheet(MySheet.Logout) }
                .testTag(VisualQaTags.LogoutAction)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(stringResource(R.string.my_logout), style = Lf.type.bodyStrong, color = Lf.colors.textMuted)
        }
    }
}

/** Super Admin (>= 100), Admin (>= 80) or Member. */
@Composable
internal fun roleLabel(permission: Int): String = stringResource(
    when {
        permission >= 100 -> R.string.my_role_super_admin
        permission >= 80 -> R.string.my_role_admin
        else -> R.string.my_role_member
    },
)
