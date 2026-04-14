package com.wavehouse.presentation.stock.stockout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.theme.StockOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: StockOutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHost.showSnackbar("✅ Xuất kho thành công!")
            viewModel.resetForm()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xuất kho", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Product
            OutlinedTextField(
                value = uiState.productName,
                onValueChange = viewModel::onProductNameChange,
                label = { Text("Sản phẩm *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.productError != null,
                supportingText = uiState.productError?.let { { Text(it) } },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Filled.QrCodeScanner, "Quét barcode")
                    }
                }
            )

            // Current stock — prominent warning if low
            if (uiState.currentStock >= 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.currentStock == 0)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tồn kho hiện tại", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${uiState.currentStock} ${uiState.unit}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.currentStock == 0) StockOut
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quantity
            OutlinedTextField(
                value = uiState.quantity,
                onValueChange = viewModel::onQuantityChange,
                label = { Text("Số lượng xuất *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.quantityError != null,
                supportingText = uiState.quantityError?.let { { Text(it) } },
                singleLine = true
            )

            // Note
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                } else {
                    Text("Xác nhận xuất kho", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
