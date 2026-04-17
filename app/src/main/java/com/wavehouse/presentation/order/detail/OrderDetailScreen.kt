package com.wavehouse.presentation.order.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.theme.Outline
import com.wavehouse.core.ui.theme.PrimaryGreen
import com.wavehouse.core.ui.theme.SurfaceContainerLow
import com.wavehouse.core.utils.toDateTimeString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.domain.model.OrderItem
import com.wavehouse.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết đơn hàng", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val order = uiState.order
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Không tìm thấy đơn hàng", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Header (Status, Date, ID)
                item {
                    val statusColor = if (order.status == OrderStatus.CANCELLED) Color.Gray else PrimaryGreen
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Receipt, null, tint = statusColor, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Đơn ${order.id.takeLast(6).uppercase()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                order.status.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Ngày bán: ${order.createdAt.toDateTimeString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Người bán: ${order.createdByName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Customer info placeholder / Payment Method
                item {
                    Text("Thông tin thanh toán", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hình thức", color = Outline)
                            Text(order.paymentMethod.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Item list
                item {
                    Text("Chi tiết mặt hàng (${order.itemCount})", fontWeight = FontWeight.Bold)
                }
                
                items(order.items, key = { it.productId }) { item ->
                    OrderItemCard(item)
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }

                // Total Summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng thanh toán", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(order.totalAmount.toVndString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemCard(item: OrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.quantity} x ${item.unitPrice.toVndString()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            // Optional: for admins you might want to show profit margin here 
            // e.g. Text("Giá vốn: ${item.costPrice.toVndString()} -> Lãi: ${item.lineProfit.toVndString()}")
        }
        Text(item.lineTotal.toVndString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
