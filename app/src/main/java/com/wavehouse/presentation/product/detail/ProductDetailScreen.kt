package com.wavehouse.presentation.product.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.wavehouse.core.utils.formatThousands
import com.wavehouse.core.utils.stripFormat
import com.wavehouse.domain.model.PriceRecord
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.StockStatus
import com.wavehouse.presentation.product.list.stockStatusColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val vndFmt: NumberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))
private val dateFmt = SimpleDateFormat("dd MMM", Locale("vi", "VN"))
private val timeFmt = SimpleDateFormat("HH:mm", Locale("vi", "VN"))

// Organic Ledger Design Tokens
private val SurfaceContainerLow = Color(0xFFEFF6EC)
private val SurfaceContainerHigh = Color(0xFFE3EAE0)
private val SurfaceContainerHighest = Color(0xFFDDE5DB)
private val PrimaryColor = Color(0xFF006D37)
private val PrimaryContainer = Color(0xFF27AE60)
private val TertiaryFixed = Color(0xFFFFDDB9)
private val TertiaryColor = Color(0xFF865300)
private val OnSurface = Color(0xFF171D17)
private val OnSurfaceVariant = Color(0xFF3D4A3F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearSuccess()
        }
    }

    if (uiState.showUpdatePriceDialog) {
        uiState.product?.let { product ->
            UpdatePriceDialog(
                currentCostPrice = product.costPrice,
                currentSalePrice = product.salePrice,
                isLoading = uiState.isUpdatingPrice,
                onDismiss = { viewModel.hideUpdatePriceDialog() },
                onConfirm = { cost, sale, reason ->
                    viewModel.updatePrice(costPrice = cost, salePrice = sale, reason = reason.takeIf { it.isNotBlank() })
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết sản phẩm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    uiState.product?.let { product ->
                        IconButton(onClick = { onNavigateToEdit(product.id) }) {
                            Icon(Icons.Filled.Edit, "Chỉnh sửa")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF4FBF1))
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.product != null -> ProductDetailContent(
                product = uiState.product!!,
                priceHistory = uiState.priceHistory,
                isHistoryLoading = uiState.isHistoryLoading,
                onUpdatePriceClick = { viewModel.showUpdatePriceDialog() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    priceHistory: List<PriceRecord>,
    isHistoryLoading: Boolean,
    onUpdatePriceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4FBF1)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero section
        item {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Product identity
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = "PRODUCT",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${product.currentStock} ${product.unitName ?: "cái"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = product.categoryName ?: "Chưa phân loại",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Giá hôm nay section
        item {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Giá hôm nay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Giá nhập
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHighest)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "NHẬP",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${vndFmt.format(product.costPrice)}đ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                    }
                    // Giá bán
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TertiaryFixed)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BÁN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TertiaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${vndFmt.format(product.salePrice)}đ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryColor
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // CTA Button with gradient
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.linearGradient(listOf(PrimaryColor, PrimaryContainer))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onUpdatePriceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxSize(),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("✏️  Cập nhật giá", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Other product details
        item {
            Column(
                Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp)
            ) {
                InfoRow("Mã SKU", product.sku)
                if (!product.barcode.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Barcode", product.barcode, icon = { Icon(Icons.Filled.QrCode, null, Modifier.size(16.dp)) })
                }
                Spacer(Modifier.height(12.dp))
                InfoRow("Đơn vị tính", product.unitName ?: "—")
                Spacer(Modifier.height(12.dp))
                InfoRow("Tồn kho tối thiểu", "${product.minStock} ${product.unitName ?: ""}")
                Spacer(Modifier.height(12.dp))
                InfoRow(
                    "Trạng thái",
                    when (product.stockStatus) {
                        StockStatus.IN_STOCK -> "Còn hàng"
                        StockStatus.LOW_STOCK -> "Sắp hết hàng"
                        StockStatus.OUT_OF_STOCK -> "Hết hàng"
                    },
                    valueColor = stockStatusColor(product.stockStatus)
                )
            }
        }

        // Lịch sử giá header
        item {
            Row(
                Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Lịch sử giá",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }
                Text(
                    "${priceHistory.size} bản ghi",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryColor
                )
            }
        }

        // Price history list
        if (isHistoryLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryColor)
                }
            }
        } else if (priceHistory.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có lịch sử giá", color = OnSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(priceHistory) { index, record ->
                PriceHistoryItem(
                    record = record,
                    isEvenRow = index % 2 == 0,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PriceHistoryItem(
    record: PriceRecord,
    isEvenRow: Boolean,
    modifier: Modifier = Modifier
) {
    val date = Date(record.timestamp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isEvenRow) SurfaceContainerLow else Color(0x80FFFFFF))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date badge
        Box(
            Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    SimpleDateFormat("MMM", Locale("vi", "VN")).format(date).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant
                )
                Text(
                    SimpleDateFormat("dd", Locale.getDefault()).format(date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Cập nhật lúc ${timeFmt.format(date)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            record.reason?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "NHẬP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    vndFmt.format(record.costPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "BÁN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TertiaryColor,
                    letterSpacing = 1.sp
                )
                Text(
                    vndFmt.format(record.salePrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TertiaryColor
                )
            }
        }
    }
}

@Composable
fun UpdatePriceDialog(
    currentCostPrice: Double,
    currentSalePrice: Double,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (costPrice: Double, salePrice: Double, reason: String) -> Unit
) {
    // ── Local TextFieldValue — hiển thị format giá, tránh cursor jump ───────────
    var costFieldValue by remember {
        val f = formatThousands(currentCostPrice.toLong().toString())
        mutableStateOf(TextFieldValue(text = f, selection = TextRange(f.length)))
    }
    var saleFieldValue by remember {
        val f = formatThousands(currentSalePrice.toLong().toString())
        mutableStateOf(TextFieldValue(text = f, selection = TextRange(f.length)))
    }
    var reason by remember { mutableStateOf("") }

    val costPrice = stripFormat(costFieldValue.text).toDoubleOrNull() ?: 0.0
    val salePrice = stripFormat(saleFieldValue.text).toDoubleOrNull() ?: 0.0
    val margin = if (costPrice > 0) ((salePrice - costPrice) / costPrice * 100).toInt() else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF4FBF1),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Cập nhật giá", fontWeight = FontWeight.ExtraBold, color = OnSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reference current prices
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Giá hiện tại: ${vndFmt.format(currentCostPrice)}đ / ${vndFmt.format(currentSalePrice)}đ",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }

                OutlinedTextField(
                                    value = costFieldValue,
                                    onValueChange = { newVal ->
                                        val raw = stripFormat(newVal.text)
                                        val formatted = formatThousands(raw)
                                        costFieldValue = TextFieldValue(
                                            text = formatted,
                                            selection = TextRange(formatted.length)
                                        )
                                    },
                                    label = { Text("Giá nhập mới (đ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                OutlinedTextField(
                                    value = saleFieldValue,
                                    onValueChange = { newVal ->
                                        val raw = stripFormat(newVal.text)
                                        val formatted = formatThousands(raw)
                                        saleFieldValue = TextFieldValue(
                                            text = formatted,
                                            selection = TextRange(formatted.length)
                                        )
                                    },
                                    label = { Text("Giá bán mới (đ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                // Margin indicator
                if (costPrice > 0 && salePrice > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (margin > 0) Color(0xFF7EFBA4).copy(alpha = 0.3f)
                                else Color(0xFFFFDAD6).copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Biên lợi nhuận: ${if (margin >= 0) "+" else ""}$margin%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (margin > 0) PrimaryColor else Color(0xFFBA1A1A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Lý do cập nhật (tùy chọn)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(PrimaryColor, PrimaryContainer)))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { onConfirm(costPrice, salePrice, reason) }) {
                        Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnSurface,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            icon?.invoke()
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
}
