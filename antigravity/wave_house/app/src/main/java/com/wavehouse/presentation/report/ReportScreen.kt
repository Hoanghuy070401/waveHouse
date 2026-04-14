package com.wavehouse.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI state mapping to filter index
    val selectedFilter = when(uiState.days) {
        0 -> 0
        7 -> 1
        30 -> 2
        else -> 0
    }
    val filters = listOf("Hôm nay", "Tuần này", "Tháng này")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { /* TODO: Custom date range */ }) {
                        Icon(Icons.Filled.DateRange, "Chọn thời gian")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Filter
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                filters.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedFilter == index,
                        onClick = { viewModel.onTimeFilterChanged(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Summary Cards
            val vndFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Doanh thu",
                    value = "${vndFormat.format(uiState.stats.totalRevenue)}đ",
                    trend = "+${String.format("%.1f", uiState.stats.shrinkageChangePercent)}%",
                    isPositive = uiState.stats.shrinkageChangePercent >= 0,
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Lợi nhuận",
                    value = "${vndFormat.format(uiState.stats.profit)}đ",
                    trend = "",
                    isPositive = uiState.stats.profit >= 0,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Tỉ lệ hao hụt",
                    value = "${String.format("%.1f", uiState.stats.shrinkageRate)}%",
                    trend = "",
                    isPositive = uiState.stats.shrinkageRate < 5.0,
                    color = ChartIn
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Ngày giao dịch",
                    value = uiState.stats.revenueByDay.size.toString(),
                    trend = "",
                    isPositive = true,
                    color = ChartOut
                )
            }

            // Placeholder for Chart
            Card(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.TrendingUp, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Biểu đồ đang được cập nhật", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Top Products list
            Text("Sản phẩm xuất nhiều nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            if (uiState.stats.topSellingProducts.isEmpty()) {
                Text(
                    "Chưa có dữ liệu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        uiState.stats.topSellingProducts.take(5).forEachIndexed { index, product ->
                            TopProductRow(
                                name = "${index + 1}. ${product.productName}",
                                quantity = "${product.quantitySold.toInt()} ${product.unitName}"
                            )
                            if (index < uiState.stats.topSellingProducts.size - 1) {
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    trend: String,
    isPositive: Boolean,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Spacer(Modifier.width(4.dp))
                Text(trend, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

@Composable
private fun TopProductRow(name: String, quantity: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(quantity, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}
