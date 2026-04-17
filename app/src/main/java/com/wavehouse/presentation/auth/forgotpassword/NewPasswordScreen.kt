package com.wavehouse.presentation.auth.forgotpassword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ── Palette (shared with ForgotPasswordScreen)
private val NpBgTop = Color(0xFF0F1A15)
private val NpBgBot = Color(0xFF050806)
private val NpCardBg = Color(0xFF1D3324)
private val NpAccent = Color(0xFF4CAF6E)
private val NpTextWhite = Color(0xFFEEF4EE)
private val NpTextMuted = Color(0xFF7A9B82)
private val NpBorder = Color(0xFF2E4A34)
private val NpError = Color(0xFFFFB4AB)

@Composable
fun NewPasswordScreen(
    oobCode: String,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NewPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    // Navigate to login on success
    LaunchedEffect(uiState.navigateToLogin) {
        if (uiState.navigateToLogin) {
            onNavigateToLogin()
            viewModel.clearNavigationFlag()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NpBgTop, NpBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = NpTextWhite
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "FreshStock",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NpTextWhite
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(40.dp))

            // ── Lock icon + heading
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(NpCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = NpAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Đặt mật khẩu mới",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NpTextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Tạo mật khẩu mới cho tài khoản của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NpTextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NpCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // ── New password field
                    Text(
                        "Mật khẩu mới",
                        style = MaterialTheme.typography.labelLarge,
                        color = NpTextWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        placeholder = { Text("••••••••", color = NpTextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showPassword) "Ẩn" else "Hiện",
                                    tint = NpTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = uiState.newPasswordError != null,
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = passwordFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.newPasswordError != null) {
                        Text(
                            uiState.newPasswordError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = NpError,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Confirm password field
                    Text(
                        "Xác nhận mật khẩu",
                        style = MaterialTheme.typography.labelLarge,
                        color = NpTextWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        placeholder = { Text("••••••••", color = NpTextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    if (showConfirm) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showConfirm) "Ẩn" else "Hiện",
                                    tint = NpTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (showConfirm) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = uiState.confirmPasswordError != null,
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.confirmPasswordReset(oobCode)
                        }),
                        shape = RoundedCornerShape(12.dp),
                        colors = passwordFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.confirmPasswordError != null) {
                        Text(
                            uiState.confirmPasswordError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = NpError,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // ── Validation hint
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = NpTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "≥ 6 ký tự, 2 ô trường phải giống nhau",
                            style = MaterialTheme.typography.labelSmall,
                            color = NpTextMuted
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Confirm button
                    val isEnabled = uiState.newPassword.isNotBlank() &&
                            uiState.confirmPassword.isNotBlank() &&
                            uiState.newPasswordError == null &&
                            uiState.confirmPasswordError == null &&
                            !uiState.isLoading

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                brush = if (isEnabled)
                                    Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF16A34A)))
                                else
                                    Brush.horizontalGradient(listOf(NpCardBg, NpCardBg))
                            )
                            .clickable(enabled = isEnabled) {
                                focusManager.clearFocus()
                                viewModel.confirmPasswordReset(oobCode)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Xác nhận",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color.White else NpTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Back to login link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("< ", color = NpTextMuted, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Quay lại đăng nhập",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NpAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF3E1F1F),
                contentColor = NpError
            )
        }
    }
}

@Composable
private fun passwordFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NpTextWhite,
    unfocusedTextColor = NpTextWhite,
    disabledTextColor = NpTextMuted,
    errorTextColor = NpTextWhite,
    focusedContainerColor = Color(0xFF1E2822),
    unfocusedContainerColor = Color(0xFF1E2822),
    cursorColor = NpAccent,
    focusedBorderColor = NpAccent,
    unfocusedBorderColor = NpBorder,
    errorBorderColor = NpError,
    errorCursorColor = NpError,
    focusedTrailingIconColor = NpAccent,
    unfocusedTrailingIconColor = NpTextMuted
)
