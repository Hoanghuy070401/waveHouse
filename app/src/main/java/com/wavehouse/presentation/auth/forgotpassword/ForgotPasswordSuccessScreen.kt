package com.wavehouse.presentation.auth.forgotpassword

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ── Palette
private val BgGradientTop = Color(0xFF0F1A15)
private val BgGradientBot = Color(0xFF050806)
private val AccentGreen = Color(0xFF4CAF6E)
private val TextWhite = Color(0xFFEEF4EE)
private val TextMuted = Color(0xFF7A9B82)
private val CardBg = Color(0xFF1D3324)

/** Mask email for security: huy@gmail.com → h***@gmail.com */
fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    val local = email.substring(0, at)
    val domain = email.substring(at)
    val visible = local.take(1)
    return "$visible***$domain"
}

@Composable
fun ForgotPasswordSuccessScreen(
    email: String,
    onNavigateToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Start countdown only if not already running (safe across rotation/recompose)
    LaunchedEffect(Unit) {
        viewModel.startCountdownIfIdle()
    }

    // Show Toast when resend fails
    LaunchedEffect(uiState.resendErrorMessage) {
        uiState.resendErrorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearResendError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgGradientTop, BgGradientBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextWhite
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "FreshStock",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(56.dp))

            // ── Animated Email + Check icon ───────────────────────
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBg)
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Mail,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Orange checkmark badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE67E22)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Kiểm tra email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                buildAnnotatedString {
                    append("Chúng tôi đã gửi link đặt lại mật khẩu đến:\n")
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append(maskEmail(email))
                    }
                    append("\n\nVui lòng kiểm tra hộp thư (và spam).")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(36.dp))

            // ── Open Gmail button ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF16A34A)))
                    )
                    .clickable {
                        // Launch Gmail or default email app
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_EMAIL)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "Không tìm thấy ứng dụng email trên thiết bị.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Mở Gmail",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Resend section ────────────────────────────────────
            Text(
                "Chưa nhận được email?",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.resendCountdown > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Mail,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Gửi lại sau ${uiState.resendCountdown}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                Text(
                    "Gửi lại",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGreen,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                        viewModel.resendEmail(email)
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Back to Login ─────────────────────────────────────
            Row(
                modifier = Modifier.padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Quay lại đăng nhập",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
