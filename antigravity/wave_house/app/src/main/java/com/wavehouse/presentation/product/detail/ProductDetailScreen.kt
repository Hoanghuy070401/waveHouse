package com.wavehouse.presentation.product.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.wavehouse.core.ui.theme.StockGood
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.ui.theme.StockOut
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.StockStatus
import com.wavehouse.presentation.product.list.stockStatusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết sản phẩm", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    uiState.product?.let { product ->
                        IconButton(onClick = { onNavigateToEdit(product.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Chỉnh sửa")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.product != null -> ProductDetailContent(
                product = uiState.product!!,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ProductDetailContent(product: Product, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Product Image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Name & Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.categoryName ?: "Chưa phân loại",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Stock quantity badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${product.currentStock}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = stockStatusColor(product.stockStatus)
                    )
                    Text(
                        text = product.unitName ?: "cái",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Info rows
            InfoCard {
                InfoRow(label = "Mã SKU", value = product.sku)
                if (!product.barcode.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(
                        label = "Barcode",
                        value = product.barcode,
                        icon = { Icon(Icons.Filled.QrCode, null, Modifier.size(16.dp)) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "Đơn vị tính", value = product.unitName ?: "—")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "Tồn kho tối thiểu", value = "${product.minStock}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = "Trạng thái",
                    value = when (product.stockStatus) {
                        StockStatus.IN_STOCK -> "Còn hàng"
                        StockStatus.LOW_STOCK -> "Sắp hết hàng"
                        StockStatus.OUT_OF_STOCK -> "Hết hàng"
                    },
                    valueColor = stockStatusColor(product.stockStatus)
                )
            }

            if (!product.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                InfoCard {
                    Text(
                        text = "Mô tả",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            icon?.invoke()
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
}
