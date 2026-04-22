package com.wavehouse.presentation.stock.lowstock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.wavehouse.core.ui.components.WaveAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.ui.theme.StockOut
import com.wavehouse.domain.model.StockItem
import com.wavehouse.domain.model.StockStatus

@Composable
fun LowStockAlertScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStockIn: () -> Unit,
    viewModel: LowStockAlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            WaveAppBar(title = "Cảnh báo tồn kho", onBack = onNavigateBack)
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tất cả sản phẩm đều đủ hàng!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> Column(modifier = Modifier.padding(padding)) {
                // Summary banner
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StockLow.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = StockLow)
                        Column {
                            Text(
                                "${uiState.items.size} sản phẩm cần nhập thêm",
                                fontWeight = FontWeight.SemiBold,
                                color = StockLow
                            )
                            Text(
                                "${uiState.items.count { it.stockStatus == StockStatus.OUT_OF_STOCK }} sản phẩm đã hết",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockOut
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items, key = { it.productId }) { item ->
                        LowStockItemCard(
                            item = item,
                            onStockIn = onNavigateToStockIn
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockItemCard(item: StockItem, onStockIn: () -> Unit) {
    val isOutOfStock = item.stockStatus == StockStatus.OUT_OF_STOCK
    val statusColor = if (isOutOfStock) StockOut else StockLow

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.productImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.productImageUrl,
                        contentDescription = item.productName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        item.productName.firstOrNull()?.uppercase() ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("SKU: ${item.productSku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(containerColor = statusColor.copy(alpha = 0.15f)) {
                        Text(
                            text = if (isOutOfStock) "Hết hàng" else "Sắp hết",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Còn: ${item.quantity} | Tối thiểu: ${item.minStock}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Quick stock-in button
            FilledTonalIconButton(onClick = onStockIn) {
                Icon(Icons.Filled.Add, contentDescription = "Nhập kho", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
