package com.wavehouse.presentation.order.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.components.WaveAppBar
import com.wavehouse.core.ui.theme.PrimaryGreen
import com.wavehouse.core.utils.toDateTimeString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.model.OrderStatus

@Composable
fun OrderHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            WaveAppBar(
                title = "Lịch sử bán hàng",
                onBack = onNavigateBack
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Simple preset filters
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Hôm nay", "Tất cả")
                var selectedFilter by remember { mutableIntStateOf(0) }
                
                filters.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedFilter == index,
                        onClick = { 
                            selectedFilter = index
                            if (index == 0) {
                                viewModel.setDateRange(todayStartMillis(), todayEndMillis())
                            } else {
                                // "All time" = roughly 2 years back for MVP
                                viewModel.setDateRange(0L, System.currentTimeMillis())
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có đơn hàng nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
                        OrderHistoryCard(
                            order = order,
                            onClick = { onNavigateToDetail(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .run { this },
                contentAlignment = Alignment.Center
            ) {
                val iconColor = if (order.status == OrderStatus.CANCELLED) Color.Gray else PrimaryGreen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = iconColor.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {}
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đơn ${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${order.itemCount} mặt hàng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = order.createdAt.toDateTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status and Total
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = order.totalAmount.toVndString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (order.status == OrderStatus.CANCELLED) Color.Gray else PrimaryGreen
                )
                if (order.debtAmount > 0) {
                    Text(
                         text = "Còn nợ: ${order.debtAmount.toVndString()}",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error,
                         fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = order.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (order.status == OrderStatus.PAID) PrimaryGreen else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
