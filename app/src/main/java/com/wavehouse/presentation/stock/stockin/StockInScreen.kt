package com.wavehouse.presentation.stock.stockin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.components.WaveAppBar
import com.wavehouse.core.utils.formatThousands
import com.wavehouse.core.utils.stripFormat
import com.wavehouse.domain.model.Product

// ─── Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    viewModel: StockInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Which product is currently open in the bottom sheet
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var stepperQty by remember { mutableStateOf(1.0) }
    var priceInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            selectedProduct = null
            stepperQty = 1.0; priceInput = ""; noteInput = ""
            snackbarHost.showSnackbar("✅ Nhập kho thành công!")
            viewModel.resetForm()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Reset stepper when product changes
    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null) stepperQty = 1.0
    }

    val bgColor = Color(0xFFF0F5F0)
    val totalStock = filteredProducts.sumOf { it.currentStock }

    Scaffold(
        topBar = {
            WaveAppBar(
                title = "Nhập kho",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = bgColor,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Search bar ──────────────────────────────────────────────────
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp))
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tìm sản phẩm", color = Color(0xFF9E9E9E), fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // ── Header ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("HÔM NAY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                        Text("Danh mục", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TỔNG TỒN KHO", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                        Text(
                            "${totalStock}",
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Loading indicator ───────────────────────────────────────────
            if (uiState.isLoadingProducts) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ── Product list ────────────────────────────────────────────────
            if (!uiState.isLoadingProducts && filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (uiState.searchQuery.isBlank()) "Chưa có sản phẩm nào trong kho"
                            else "Không tìm thấy \"${uiState.searchQuery}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            items(filteredProducts, key = { it.id }) { product ->
                ProductStockCard(
                    product = product,
                    onNhap = { selectedProduct = product }
                )
            }

            // ── Add new product link ────────────────────────────────────────
            item {
                Surface(
                    onClick = onNavigateToAddProduct,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.AddCircleOutline, "Thêm mới",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Thêm sản phẩm mới",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }


            // ── Quote block ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "\"Tối ưu hóa quản lý kho giúp giảm thiểu lãng phí thực phẩm và tăng hiệu quả vận hành kho hàng tươi sống.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(40.dp).clip(CircleShape),
                            thickness = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(80.dp)) // FAB clearance
            }
        }
    }

    // ── Bottom Sheet ────────────────────────────────────────────────────────
    selectedProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            StockInDetailSheet(
                product = product,
                stepperQty = stepperQty,
                onQtyChange = { stepperQty = it },
                priceInput = priceInput,
                onPriceChange = { priceInput = it },
                noteInput = noteInput,
                onNoteChange = { noteInput = it },
                isLoading = uiState.isLoading,
                onHuy = { selectedProduct = null },
                onXacNhan = {
                    viewModel.submitDirect(
                        product = product,
                        quantity = stepperQty,
                        // FIX: toLongOrNull() trả null cả khi input = "0" → dùng let + check > 0
                        unitCostPrice = priceInput.toLongOrNull()?.takeIf { it > 0 }?.toDouble(),
                        note = noteInput.ifBlank { null }
                    )
                }
            )
        }
    }
}

// ─── Product card ───────────────────────────────────────────────────────────

