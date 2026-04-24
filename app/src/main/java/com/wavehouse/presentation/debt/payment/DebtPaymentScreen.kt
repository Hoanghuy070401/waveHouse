package com.wavehouse.presentation.debt.payment

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.utils.toVndString
import com.wavehouse.domain.model.PaymentMethod

// Max credit limit assumed for progress bar display
private const val CREDIT_LIMIT = 20_000_000.0

@Composable
fun DebtPaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: DebtPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var paymentAmountStr by remember { mutableStateOf("") }
    val paymentAmount = paymentAmountStr.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

    val remainingDebt = (uiState.totalDebt - paymentAmount).coerceAtLeast(0.0)
    val creditUsageRatio = (uiState.totalDebt / CREDIT_LIMIT).coerceIn(0.0, 1.0).toFloat()
    val creditUsagePercent = (creditUsageRatio * 100).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = creditUsageRatio,
        animationSpec = tween(800),
        label = "credit_progress"
    )

    LaunchedEffect(uiState.isPaymentSuccess) {
        if (uiState.isPaymentSuccess) onPaymentSuccess()
    }

    // ── Success State ─────────────────────────────────────────────────────────
    if (uiState.isPaymentSuccess) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4FBF1)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF006D37).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = Color(0xFF006D37),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Thu nợ thành công!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006D37)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    paymentAmount.toVndString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF171D17)
                )
            }
        }
        return
    }

    // ── Main Screen ───────────────────────────────────────────────────────────
    Scaffold(
        topBar = { TopBar(onNavigateBack) },
        containerColor = Color(0xFFF4FBF1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "THANH TOÁN ĐỊNH KỲ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D7A6E),
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Thu hồi nợ",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF171D17),
                    lineHeight = 36.sp
                )
                Text(
                    if (uiState.name.isNotBlank() && uiState.name != "Khách hàng vãng lai")
                        uiState.name
                    else
                        "Khách #${uiState.phone.takeLast(4)}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF006D37),
                    lineHeight = 36.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Tổng nợ cũ + Progress ─────────────────────────────────────────
            Surface(
                color = Color(0xFFEFF6EC),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Tổng nợ cũ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF526478)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            uiState.totalDebt.toVndString().dropLast(1), // remove trailing đ
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171D17)
                        )
                        Text(
                            "đ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF6D7A6E),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            // ── Credit Limit Progress ─────────────────────────────────────────
            Surface(
                color = Color(0xFF006D37).copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Trạng thái hạn mức",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006D37)
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF006D37),
                            trackColor = Color(0xFFDDE5DB),
                            strokeCap = StrokeCap.Round
                        )
                    }
                    Text(
                        "$creditUsagePercent% Hạn mức",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D7A6E)
                    )
                }
            }

            // ── Input Form Card ───────────────────────────────────────────────
            Surface(
                color = Color(0xFFDDE5DB),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Amount input
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Số tiền khách trả nợ hôm nay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF171D17),
                            lineHeight = 24.sp
                        )
                        OutlinedTextField(
                            value = paymentAmountStr,
                            onValueChange = { paymentAmountStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = {
                                Text(
                                    "0",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF006D37),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                Text(
                                    "VNĐ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D7A6E),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF171D17)
                            )
                        )
                    }

                    // Payment method
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "PHƯƠNG THỨC THANH TOÁN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D7A6E),
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PaymentMethodButton(
                                icon = Icons.Filled.Payments,
                                text = "Tiền mặt",
                                isSelected = selectedMethod == PaymentMethod.CASH,
                                onClick = { selectedMethod = PaymentMethod.CASH },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodButton(
                                icon = Icons.Filled.AccountBalance,
                                text = "Chuyển khoản",
                                isSelected = selectedMethod == PaymentMethod.QR,
                                onClick = { selectedMethod = PaymentMethod.QR },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Remaining Debt ────────────────────────────────────────────────
            Surface(
                color = Color(0xFFFFDDB9),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFF472A00).copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ReceiptLong,
                                null,
                                tint = Color(0xFF2B1700),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Còn nợ mới",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF472A00)
                            )
                            Text(
                                "Tự động cập nhật",
                                fontSize = 10.sp,
                                color = Color(0xFF865300).copy(alpha = 0.8f)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            remainingDebt.toVndString().dropLast(1),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2B1700)
                        )
                        Text(
                            "đ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B1700)
                        )
                    }
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            Surface(
                color = Color(0xFFEFF6EC),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        "GHI CHÚ GIAO DỊCH (Tùy chọn)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D7A6E),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        placeholder = {
                            Text(
                                "Nhập lý do hoặc mã tham chiếu...",
                                color = Color(0xFF6D7A6E).copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = Color(0xFF171D17)
                        )
                    )
                }
            }

            if (uiState.error != null) {
                Surface(
                    color = Color(0xFFFFDAD6),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        uiState.error!!,
                        color = Color(0xFFBA1A1A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Confirm Button ────────────────────────────────────────────────
            Button(
                onClick = { viewModel.submitPayment(paymentAmount, selectedMethod, notes) },
                enabled = !uiState.isPaying && paymentAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (!uiState.isPaying && paymentAmount > 0)
                                Brush.linearGradient(listOf(Color(0xFF006D37), Color(0xFF27AE60)))
                            else
                                Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isPaying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Xác nhận thu nợ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color(0xFFF4FBF1).copy(alpha = 0.92f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDDE5DB), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color(0xFF006D37),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "Quản lý công nợ",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF006D37)
            )
        }
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF27AE60)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAYMENT METHOD BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentMethodButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFF006D37).copy(alpha = 0.06f) else Color(0xFFEFF6EC)
    val borderColor = if (isSelected) Color(0xFF006D37) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF006D37) else Color(0xFF6D7A6E)
    val iconTint = if (isSelected) Color(0xFF006D37) else Color(0xFF6D7A6E)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
