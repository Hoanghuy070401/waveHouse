package com.wavehouse.presentation.stock.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.utils.toDateTimeString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockEntryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Design tokens (The Organic Grid) ────────────────────────────────────────
private val SurfaceBase          = Color(0xFFF4FBF1)
private val SurfaceContainerLow  = Color(0xFFEFF6EC)
private val SurfaceContainerHigh = Color(0xFFDDE5DB)
private val OnSurface            = Color(0xFF171D17)
private val OnSurfaceVariant     = Color(0xFF3D4E39)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEntryDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (String) -> Unit = {},
    viewModel: StockEntryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Redirect sang OrderDetail khi entry là SALE
    LaunchedEffect(Unit) {
        viewModel.navigateToOrder.collect { orderId ->
            onNavigateToOrder(orderId)
        }
    }

    Scaffold(
        topBar = {
            // Header: nền SurfaceBase, title xanh — khớp design
            Surface(color = SurfaceBase, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại",
                            tint = ChartIn)
                    }
                    Text(
                        "Order Details",
                        fontWeight = FontWeight.SemiBold,
                        color = ChartIn,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    // More actions placeholder
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert,
                            contentDescription = null, tint = OnSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        val bgMod = Modifier.background(SurfaceBase)
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().then(bgMod).padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ChartIn) }

            uiState.entry == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiState.errorMessage ?: "Không tìm thấy giao dịch",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> StockEntryDetailContent(
                entry = uiState.entry!!,
                onOpenOrder = { orderId -> viewModel.openSourceOrder(orderId) },
                modifier = Modifier
                    .fillMaxSize()
                    .then(bgMod)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun StockEntryDetailContent(
    entry: StockEntry,
    onOpenOrder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, accent) = entry.typeIconAndColor()
    when {
        entry.type == StockEntryType.IN       -> StockInDetailContent(entry, icon, accent, modifier)
        entry.type == StockEntryType.SHRINKAGE -> ShrinkageDetailContent(entry, accent, modifier)
        entry.source == "SALE"                -> SaleEntryRedirectContent(entry, accent, onOpenOrder, modifier)
        else                                  -> GenericStockEntryDetailContent(entry, icon, accent, modifier)
    }
}

@Composable
private fun StockInDetailContent(
    entry: StockEntry,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val totalValue = entry.displayValue()
    val supplierName = entry.supplierName?.takeIf { it.isNotBlank() }
    val qtyText = "+${formatQty(entry.quantity)}"

    Column(
        modifier = modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Title block ────────────────────────────────────────
        Text(
            "Chi tiết nhập hàng",
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceVariant
        )
        Text(
            text = "Order ID #${entry.id.takeLast(6).uppercase()}",
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            color = OnSurface
        )
        StatusChip(label = "● Đã nhập", color = accent)

        Spacer(Modifier.height(4.dp))

        // ── Supplier card ──────────────────────────────────────
        SupplierHighlightCard(
            supplierName = supplierName,
            supplierId = entry.supplierId,
            accent = accent
        )

        // ── Date card ──────────────────────────────────────────
        ScheduleHighlightCard(timestamp = entry.createdAt, accent = accent)

        // ── Product list ───────────────────────────────────────
        Text(
            "Danh sách sản phẩm",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface
        )

        StockInProductCard(
            productName = entry.productName,
            sku = entry.productSku,
            quantity = qtyText,
            unitCost = entry.unitCostPrice,
            accent = accent,
            icon = icon
        )

        // ── Note ───────────────────────────────────────────────
        if (!entry.note.isNullOrBlank()) {
            StockInNoteCard(entry.note!!)
        }

        // ── Summary bar ────────────────────────────────────────
        StockInSummaryCard(
            label = "Tổng cộng đơn hàng",
            valueText = totalValue?.toVndString() ?: "${formatQty(entry.quantity)} đv",
            paymentLabel = null  // StockEntry không có paymentMethod
        )
    }
}

/**
 * Entry được tạo tự động bởi POS khi bán hàng.
 * Không hiển thị như một phiếu xuất kho — thay vào đó hiển thị banner
 * và nút "Xem đơn hàng" để redirect sang OrderDetailScreen.
 */
