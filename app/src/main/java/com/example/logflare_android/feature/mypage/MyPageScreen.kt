package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.ui.components.BackHeader
import com.example.logflare.core.designsystem.AppTheme

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
    onSelectLogLevel: (String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (String) -> Unit
) {
    var showLogLevelDropdown by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.neutral.white)
            .navigationBarsPadding()
    ) {
        BackHeader(title = "MYPAGE", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SectionHeader(title = "Account Info")
                UserCard(
                    username = uiState.username ?: "--",
                    role = uiState.permission,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                uiState.errorMessage?.let { message ->
                    ErrorBanner(
                        message = message,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alert Level",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.neutral.black
                    )

                    LogLevelDropdown(
                        selectedLevel = uiState.selectedLogLevel,
                        levels = uiState.logLevels,
                        expanded = showLogLevelDropdown,
                        onExpandedChange = { showLogLevelDropdown = it },
                        onLevelSelected = {
                            onSelectLogLevel(it)
                            showLogLevelDropdown = false
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.neutral.black
                    )

                    TextButton(
                        onClick = onAddMember,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTheme.colors.secondary.default
                        )
                    ) {
                        Text(
                            text = "Add Member",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                MembersCard(
                    members = uiState.members,
                    onMemberClick = { member -> onEditMember(member.username) },
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTheme.colors.secondary.default
                        )
                    ) {
                        Text(
                            text = "Log Out",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    color = AppTheme.colors.neutral.black,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp)
    )
}

@Composable
private fun UserCard(
    username: String,
    role: UserPermission,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.neutral.s20)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.neutral.black
                )
                Text(
                    text = role.label,
                    fontSize = 12.sp,
                    color = AppTheme.colors.neutral.s60,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            RoleChip(role = role, size = RoleChipSize.Large)
        }
    }
}

@Composable
private fun RoleChip(
    role: UserPermission,
    size: RoleChipSize = RoleChipSize.Small
) {
    val backgroundColor = when (role) {
    UserPermission.SUPER_USER -> AppTheme.colors.primary.pressed
    UserPermission.MODERATOR -> AppTheme.colors.primary.default
    UserPermission.USER -> AppTheme.colors.neutral.s70
    }

    val (height, fontSize, horizontalPadding) = when (size) {
        RoleChipSize.Large -> Triple(28.dp, 14.sp, 12.dp)
        RoleChipSize.Small -> Triple(20.dp, 10.sp, 8.dp)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier.height(height)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = role.label,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.neutral.s5
            )
        }
    }
}

@Composable
private fun LogLevelDropdown(
    selectedLevel: String,
    levels: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLevelSelected: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AppTheme.colors.neutral.s40),
            color = Color.Transparent,
            modifier = Modifier
                .width(133.5.dp)
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLevel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.neutral.s70
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.neutral.s70
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(133.5.dp)
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level, fontSize = 14.sp) },
                    onClick = { onLevelSelected(level) }
                )
            }
        }
    }
}

@Composable
private fun MembersCard(
    members: List<MyPageMemberUiModel>,
    onMemberClick: (MyPageMemberUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.neutral.s10)
    ) {
        if (members.isEmpty()) {
            Text(
                text = "No members registered yet",
                fontSize = 13.sp,
                color = AppTheme.colors.neutral.s60,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                members.forEach { member ->
                    MemberListItem(
                        member = member,
                        onClick = { onMemberClick(member) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberListItem(
    member: MyPageMemberUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = member.username,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.neutral.black
            )
            RoleChip(role = member.role, size = RoleChipSize.Small)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View member",
            tint = AppTheme.colors.secondary.default,
            modifier = Modifier.size(20.dp)
        )
    }
}

private enum class RoleChipSize {
    Large, Small
}
