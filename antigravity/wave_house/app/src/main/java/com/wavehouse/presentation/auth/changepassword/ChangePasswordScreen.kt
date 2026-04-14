package com.wavehouse.presentation.auth.changepassword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Show success snackbar + navigate back after short delay
    LaunchedEffect(uiState.changeSuccess) {
        if (uiState.changeSuccess) {
            snackbarHostState.showSnackbar("✅ Đổi mật khẩu thành công!")
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đổi mật khẩu", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Nhập mật khẩu hiện tại và mật khẩu mới để cập nhật.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // ── Mật khẩu hiện tại ────────────────────────────────────────
            OutlinedTextField(
                value = uiState.currentPassword,
                onValueChange = viewModel::onCurrentPasswordChange,
                label = { Text("Mật khẩu hiện tại") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleCurrentPasswordVisibility) {
                        Icon(
                            if (uiState.isCurrentPasswordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (uiState.isCurrentPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.currentPasswordError != null,
                supportingText = uiState.currentPasswordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Mật khẩu mới ─────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text("Mật khẩu mới") },
                leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleNewPasswordVisibility) {
                        Icon(
                            if (uiState.isNewPasswordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (uiState.isNewPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.newPasswordError != null,
                supportingText = uiState.newPasswordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Xác nhận mật khẩu mới ────────────────────────────────────
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Xác nhận mật khẩu mới") },
                leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // ── Password strength hint ────────────────────────────────────
            AnimatedVisibility(visible = uiState.newPassword.isNotEmpty()) {
                val strength = when {
                    uiState.newPassword.length >= 12 -> "Mạnh" to MaterialTheme.colorScheme.primary
                    uiState.newPassword.length >= 8  -> "Trung bình" to MaterialTheme.colorScheme.tertiary
                    else                              -> "Yếu" to MaterialTheme.colorScheme.error
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = strength.second.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null,
                            tint = strength.second, modifier = Modifier.size(16.dp))
                        Text(
                            "Độ mạnh mật khẩu: ${strength.first}",
                            style = MaterialTheme.typography.bodySmall,
                            color = strength.second,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Submit button ─────────────────────────────────────────────
            Button(
                onClick = viewModel::changePassword,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Cập nhật mật khẩu", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