@Composable
private fun SaleEntryRedirectContent(
    entry: StockEntry,
    accent: Color,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon + label
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Đây là giao dịch bán hàng",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Mục này thuộc về đơn hàng POS, không phải phiếu xuất kho thủ công.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Info tóm tắt
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Người bán",
                    value = entry.createdByName.ifBlank { "—" },
                    accent = accent
                )
                InfoRow(
                    icon = Icons.Filled.Receipt,
                    label = "Sản phẩm",
                    value = "${entry.productName} · ${formatQty(entry.quantity)} đv",
                    accent = accent
                )
                InfoRow(
                    icon = Icons.Filled.Description,
                    label = "Ghi chú",
                    value = entry.note ?: "—",
                    accent = accent
                )
            }
        }

        // Nút CTA điều hướng
        val orderId = entry.orderId
        if (orderId != null) {
            Button(
                onClick = { onOpenOrder(orderId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xem chi tiết đơn hàng", fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Entry cũ (trước khi thêm orderId) — hiển thông báo thay thế
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Đơn hàng này được tạo trước khi có tính năng liên kết. Vui lòng xem trong Lịch sử Đơn Hàng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun GenericStockEntryDetailContent(
    entry: StockEntry,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    // Show positive values with color coding instead of negative signs
    val qtyPrefix = if (entry.type.isIncoming) "+" else ""

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(label = "Đã xác nhận", color = accent)
            Text(
                entry.createdAt.toDateTimeString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.displayTitle(),
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (entry.type == StockEntryType.SHRINKAGE && entry.shrinkageReason != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Lý do:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.shrinkageReason.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }
            Text(
                text = "#${entry.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entry.type == StockEntryType.IN && !entry.supplierName.isNullOrBlank()) {
                    InfoRow(
                        icon = Icons.Filled.Storefront,
                        label = "Nhà cung cấp",
                        value = entry.supplierName,
                        accent = accent
                    )
                }
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Người thực hiện",
                    value = entry.createdByName.ifBlank { "—" },
                    accent = accent
                )
            }
        }

        val totalValue = entry.displayValue()
        if (totalValue != null) {
            InfoCard(containerColor = accent.copy(alpha = 0.10f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.valueLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = totalValue.toVndString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = accent
                    )
                }
            }
        }

        if (!entry.note.isNullOrBlank()) {
            InfoCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconCircle(icon = Icons.Filled.Description, color = accent)
                    Column {
                        Text(
                            "Ghi chú",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            entry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            "Danh sách sản phẩm",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        InfoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.productName.ifBlank { "Sản phẩm không xác định" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    if (entry.productSku.isNotBlank()) {
                        Text(
                            "SKU ${entry.productSku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.unitCostPrice != null && entry.unitCostPrice > 0.0) {
                        Text(
                            "${entry.unitCostPrice.toVndString()} / đv",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$qtyPrefix${formatQty(entry.quantity)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════
// Chi tiết hao hụt — Stitch design
// ══════════════════════════════════════════════════════════════════

@Composable
private fun ShrinkageDetailContent(
    entry: StockEntry,
    accent: Color,   // = ChartOut (red)
    modifier: Modifier = Modifier
) {
    val lossValue = entry.displayValue() ?: 0.0
    val dateStr = remember(entry.createdAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(entry.createdAt))
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Title block (nền SurfaceContainerLow) ─────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pill + date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(label = "✓ Đã xác nhận", color = Color(0xFFB8860B))
                    Text(dateStr, style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant)
                }
                // Title xanh
                Text(
                    "Chi tiết hao hụt",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = Color(0xFF006D37)
                )
                // Lý do: ...
                if (entry.shrinkageReason != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Lý do:", style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant)
                        Text(
                            entry.shrinkageReason.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // ── Staff card ──────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(0xFF006D37).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = Color(0xFF006D37),
                        modifier = Modifier.size(32.dp))
                }
                Text(
                    "NGƯỜI THỰC HIỆN",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Text(entry.createdByName.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface)
            }
        }

        // ── Loss value card ─────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "TỔNG GIÁ TRỊ THẤT THOÁT",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "-${lossValue.toVndString()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    color = accent
                )
                // Note sub-section
                if (!entry.note.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Menu, null,
                                    tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text("GHI CHÚ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp)
                            }
                            Text(entry.note, style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface)
                        }
                    }
                }
            }
        }

        // ── Product list ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Danh sách sản phẩm",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF006D37))
            Surface(shape = CircleShape, color = SurfaceContainerHigh) {
                Text("1 item",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }

        ShrinkageProductItem(
            productName = entry.productName,
            sku = entry.productSku,
            quantity = formatQty(entry.quantity),
            accent = accent
        )

        // ── Location card ───────────────────────────────────────────
        ShrinkageLocationCard()
    }
}

@Composable
private fun ShrinkageProductItem(productName: String, sku: String, quantity: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Warning, null,
                    tint = accent, modifier = Modifier.size(28.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(productName.ifBlank { "Sản phẩm" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = OnSurface)
                if (sku.isNotBlank()) {
                    Text("SKU: $sku", style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("-${quantity}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = accent)
                Text("Hao hụt", style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShrinkageLocationCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Map placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Map, null,
                    tint = OnSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp))
            }
            Text("KHU VỰC KIỂM KHO",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8860B),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp)
            Text("Khu vực A — Kho chính",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = OnSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.AcUnit, null,
                        tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text("4°C", style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Opacity, null,
                        tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text("85%", style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Small UI building blocks
// ══════════════════════════════════════════════════════════════════

@Composable
private fun InfoCard(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(icon = icon, color = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.5).dp))
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Entry-type presentation helpers
// ══════════════════════════════════════════════════════════════════

@Composable
private fun StockEntry.typeIconAndColor(): Pair<ImageVector, Color> = when (type) {
    StockEntryType.IN -> Icons.Filled.ArrowDownward to ChartIn
    StockEntryType.OUT -> Icons.Filled.ArrowUpward to ChartOut
    StockEntryType.ADJUST -> Icons.Filled.Tune to StockLow
    StockEntryType.TRANSFER -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.primary
    StockEntryType.SHRINKAGE -> Icons.Filled.DeleteForever to MaterialTheme.colorScheme.error
}

private fun StockEntry.displayTitle(): String = when (type) {
    StockEntryType.IN -> "Chi tiết nhập kho"
    StockEntryType.OUT -> "Chi tiết xuất kho"
    StockEntryType.SHRINKAGE -> "Chi tiết hao hụt"
    StockEntryType.ADJUST -> "Chi tiết điều chỉnh"
    StockEntryType.TRANSFER -> "Chi tiết chuyển kho"
}

private fun StockEntry.valueLabel(): String = when (type) {
    StockEntryType.IN -> "Tổng giá trị nhập hàng"
    StockEntryType.OUT -> "Tổng giá trị xuất"
    StockEntryType.SHRINKAGE -> "Tổng giá trị thất thoát"
    else -> "Tổng giá trị"
}

private fun StockEntry.displayValue(): Double? {
    val priceEach = unitCostPrice ?: macAfter
    return if (priceEach != null && priceEach > 0.0) priceEach * quantity else null
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

@Composable
private fun SupplierHighlightCard(supplierName: String?, supplierId: String?, accent: Color) {
    // Nền SurfaceContainerLow, không shadow — background shift
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Uppercase gray label
                Text(
                    "NHÀ CUNG CẤP",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Text(
                    supplierName ?: "Chưa có nhà cung cấp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                if (supplierId != null) {
                    Text(
                        "Chuyên cung cấp rau củ quả hữu cơ tiêu chuẩn VietGAP từ cao nguyên Đà Lạt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            // Icon cây/thực vật góc phải
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Icon(
                    Icons.Filled.Storefront, null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleHighlightCard(timestamp: Long, accent: Color) {
    val fullDate = remember(timestamp) { timestamp.toFullDateString() }
    val dayMonth = remember(timestamp) { timestamp.toDayOfMonth() }  // "24/10"
    val year = remember(timestamp) { SimpleDateFormat("yyyy", locale).format(Date(timestamp)) }

    // Nền xanh đậm — khớp design
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF006D37),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar icon
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                Icon(
                    Icons.Filled.CalendarMonth, null,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            // Label + date
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Ngày nhập kho", style = MaterialTheme.typography.labelMedium)
                Text(
                    dayMonth,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            }
            // Year badge
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.18f)) {
                Text(
                    year,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StockInProductCard(
    productName: String,
    sku: String,
    quantity: String,
    unitCost: Double?,
    accent: Color,
    icon: ImageVector
) {
    // Layout design: ảnh/icon ở trên, tên + đơn giá giữa, rồi SỐ LƯỢNG / THÀNH TIỀN dưới
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ảnh / icon — trên cùng
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(40.dp))
            }

            // Tên sản phẩm + đơn giá
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    productName.ifBlank { "Sản phẩm không xác định" },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                if (unitCost != null && unitCost > 0.0) {
                    Text(
                        "Đơn giá: ${unitCost.toVndString()} / ${if (sku.isNotBlank()) sku else "kg"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            // SỐ LƯỢNG + THÀNH TIỀN — layout 2 cột
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "SỐ LƯỢNG",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        quantity,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = accent
                    )
                }
                if (unitCost != null && unitCost > 0.0) {
                    // Parse qty number from qtyText (e.g. "+100")
                    val rawQty = quantity.trimStart('+').toDoubleOrNull() ?: 0.0
                    val total = unitCost * rawQty
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "THÀNH TIỀN",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            total.toVndString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockInNoteCard(note: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF3E6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.7f)) {
                Icon(
                    Icons.Filled.Description, null,
                    tint = Color(0xFFFF8A4A),
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Column {
                Text(
                    "GHI CHÚ",  // uppercase — khớp design
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A4F31),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "\"$note\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E331F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StockInSummaryCard(label: String, valueText: String, paymentLabel: String?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B3B29),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Text(
                valueText,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = ChartIn  // xanh sáng — giá trị nổi bật
            )
            // Payment method row
            if (!paymentLabel.isNullOrBlank()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "THANH TOÁN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        paymentLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ChartIn
                    )
                }
            }
        }
    }
}

@Composable
private fun LightStatusChip(label: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

private val locale = Locale("vi", "VN")
private val fullDateFormatter = SimpleDateFormat("dd/MM/yyyy", locale)
private val dayFormatter = SimpleDateFormat("dd/MM", locale)
private val timeFormatter = SimpleDateFormat("HH:mm", locale)

private fun Long.toFullDateString(): String = fullDateFormatter.format(Date(this))
private fun Long.toDayOfMonth(): String = dayFormatter.format(Date(this))
private fun Long.toHourMinuteString(): String = timeFormatter.format(Date(this))
