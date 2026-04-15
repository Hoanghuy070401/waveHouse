package com.wavehouse.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.wavehouse.core.ui.navigation.Routes
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.utils.toRelativeTimeString
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockEntryType
import com.wavehouse.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = uiState.userRole == UserRole.ADMIN
    val isAdminOrWarehouse = isAdmin || uiState.userRole == UserRole.WAREHOUSE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.warehouseName.isNotBlank())
                                uiState.warehouseName else "Xin chào, ${uiState.userName.ifBlank { "..." }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (uiState.userRole) {
                                UserRole.ADMIN -> "Chủ kho • Toàn quyền"
                                UserRole.WAREHOUSE -> "Thủ kho"
                                UserRole.ACCOUNTANT -> "Kế toán"
                                UserRole.STAFF -> "Nhân viên"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.Account.route) }) {
                        Icon(Icons.Filled.Person, contentDescription = "Tài khoản")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── KPI Cards ─────────────────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Filled.Insights,
                        title = "Tổng Quan Nhanh"
                    )
                    Spacer(Modifier.height(10.dp))
                    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KpiCard(
                            title = "Doanh thu",
                            value = "${fmt.format(uiState.stats.todayRevenue)}đ",
                            subtitle = "hôm nay",
                            icon = Icons.Filled.ArrowUpward,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Tồn kho",
                            value = "${uiState.stats.totalProducts} sp",
                            subtitle = "tổng sản phẩm",
                            icon = Icons.Filled.Inventory2,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KpiCard(
                            title = "Hao hụt",
                            value = "-${uiState.stats.todayStockOut}kg",
                            subtitle = "hôm nay",
                            icon = Icons.Filled.ArrowDownward,
                            iconColor = StockLow,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Đơn hàng",
                            value = "${uiState.stats.todayOrders} đơn",
                            subtitle = "hôm nay",
                            icon = Icons.Filled.ShoppingCart,
                            iconColor = ChartIn,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Quản Lý Kho (Admin + Warehouse) ──────────────────────
                if (isAdminOrWarehouse) {
                    item {
                        SectionHeader(
                            icon = Icons.Filled.Warehouse,
                            title = "Quản Lý Kho"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Thêm SP — Admin only
                            if (isAdmin) {
                                ActionTile(
                                    icon = Icons.Filled.AddBox,
                                    label = "Thêm SP",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Routes.AddProduct.route) }
                                )
                            }
                            ActionTile(
                                icon = Icons.Filled.MoveToInbox,
                                label = "Nhập hàng",
                                tint = ChartIn,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(Routes.StockIn.route) }
                            )
                            ActionTile(
                                icon = Icons.Filled.Output,
                                label = "Xuất hàng",
                                tint = ChartOut,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(Routes.StockOut.route) }
                            )
                            ActionTile(
                                icon = Icons.Filled.FactCheck,
                                label = "Kiểm kê",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(Routes.StockOverview.route) }
                            )
                        }
                    }
                }

                // ── Kinh Doanh ────────────────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Filled.Analytics,
                        title = "Kinh Doanh"
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionTile(
                            icon = Icons.Filled.ReceiptLong,
                            label = "Đơn hàng",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.Pos.route) }
                        )
                        ActionTile(
                            icon = Icons.Filled.History,
                            label = "Lịch sử",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.StockHistory.route) }
                        )
                        if (isAdmin) {
                            ActionTile(
                                icon = Icons.Filled.BarChart,
                                label = "Báo cáo",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(Routes.Report.route) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // ── Nhân sự (Admin only) ──────────────────────────────────
                if (isAdmin) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                icon = Icons.Filled.Badge,
                                title = "Nhân sự"
                            )
                            TextButton(
                                onClick = { navController.navigate(Routes.ManageStaff.route) }
                            ) {
                                Text(
                                    "Xét duyệt",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── Giao dịch gần đây ────────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Filled.SwapVert,
                        title = "Giao dịch gần đây"
                    )
                }

                if (uiState.recentEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có giao dịch nào hôm nay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(uiState.recentEntries) { entry ->
                        StockEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

// ── Components ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StockEntryRow(entry: StockEntry) {
    val isIncoming = entry.type.isIncoming
    val color = if (isIncoming) ChartIn else ChartOut
    val sign = if (isIncoming) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isIncoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = entry.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${entry.type.label} • ${entry.createdAt.toRelativeTimeString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "$sign${entry.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
