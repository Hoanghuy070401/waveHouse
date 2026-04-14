package com.wavehouse.presentation.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// Dark theme colors matching Stitch design
private val DarkBg = Color(0xFF0D1F14)
private val DarkSurface = Color(0xFF1A2E1E)
private val DarkBorder = Color(0xFF2E4A34)
private val TextWhite = Color(0xFFEEF4EE)
private val TextMuted = Color(0xFF7A9B82)
private val AccentGreen = Color(0xFF4CAF6E)

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToEmailVerification: (String) -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onNavigateToDashboard()
    }

    LaunchedEffect(uiState.requiresEmailVerification) {
        if (uiState.requiresEmailVerification) {
            onNavigateToEmailVerification(uiState.verificationEmail)
            viewModel.clearVerificationFlag()
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
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── FreshStock brand header ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.EnergySavingsLeaf, // Plant-like icon
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "FreshStock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Title block ──────────────────────────────────────
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chào mừng trở lại",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 42.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Manage your digital orchard with precision.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Glass Form Container ─────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2622).copy(alpha = 0.85f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

            // ── Email field ──────────────────────────────────────
            DarkFieldLabel("Email address")
            DarkTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "name@freshstock.com",
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

            Spacer(Modifier.height(16.dp))

            // ── Password field with Forgot ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Password",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Quên mật khẩu?",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentGreen,
                    modifier = Modifier.clickable { onNavigateToForgotPassword() }
                )
            }
            Spacer(Modifier.height(8.dp))
            DarkTextField(
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.login()
                }),
                enabled = !uiState.isLoading
            )

            Spacer(Modifier.height(28.dp))

            // ── Login button ─────────────────────────────────────
            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading,
                shape = CircleShape, // Pill shape
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF269D4F)) // Brighter green
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        "Đăng nhập →",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Divider HOẶC ─────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text("  HOẶC  ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Google button ─────────────────────────────────────
            OutlinedButton(
                onClick = { /* TODO: Google sign-in */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                )
            ) {
                Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                Spacer(Modifier.width(12.dp))
                Text("Sign in with Google", fontWeight = FontWeight.SemiBold, color = TextWhite)
            }

            Spacer(Modifier.height(36.dp))

            // ── Register link ─────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản?  ", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(
                    "Đăng ký",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Footer ────────────────────────────────────────────
            Text(
                "© 2024 FreshStock Logistics. By signing in, you agree to\nour Terms of Harvest and Environmental Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))
        }

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
    }
}

// ── Shared dark-theme field components ───────────────────────────────────────

@Composable
private fun DarkFieldLabel(label: String) {
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
private fun DarkTextField(
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
