package com.wavehouse.presentation.product.addedit

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Navigate back on success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
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
                title = {
                    Text(
                        if (uiState.isEditMode) "Chỉnh sửa sản phẩm" else "Thêm sản phẩm",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Lưu", fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Tên sản phẩm *") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            // SKU
            OutlinedTextField(
                value = uiState.sku,
                onValueChange = viewModel::onSkuChange,
                label = { Text("Mã SKU *") },
                isError = uiState.skuError != null,
                supportingText = uiState.skuError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true
            )

            // Barcode
            OutlinedTextField(
                value = uiState.barcode,
                onValueChange = viewModel::onBarcodeChange,
                label = { Text("Barcode / QR") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Filled.QrCodeScanner, "Quét barcode")
                    }
                }
            )

            // Price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.costPrice,
                    onValueChange = viewModel::onCostPriceChange,
                    label = { Text("Giá nhập") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("đ") }
                )
                OutlinedTextField(
                    value = uiState.salePrice,
                    onValueChange = viewModel::onSalePriceChange,
                    label = { Text("Giá bán") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("đ") }
                )
            }

            // Min Stock
            OutlinedTextField(
                value = uiState.minStock,
                onValueChange = viewModel::onMinStockChange,
                label = { Text("Tồn kho tối thiểu") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Category (simple text for now — can be a dropdown)
            OutlinedTextField(
                value = uiState.selectedCategoryName,
                onValueChange = { viewModel.onCategorySelected("", it) },
                label = { Text("Danh mục") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Unit
            OutlinedTextField(
                value = uiState.selectedUnitName,
                onValueChange = { viewModel.onUnitSelected("", it) },
                label = { Text("Đơn vị tính (cái, thùng, kg...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(16.dp))

            // Save button (bottom)
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading
            ) {
                Text(
                    if (uiState.isEditMode) "Cập nhật sản phẩm" else "Thêm sản phẩm",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
