package com.wavehouse.presentation.stock.shrinkage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.ShrinkageReason

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShrinkageScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShrinkageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Success dialog
    if (uiState.saveSuccess) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearSuccess()
                onNavigateBack()
            },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Đã ghi nhận hao hụt") },
            text = { Text("Tồn kho đã được cập nhật thành công.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSuccess()
                    onNavigateBack()
                }) { Text("Đóng") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghi nhận hao hụt", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
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
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Step 1: Chọn sản phẩm ──────────────────────
            item {
                Text("1. Chọn sản phẩm", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            if (uiState.selectedProduct == null) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm nông sản...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(uiState.products.take(10)) { product ->
                    ProductSelectRow(
                        product = product,
                        onClick = { viewModel.selectProduct(product) }
                    )
                }
            } else {
                item {
                    SelectedProductCard(
                        product = uiState.selectedProduct!!,
                        onClear = { viewModel.selectProduct(uiState.selectedProduct!!) }
                    )
                }
            }

            // ── Step 2: Số lượng & lý do ────────────────────
            if (uiState.selectedProduct != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("2. Số lượng hao hụt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.quantity,
                        onValueChange = viewModel::onQuantityChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Số lượng") },
                        suffix = { Text(uiState.selectedProduct?.unitName ?: "sp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text("Tồn kho hiện tại: ${uiState.selectedProduct?.currentStock ?: 0}")
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("3. Lý do", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShrinkageReason.entries.forEach { reason ->
                            FilterChip(
                                selected = uiState.selectedReason == reason,
                                onClick = { viewModel.onReasonChange(reason) },
                                label = { Text(reason.label) },
                                leadingIcon = if (uiState.selectedReason == reason) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("4. Ghi chú (không bắt buộc)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Thêm mô tả chi tiết...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Error
                uiState.error?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Submit button
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving && uiState.quantity.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Delete, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ghi nhận hao hụt", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductSelectRow(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium)
                Text(
                    "SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${product.currentStock} ${product.unitName ?: "sp"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SelectedProductCard(
    product: Product,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tồn kho: ${product.currentStock} ${product.unitName ?: "sp"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, "Đổi sản phẩm")
            }
        }
    }
}