@Composable
private fun ProductStockCard(product: Product, onNhap: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle with first letter or image
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        product.name.firstOrNull()?.uppercase() ?: "",
                        fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Tồn kho: ${product.currentStock} ${product.unitName ?: ""}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNhap,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Nhập", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

/** Format Double qty: hide .0 for whole numbers, show 1 decimal for 0.1 increments */
private fun formatQty(qty: Double): String =
    if (qty % 1.0 == 0.0) qty.toLong().toString()
    else "%.1f".format(qty).replace('.', ',')

/** Round to 1 decimal place to fix floating-point drift (1.1000000001 → 1.1) */
private fun roundQty(qty: Double): Double = Math.round(qty * 10) / 10.0

// ─── Bottom Sheet Detail ────────────────────────────────────────────────────

@Composable
private fun StockInDetailSheet(
    product: Product,
    stepperQty: Double,
    onQtyChange: (Double) -> Unit,
    priceInput: String,           // raw digits e.g. "50000"
    onPriceChange: (String) -> Unit,
    noteInput: String,
    onNoteChange: (String) -> Unit,
    isLoading: Boolean,
    onHuy: () -> Unit,
    onXacNhan: () -> Unit
) {
    val totalPrice = (priceInput.toLongOrNull() ?: 0L) * stepperQty
    val bgField = Color(0xFFF5F8F5)
    val unit = product.unitName ?: "cái"

    // Local qty text — sync from stepper, editable directly
    var qtyText by remember(stepperQty) { mutableStateOf(formatQty(stepperQty)) }

    // FIX: Dùng TextFieldValue thay vì String để kiểm soát cursor position.
    // Khi format lại ("1222" → "1.222"), text thay đổi độ dài → cursor bị reset về 0.
    // TextRange(formatted.length) ghim cursor vào CUỐI chuỗi sau mỗi lần reformat.
    var priceFieldValue by remember {
        val initial = formatThousands(priceInput)
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Title row ──────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Nhập hàng: ${product.name}", fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp, lineHeight = 26.sp)
                Text("Tồn hiện tại: ${formatQty(product.currentStock)} $unit",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(product.name.firstOrNull()?.uppercase() ?: "", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Quantity Stepper ────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Số lượng nhập", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Minus button: step -0.1, min 0.1
                // FIX: Dùng Box thay vì Surface để tránh clip IconButton (48dp > Surface 42dp → icon bị clipped thành ≡)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(bgField),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val newQty = roundQty((stepperQty - 0.1).coerceAtLeast(0.1))
                            onQtyChange(newQty)
                            qtyText = formatQty(newQty)
                        },
                        enabled = stepperQty > 0.1,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Filled.Remove, "Giảm",
                            tint = if (stepperQty > 0.1) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Editable qty field — allows decimals
                Surface(shape = RoundedCornerShape(12.dp), color = bgField, modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextField(
                            value = qtyText,
                            onValueChange = { v ->
                                // Chấp nhận digit + 1 dấu thập phân (dấu phẩy hoặc chấm)
                                // FIX: normalize sang dấu phẩy cho hiển thị tiếng Việt
                                val normalized = v.replace('.', ',')
                                val clean = normalized.filter { it.isDigit() || it == ',' }
                                    .let { s ->
                                        val comma = s.indexOf(',')
                                        if (comma == -1) s
                                        else s.substring(0, comma + 1) + s.substring(comma + 1).filter { it.isDigit() }
                                    }
                                qtyText = clean
                                // Parse: convert phẩy → chấm cho toDoubleOrNull
                                val parsed = clean.replace(',', '.').toDoubleOrNull() ?: 0.0
                                if (parsed > 0.0) onQtyChange(parsed)
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        Text(unit, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Plus button: step +0.1
                // FIX: dùng Box để đồng nhất với nút Minus, tránh clip
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val newQty = roundQty(stepperQty + 0.1)
                            onQtyChange(newQty)
                            qtyText = formatQty(newQty)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Tăng", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // ── Price field — formatted with thousand-dot separator ───────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Giá nhập (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(12.dp), color = bgField, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = priceFieldValue,
                        onValueChange = { newValue ->
                            // Strip dots, reformat, ghim cursor vào cuối
                            val raw = stripFormat(newValue.text)
                            val formatted = formatThousands(raw)
                            priceFieldValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length) // cursor luôn ở cuối
                            )
                            onPriceChange(raw)
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0", color = Color(0xFF9E9E9E)) },
                        singleLine = true
                    )
                    Text("đ/$unit", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Note field ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ghi chú (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(12.dp), color = bgField, modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = noteInput,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Hàng sáng nay", color = Color(0xFF9E9E9E)) },
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        // ── Total estimate — always visible, formatted ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tổng tiền dự kiến", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (totalPrice > 0) formatThousands(totalPrice.toLong().toString()) + " đ" else "--",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (totalPrice > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── Action buttons ──────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onHuy,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text("Hủy", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = onXacNhan,
                modifier = Modifier.weight(2f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xác nhận", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
