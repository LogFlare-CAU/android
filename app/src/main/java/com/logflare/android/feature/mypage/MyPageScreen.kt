package com.logflare.android.feature.mypage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.logflare.core.designsystem.components.button.ButtonSize
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.dropdown.DropdownSize
import com.example.logflare.core.designsystem.components.dropdown.LogFlareDropdown
import com.example.logflare.core.designsystem.components.user.UserProfileCard
import com.example.logflare.core.designsystem.components.userlist.RoleBadgeType
import com.example.logflare.core.designsystem.components.userlist.UserItemSize
import com.example.logflare.core.designsystem.components.userlist.UserListItem
import com.example.logflare.core.designsystem.theme.AppTheme
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.ui.VisualQaTags

@Composable
fun MyPageScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    onAddMember: () -> Unit = {},
    onEditMember: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.ui.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MyPageContent(
        uiState = uiState,
        onLogout = onLogout,
        onAddMember = onAddMember,
        onEditMember = onEditMember,
        onSelectLogLevel = viewModel::selectLogLevel,
        modifier = modifier,
    )
}

@Composable
fun MyPageContent(
    uiState: MyPageUiState,
    onLogout: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (String) -> Unit,
    onSelectLogLevel: (LogLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.MyPage),
        color = AppTheme.colors.surface,
    ) {
        when {
            uiState.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(VisualQaTags.Loading),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    color = AppTheme.colors.surface,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = AppTheme.spacing.s4),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            item {
                                SectionHeader(title = "Account Info")
                                UserProfileCard(
                                    username = uiState.username ?: "--",
                                    roleLabel = uiState.permission.label,
                                    roleType = uiState.permission.toRoleBadgeType(),
                                    modifier = Modifier.padding(horizontal = AppTheme.spacing.s4),
                                )
                                uiState.errorMessage?.let { message ->
                                    ErrorBanner(
                                        message = message,
                                        modifier = Modifier
                                            .padding(horizontal = AppTheme.spacing.s4)
                                            .padding(top = AppTheme.spacing.s3)
                                            .testTag(VisualQaTags.Error),
                                    )
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = AppTheme.spacing.s4,
                                            vertical = AppTheme.spacing.s8,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Alert Level",
                                        style = AppTheme.typography.bodyMdBold,
                                        color = AppTheme.colors.onSurface,
                                    )

                                    LogFlareDropdown(
                                        items = uiState.logLevels,
                                        selectedItem = uiState.selectedLogLevel,
                                        onItemSelected = onSelectLogLevel,
                                        itemLabelMapper = { it.label },
                                        placeholder = "Log Level",
                                        size = DropdownSize.Large,
                                        modifier = Modifier.width(140.dp),
                                        showCheckboxInMenu = false,
                                    )
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppTheme.spacing.s4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Members",
                                        style = AppTheme.typography.bodyMdBold,
                                        color = AppTheme.colors.onSurface,
                                    )

                                    if (uiState.permission.code >= UserPermission.MODERATOR.code) {
                                        LogFlareButton(
                                            text = "Add Member",
                                            onClick = onAddMember,
                                            type = ButtonType.Text,
                                            variant = ButtonVariant.Secondary,
                                            size = ButtonSize.Small,
                                            modifier = Modifier.testTag(VisualQaTags.AddMember),
                                        )
                                    }
                                }
                            }

                            item {
                                Surface(
                                    color = AppTheme.colors.neutral.s10,
                                    shape = AppTheme.radius.large,
                                    modifier = Modifier
                                        .padding(horizontal = AppTheme.spacing.s4)
                                        .fillMaxWidth(),
                                ) {
                                    if (uiState.members.isEmpty()) {
                                        Text(
                                            text = "No members registered yet",
                                            style = AppTheme.typography.bodySmLight,
                                            color = AppTheme.colors.neutral.s60,
                                            modifier = Modifier
                                                .padding(
                                                    horizontal = AppTheme.spacing.s4,
                                                    vertical = AppTheme.spacing.s6,
                                                )
                                                .testTag(VisualQaTags.Empty),
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    vertical = AppTheme.spacing.s6,
                                                    horizontal = AppTheme.spacing.s4,
                                                ),
                                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3),
                                        ) {
                                            uiState.members.forEach { member ->
                                                UserListItem(
                                                    username = member.username,
                                                    roleLabel = member.role.label,
                                                    roleType = member.role.toRoleBadgeType(),
                                                    size = UserItemSize.Small,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag(VisualQaTags.memberRow(member.username))
                                                        .clickable { onEditMember(member.username) },
                                                    trailingContent = {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                            contentDescription = null,
                                                            tint = AppTheme.colors.secondary.default,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(AppTheme.spacing.s8))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppTheme.spacing.s6),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LogFlareButton(
                                        text = "Log Out",
                                        onClick = onLogout,
                                        type = ButtonType.Text,
                                        variant = ButtonVariant.Secondary,
                                        modifier = Modifier.testTag(VisualQaTags.LogoutAction),
                                    )
                                }
                                Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppTheme.colors.red.default.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = message,
            color = AppTheme.colors.red.default,
            style = AppTheme.typography.bodySmMedium,
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.s4,
                vertical = AppTheme.spacing.s3,
            ),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = AppTheme.typography.bodyMdBold,
        color = AppTheme.colors.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.s4,
                vertical = AppTheme.spacing.s8,
            ),
    )
}

private fun UserPermission.toRoleBadgeType(): RoleBadgeType = when (this) {
    UserPermission.SUPER_USER -> RoleBadgeType.SuperUser
    UserPermission.MODERATOR -> RoleBadgeType.Moderator
    UserPermission.USER -> RoleBadgeType.Member
}
