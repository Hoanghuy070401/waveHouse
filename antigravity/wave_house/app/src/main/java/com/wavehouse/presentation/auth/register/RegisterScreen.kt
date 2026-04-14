package com.wavehouse.presentation.auth.register

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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Dark theme palette (same as LoginScreen)
private val DarkBg = Color(0xFF0D1F14)
private val DarkSurface = Color(0xFF1A2E1E)
private val DarkBorder = Color(0xFF2E4A34)
private val TextWhite = Color(0xFFEEF4EE)
private val TextMuted = Color(0xFF7A9B82)
private val AccentGreen = Color(0xFF4CAF6E)

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.navigateToVerification) {
        if (uiState.navigateToVerification) {
            onNavigateToEmailVerification(uiState.registeredEmail)
            viewModel.clearNavigationFlag()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1A15), Color(0xFF050806))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            // ── Top bar ───────────────────────────────────────────
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
                        tint = TextWhite
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFlorist,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "FreshStock",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── Title ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thiết lập Tài khoản FreshStock",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Điền thông tin để bắt đầu quản lý nông sản của bạn một cách có tổ chức.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Glass Form Container ──────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1D3324)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    // ── Thông tin cá nhân Divider ──────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thông tin cá nhân".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    }

                    // ── Họ và tên ──────────────────────────────────
                    DarkLabel("Họ và tên")
                    DarkField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        placeholder = "Nguyễn Văn A",
                        leadingIcon = Icons.Filled.Person,
                        isError = uiState.nameError != null,
                        errorText = uiState.nameError,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !uiState.isLoading
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Email ──────────────────────────────────────
                    DarkLabel("Email")
                    DarkField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "example@freshstock.com",
                        leadingIcon = Icons.Filled.Email,
                        isError = uiState.emailError != null,
                        errorText = uiState.emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !uiState.isLoading
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Bảo mật Divider ────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bảo mật".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    }

                    // ── Mật khẩu ──────────────────────────────────
                    DarkLabel("Mật khẩu")
                    DarkField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "••••••••",
                        leadingIcon = Icons.Filled.Lock,
                        isError = uiState.passwordError != null,
                        errorText = uiState.passwordError,
                        isPassword = true,
                        isPasswordVisible = uiState.isPasswordVisible,
                        onTogglePassword = viewModel::togglePasswordVisibility,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !uiState.isLoading
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Xác nhận mật khẩu ─────────────────────────
                    DarkLabel("Xác nhận mật khẩu")
                    DarkField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        placeholder = "••••••••",
                        leadingIcon = Icons.Filled.Shield,
                        isError = uiState.confirmPasswordError != null,
                        errorText = uiState.confirmPasswordError,
                        isPassword = true,
                        isPasswordVisible = uiState.isConfirmPasswordVisible,
                        onTogglePassword = viewModel::toggleConfirmPasswordVisibility,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        enabled = !uiState.isLoading
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Terms checkbox ─────────────────────────────
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = uiState.isAgreeTerms,
                            onCheckedChange = viewModel::onAgreeTermsChange,
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentGreen,
                                uncheckedColor = DarkBorder,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Tôi đồng ý với ")
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) {
                                    append("Điều khoản dịch vụ")
                                }
                                append(" và ")
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) {
                                    append("Chính sách bảo mật")
                                }
                                append(" của FreshStock.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Register button ────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF16A34A))
                                )
                            )
                            .clickable(enabled = !uiState.isLoading, onClick = viewModel::register),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Hoàn tất đăng ký",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Divider HOẶC ───────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = "Hoặc".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                            color = Color.White.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF1D3324))
                                .padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Google button ──────────────────────────────
                    OutlinedButton(
                        onClick = { /* TODO: Google sign-in */ },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Tiếp tục với Google", fontWeight = FontWeight.Medium, color = Color.White)
                    }
                } // end Card Column
            } // end Card

            Spacer(Modifier.height(24.dp))

            // ── Login link ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bạn đã là thành viên? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    "Đăng nhập ngay.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGreen,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        } // end outer Column

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF3E1F1F),
                contentColor = Color(0xFFFFCDD2)
            )
        }
    } // end Box
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun DarkLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = TextWhite,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false,
    errorText: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            isError = isError,
            singleLine = true,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                disabledTextColor = TextMuted,
                errorTextColor = TextWhite,
                focusedContainerColor = Color(0xFF1E2822),
                unfocusedContainerColor = Color(0xFF1E2822),
                disabledContainerColor = Color(0xFF1E2822),
                cursorColor = AccentGreen,
                focusedBorderColor = AccentGreen,
                unfocusedBorderColor = Color(0xFF385241),
                errorBorderColor = Color(0xFFFFB4AB),
                errorCursorColor = Color(0xFFFFB4AB),
                focusedLeadingIconColor = AccentGreen,
                unfocusedLeadingIconColor = AccentGreen,
                focusedTrailingIconColor = AccentGreen,
                unfocusedTrailingIconColor = AccentGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorText != null) {
            Text(
                errorText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFB4AB),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
