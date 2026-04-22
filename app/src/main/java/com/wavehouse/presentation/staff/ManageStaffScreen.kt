package com.wavehouse.presentation.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.wavehouse.core.ui.components.WaveAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole

@Composable
fun ManageStaffScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WaveAppBar(title = "Quản lý nhân viên", onBack = onNavigateBack)
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Mã kết nối cửa hàng ──────────────────────────────────
            if (uiState.joinCode.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MÃ KẾT NỐI CỬA HÀNG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                uiState.joinCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 6.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Chia sẻ mã này với nhân viên để họ đăng ký",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(uiState.joinCode))
                            }
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Sao chép mã",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Tabs: Đang làm / Chờ duyệt ───────────────────────────
            val pendingCount = uiState.pendingUsers.size
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Đang làm việc (${uiState.users.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chờ duyệt")
                            if (pendingCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$pendingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else when (selectedTabIndex) {
                0 -> {
                    // Nhân viên đang hoạt động
                    if (uiState.users.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Chưa có nhân viên nào\nHãy chia sẻ mã kết nối để nhân viên đăng ký",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.users, key = { it.id }) { user ->
                                StaffItem(
                                    user = user,
                                    onRoleChange = { newRole -> viewModel.updateRole(user.id, newRole) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                1 -> {
                    // Nhân viên chờ duyệt
                    if (uiState.pendingUsers.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Không có nhân viên nào đang chờ duyệt",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.pendingUsers, key = { it.id }) { user ->
                                PendingStaffItem(
                                    user = user,
                                    onApprove = { viewModel.approveStaff(user.id) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingStaffItem(user: User, onApprove: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(user.name.ifBlank { "Chưa đặt tên" }, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Column {
                Text(user.email)
                Text(
                    "Chờ Admin duyệt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        leadingContent = {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            FilledTonalButton(onClick = onApprove) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Duyệt")
            }
        }
    )
}

@Composable
private fun StaffItem(user: User, onRoleChange: (UserRole) -> Unit) {
    var showRoleDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(user.name.ifBlank { "Chưa đặt tên" }, fontWeight = FontWeight.Medium)
        },
        supportingContent = { Text(user.email) },
        leadingContent = {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            RoleBadge(role = user.role, onClick = { showRoleDialog = true })
        }
    )

    if (showRoleDialog) {
        RolePickerDialog(
            currentRole = user.role,
            userName = user.name,
            onDismiss = { showRoleDialog = false },
            onConfirm = { newRole ->
                onRoleChange(newRole)
                showRoleDialog = false
            }
        )
    }
}

@Composable
private fun RoleBadge(role: UserRole, onClick: () -> Unit) {
    val (label, containerColor) = when (role) {
        UserRole.ADMIN -> "Admin" to MaterialTheme.colorScheme.errorContainer
        UserRole.WAREHOUSE -> "Thủ kho" to MaterialTheme.colorScheme.primaryContainer
        UserRole.ACCOUNTANT -> "Kế toán" to MaterialTheme.colorScheme.tertiaryContainer
        UserRole.STAFF -> "Nhân viên" to MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.onErrorContainer
        UserRole.WAREHOUSE -> MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.ACCOUNTANT -> MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.STAFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RolePickerDialog(
    currentRole: UserRole,
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Phân quyền: ${userName.ifBlank { "Nhân viên" }}") },
        text = {
            Column {
                Text(
                    "Chọn vai trò mới:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                UserRole.entries.filter { it != UserRole.ADMIN }.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (role) {
                                UserRole.ADMIN -> "Admin — Toàn quyền"
                                UserRole.WAREHOUSE -> "Thủ kho — Nhập/xuất kho"
                                UserRole.ACCOUNTANT -> "Kế toán — Xem báo cáo"
                                UserRole.STAFF -> "Nhân viên — Xem & bán hàng"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedRole) }) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        }
    )
}
