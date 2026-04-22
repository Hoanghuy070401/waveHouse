package com.wavehouse.presentation.stock.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.wavehouse.core.ui.components.WaveAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wavehouse.core.ui.navigation.Routes
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut
import com.wavehouse.core.ui.theme.StockLow

@Composable
fun StockOverviewScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            WaveAppBar(
                title = "Kho hàng",
                onBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.StockIn.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nhập kho")
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StockActionCard(
                    title = "Nhập Kho",
                    description = "Ghi nhận hàng nhập từ nhà cung cấp",
                    icon = Icons.Filled.ArrowDownward,
                    iconColor = ChartIn,
                    onClick = { navController.navigate(Routes.StockIn.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Xuất Kho",
                    description = "Ghi nhận hàng xuất, bán hoặc chuyển kho",
                    icon = Icons.Filled.ArrowUpward,
                    iconColor = ChartOut,
                    onClick = { navController.navigate(Routes.StockOut.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Ghi Hao Hụt",
                    description = "Ghi chép hàng hỏng, hết hạn, hủy",
                    icon = Icons.Filled.Delete,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { navController.navigate(Routes.Shrinkage.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Lịch Sử Giao Dịch",
                    description = "Xem toàn bộ lịch sử nhập xuất kho",
                    icon = Icons.Filled.History,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate(Routes.StockHistory.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Lịch Sử Đơn Hàng",
                    description = "Xem lịch sử thanh toán / bán hàng POS",
                    icon = Icons.Filled.Receipt,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = { navController.navigate(Routes.OrderHistory.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Cảnh Báo Tồn Kho",
                    description = "Danh sách sản phẩm cần nhập thêm",
                    icon = Icons.Filled.Warning,
                    iconColor = StockLow,
                    onClick = { navController.navigate(Routes.LowStockAlert.route) }
                )
            }
            item {
                StockActionCard(
                    title = "Nhà Cung Cấp",
                    description = "Quản lý đối tác và thông tin liên hệ",
                    icon = Icons.Filled.Store,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { navController.navigate(Routes.SupplierList.route) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
