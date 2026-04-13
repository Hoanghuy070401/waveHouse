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
fun ReportScreen() {
    // Placeholder states for report filter
    var selectedFilter by remember { mutableIntStateOf(0) }
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
                        onClick = { selectedFilter = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Nhập",
                    value = "1,240",
                    trend = "+12%",
                    isPositive = true,
                    color = ChartIn
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Xuất",
                    value = "850",
                    trend = "-5%",
                    isPositive = false,
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

            // Top Products list placeholder
            Text("Sản phẩm xuất nhiều nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    TopProductRow("1. Thùng Carton 60x40", "320 cái")
                    HorizontalDivider()
                    TopProductRow("2. Băng keo trong", "180 cuộn")
                    HorizontalDivider()
                    TopProductRow("3. Màng PE bọc hàng", "95 cuộn")
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
