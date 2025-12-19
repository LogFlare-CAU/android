package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.example.logflare.core.designsystem.components.user.UserProfileCard
import com.example.logflare.core.designsystem.components.userlist.RoleBadgeType
import com.example.logflare.core.designsystem.components.userlist.UserItemSize
import com.example.logflare.core.designsystem.components.userlist.UserListItem
import com.example.logflare.core.designsystem.theme.AppTheme
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.UserPermission

@Composable
fun MyPageScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    onAddMember: () -> Unit = {},
    onEditMember: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel()
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colors.neutral.white
    ) {
        when {
            uiState.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> MyPageContent(
                uiState = uiState,
                onSelectLogLevel = viewModel::selectLogLevel,
                onBack = onBack,
                onLogout = onLogout,
                onAddMember = onAddMember,
                onEditMember = onEditMember
            )
        }
    }
}

@Composable
private fun MyPageContent(
    uiState: MyPageUiState,
    onSelectLogLevel: (LogLevel) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = AppTheme.colors.neutral.white
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "MYPAGE",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = AppTheme.spacing.s4),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    SectionHeader(title = "Account Info")
                    UserProfileCard(
                        username = uiState.username ?: "--",
                        roleLabel = uiState.permission.label,
                        roleType = uiState.permission.toRoleBadgeType(),
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.s4)
                    )
                    uiState.errorMessage?.let { message ->
                        ErrorBanner(
                            message = message,
                            modifier = Modifier
                                .padding(horizontal = AppTheme.spacing.s4)
                                .padding(top = AppTheme.spacing.s3)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppTheme.spacing.s4,
                                vertical = AppTheme.spacing.s8
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert Level",
                            style = AppTheme.typography.bodyMdBold,
                            color = AppTheme.colors.neutral.black
                        )

                        LogFlareDropdown(
                            items = uiState.logLevels,
                            selectedItem = uiState.selectedLogLevel,
                            onItemSelected = onSelectLogLevel,
                            itemLabelMapper = { it.label },
                            placeholder = "Log Level",
                            size = DropdownSize.Large,
                            modifier = Modifier.width(140.dp),
                            showCheckboxInMenu = false
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTheme.spacing.s4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Members",
                            style = AppTheme.typography.bodyMdBold,
                            color = AppTheme.colors.neutral.black
                        )

                        if (uiState.permission.code >= UserPermission.MODERATOR.code) {
                            LogFlareButton(
                                text = "Add Member",
                                onClick = onAddMember,
                                type = ButtonType.Text,
                                variant = ButtonVariant.Secondary,
                                size = ButtonSize.Small
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
                            .fillMaxWidth()
                    ) {
                        if (uiState.members.isEmpty()) {
                            Text(
                                text = "No members registered yet",
                                style = AppTheme.typography.bodySmLight,
                                color = AppTheme.colors.neutral.s60,
                                modifier = Modifier.padding(
                                    horizontal = AppTheme.spacing.s4,
                                    vertical = AppTheme.spacing.s6
                                )
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = AppTheme.spacing.s6,
                                        horizontal = AppTheme.spacing.s4
                                    ),
                                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3)
                            ) {
                                uiState.members.forEach { member ->
                                    UserListItem(
                                        username = member.username,
                                        roleLabel = member.role.label,
                                        roleType = member.role.toRoleBadgeType(),
                                        size = UserItemSize.Small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEditMember(member.username) },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = AppTheme.colors.secondary.default,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
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
                        contentAlignment = Alignment.Center
                    ) {
                        LogFlareButton(
                            text = "Log Out",
                            onClick = onLogout,
                            type = ButtonType.Text,
                            variant = ButtonVariant.Secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
                }
            }
        }
    }
}

// MyPageHeader replaced by BackHeader

@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppTheme.colors.red.default.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = AppTheme.colors.red.default,
            style = AppTheme.typography.bodySmMedium,
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.s4,
                vertical = AppTheme.spacing.s3
            )
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = AppTheme.typography.bodyMdBold,
        color = AppTheme.colors.neutral.black,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.s4,
                vertical = AppTheme.spacing.s8
            )
    )
}

private fun UserPermission.toRoleBadgeType(): RoleBadgeType = when (this) {
    UserPermission.SUPER_USER -> RoleBadgeType.SuperUser
    UserPermission.MODERATOR -> RoleBadgeType.Moderator
    UserPermission.USER -> RoleBadgeType.Member
}

