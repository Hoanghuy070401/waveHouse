package com.wavehouse.presentation.debt.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.utils.toVndString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DebtDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (String) -> Unit,
    viewModel: DebtDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBar(uiState.name, onNavigateBack) },
        containerColor = Color(0xFFF4FBF1)
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006D37))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── Hero Section: Summary + Due Date ─────────────────────────
                item {
                    HeroSection(
                        totalDebt = uiState.totalDebt,
                        orders = uiState.orders,
                        onNavigateToPayment = { onNavigateToPayment(uiState.phone) }
                    )
                }

                // ── Đơn hàng chưa thanh toán ─────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Đơn hàng chưa thanh toán",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006D37)
                        )
                        Text(
                            "${uiState.orders.size} đơn hàng",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6D7A6E)
                        )
                    }
                }

                items(uiState.orders, key = { it.id }) { order ->
                    UnpaidOrderCard(order)
                }

                // ── Lịch sử trả nợ ───────────────────────────────────────────
                item {
                    Text(
                        "Lịch sử trả nợ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF865300),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    DebtPaymentTimeline(uiState.orders)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(name: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color(0xFFF4FBF1).copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    tint = Color(0xFF006D37)
                )
            }
            Text(
                name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF006D37)
            )
        }
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
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
// HERO SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    totalDebt: Double,
    orders: List<com.wavehouse.domain.model.Order>,
    onNavigateToPayment: () -> Unit
) {
    val overdueThreshold = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000
    val earliestOverdueOrder = orders
        .filter { it.createdAt < overdueThreshold && it.debtAmount > 0 }
        .minByOrNull { it.createdAt }

    // Calculate nearest due date (createdAt + 15 days)
    val nearestDueMs = orders
        .filter { it.debtAmount > 0 }
        .minOfOrNull { it.createdAt + 15L * 24 * 60 * 60 * 1000 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Left card: total + buttons ────────────────────────────────────
        Surface(
            modifier = Modifier.weight(2f),
            color = Color(0xFFDDE5DB),
            shape = RoundedCornerShape(20.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Decorative blur circle (simulated with semi-transparent circle)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = 60.dp, y = (-30).dp)
                        .background(Color(0xFF006D37).copy(alpha = 0.08f), CircleShape)
                        .align(Alignment.TopEnd)
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "TỔNG DƯ NỢ HIỆN TẠI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3D4A3F).copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        totalDebt.toVndString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF006D37),
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Thu nợ button
                    Button(
                        onClick = onNavigateToPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D37)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Filled.Payments, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thu nợ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Cập nhật thanh toán button
                    Button(
                        onClick = onNavigateToPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD58700),
                            contentColor = Color(0xFF472A00)
                        )
                    ) {
                        Icon(Icons.Filled.Update, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cập nhật thanh toán", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Right card: due date ──────────────────────────────────────────
        Surface(
            modifier = Modifier.weight(1f),
            color = Color(0xFFFFDDB9),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF865300),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "HẠN NỢ GẦN NHẤT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF663E00).copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))

                if (nearestDueMs != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = nearestDueMs }
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val month = cal.get(Calendar.MONTH) + 1
                    val daysLeft = ((nearestDueMs - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()

                    Text(
                        "$day Thg $month",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B1700),
                        lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (daysLeft > 0) "Còn lại $daysLeft ngày" else "Đã quá hạn",
                        fontSize = 11.sp,
                        color = if (daysLeft > 0) Color(0xFF663E00).copy(alpha = 0.8f) else Color(0xFFBA1A1A)
                    )
                } else {
                    Text(
                        "Không có",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B1700)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UNPAID ORDER CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UnpaidOrderCard(order: com.wavehouse.domain.model.Order) {
    val isOverdue = order.createdAt < System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000
    val badgeBg = if (isOverdue) Color(0xFFFFDAD6) else Color(0xFFDDE5DB)
    val badgeColor = if (isOverdue) Color(0xFF93000A) else Color(0xFF3D4A3F)
    val badgeText = if (isOverdue) "Quá hạn" else "Đang nợ"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Surface(
        color = Color(0xFFEFF6EC),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ReceiptLong, null, tint = Color(0xFF006D37), modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "#${order.id.take(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF171D17)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${sdf.format(Date(order.createdAt))} • ${order.items.firstOrNull()?.productName ?: "Sản phẩm"}",
                        fontSize = 11.sp,
                        color = Color(0xFF3D4A3F),
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    order.debtAmount.toVndString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF006D37)
                )
                Spacer(Modifier.height(4.dp))
                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEBT PAYMENT TIMELINE
// ─────────────────────────────────────────────────────────────────────────────

private data class PaymentHistoryItem(
    val label: String,
    val description: String,
    val amount: String,
    val note: String,
    val isRecent: Boolean
)

@Composable
private fun DebtPaymentTimeline(orders: List<com.wavehouse.domain.model.Order>) {
    // Simulate payment history from partially-paid orders (paidAmount > 0)
    val paidOrders = orders.filter { it.paidAmount > 0 }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Build timeline items from orders that have been partially paid
    val timelineItems = if (paidOrders.isNotEmpty()) {
        paidOrders.mapIndexed { idx, order ->
            val isRecent = idx == 0
            val dateLabel = if (isRecent) {
                val diffDays = (System.currentTimeMillis() - order.createdAt) / (1000 * 60 * 60 * 24)
                if (diffDays == 0L) "Hôm nay"
                else if (diffDays == 1L) "Hôm qua"
                else sdf.format(Date(order.createdAt))
            } else sdf.format(Date(order.createdAt))

            PaymentHistoryItem(
                label = dateLabel,
                description = "Đã trả (${order.paymentMethod.label})",
                amount = "+${order.paidAmount.toVndString()}",
                note = "Đơn #${order.id.take(6).uppercase()}",
                isRecent = isRecent
            )
        }
    } else {
        // Fallback if no payment history yet
        listOf(
            PaymentHistoryItem(
                label = "Chưa có",
                description = "Chưa có lịch sử trả nợ",
                amount = "",
                note = "Sử dụng nút 'Thu nợ' để ghi nhận thanh toán",
                isRecent = false
            )
        )
    }

    val dotColor = Color(0xFF865300)
    val dotColorDim = Color(0xFFBCCABC)
    val lineColor = Color(0xFFBCCABC)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    ) {
        timelineItems.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Timeline line + dot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    // Dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (item.isRecent) dotColor else dotColorDim,
                                CircleShape
                            )
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF4FBF1), CircleShape)
                        )
                    }
                    // Dashed vertical line (if not last item)
                    if (index < timelineItems.size - 1) {
                        DashedVerticalLine(
                            color = lineColor,
                            modifier = Modifier
                                .width(2.dp)
                                .height(72.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Content card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index < timelineItems.size - 1) 0.dp else 0.dp)
                ) {
                    Text(
                        item.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isRecent) Color(0xFF865300) else Color(0xFF6D7A6E),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.description,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171D17)
                                )
                                if (item.amount.isNotEmpty()) {
                                    Text(
                                        item.amount,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006D37)
                                    )
                                }
                            }
                            if (item.note.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.note,
                                    fontSize = 10.sp,
                                    color = Color(0xFF3D4A3F)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DashedVerticalLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val dashLength = 8.dp.toPx()
        val gapLength = 4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
        )
    }
}
